"""In-memory tracking of submitted Slurm jobs with optional TinyDB persistence.

Maintains a mapping of EMX2 job IDs to Slurm job state for the daemon's main
loop. When a ``state_db_path`` is provided, all mutations are written through
to a TinyDB JSON file so that ``once`` mode (cron-invoked) can reliably
progress jobs across invocations.

Wall-clock timestamps (``time.time()``) are stored in the DB because
``time.monotonic()`` is process-local. On load, wall-clock values are
converted back to monotonic offsets.
"""

from __future__ import annotations

import logging
import time
from dataclasses import dataclass, fields
from pathlib import Path

logger = logging.getLogger(__name__)


@dataclass
class TrackedJob:
    """A job being tracked by this daemon instance."""

    emx2_job_id: str
    slurm_job_id: str | None = None
    status: str = "CLAIMED"
    work_dir: str | None = None
    input_dir: str | None = None
    output_dir: str | None = None
    processor: str | None = None
    profile: str | None = None
    claimed_at: float = 0.0  # time.monotonic() at tracking time
    last_progress_hash: str | None = None
    last_queue_report: float = 0.0  # time.monotonic() of last queue status report

    @property
    def profile_key(self) -> str:
        """Return the 'processor:profile' key used for timeout lookups."""
        return f"{self.processor}:{self.profile or ''}"


def _wall_to_mono(wall: float) -> float:
    """Convert a wall-clock timestamp to a monotonic-equivalent value."""
    if wall <= 0:
        return 0.0
    offset = time.time() - time.monotonic()
    return max(0.0, wall - offset)


def _mono_to_wall(mono: float) -> float:
    """Convert a monotonic timestamp to wall-clock for storage."""
    if mono <= 0:
        return 0.0
    offset = time.time() - time.monotonic()
    return mono + offset


class JobTracker:
    """Tracks active (non-terminal) jobs managed by this daemon.

    When ``state_db_path`` is provided, mutations are written through to a
    TinyDB JSON file for persistence across process restarts.
    """

    _VALID_FIELDS = frozenset(f.name for f in fields(TrackedJob))
    _TIME_FIELDS = frozenset({"claimed_at", "last_queue_report"})

    def __init__(self, state_db_path: Path | str | None = None):
        self._jobs: dict[str, TrackedJob] = {}
        self._db = None
        self._table = None

        if state_db_path is not None:
            self._init_db(Path(state_db_path))

    def _init_db(self, path: Path) -> None:
        """Initialise the TinyDB state file."""
        from tinydb import TinyDB

        path.parent.mkdir(parents=True, exist_ok=True)
        self._db = TinyDB(str(path))
        self._table = self._db.table("tracked_jobs")
        logger.debug("State DB opened at %s", path)

    # -- persistence helpers --------------------------------------------------

    def _persist(self, job: TrackedJob) -> None:
        """Write-through: upsert a job into the DB."""
        if self._table is None:
            return
        from tinydb import Query

        doc = {f.name: getattr(job, f.name) for f in fields(TrackedJob)}
        # Store time fields as wall-clock
        for tf in self._TIME_FIELDS:
            doc[tf] = _mono_to_wall(doc[tf])

        Q = Query()
        self._table.upsert(doc, Q.emx2_job_id == job.emx2_job_id)

    def _remove_from_db(self, emx2_job_id: str) -> None:
        """Remove a job row from the DB."""
        if self._table is None:
            return
        from tinydb import Query

        Q = Query()
        self._table.remove(Q.emx2_job_id == emx2_job_id)

    def load_from_db(self) -> dict[str, TrackedJob]:
        """Load all persisted jobs into memory.

        Returns the dict of loaded jobs (also stored in ``self._jobs``).
        Existing in-memory entries are **not** overwritten.
        """
        if self._table is None:
            return self._jobs

        for doc in self._table.all():
            emx2_id = doc.get("emx2_job_id")
            if not emx2_id or emx2_id in self._jobs:
                continue

            kwargs = {
                k: doc[k]
                for k in (f.name for f in fields(TrackedJob))
                if k in doc and k != "emx2_job_id"
            }
            # Convert wall-clock times back to monotonic
            for tf in self._TIME_FIELDS:
                if tf in kwargs:
                    kwargs[tf] = _wall_to_mono(kwargs[tf])

            self._jobs[emx2_id] = TrackedJob(emx2_job_id=emx2_id, **kwargs)
            logger.debug("Loaded job %s from state DB", emx2_id)

        return self._jobs

    # -- public API -----------------------------------------------------------

    def track(self, emx2_job_id: str, **kwargs) -> TrackedJob:
        """Start tracking a job."""
        if "claimed_at" not in kwargs:
            kwargs["claimed_at"] = time.monotonic()
        job = TrackedJob(emx2_job_id=emx2_job_id, **kwargs)
        self._jobs[emx2_job_id] = job
        self._persist(job)
        logger.debug("Tracking job %s", emx2_job_id)
        return job

    def update(self, emx2_job_id: str, **kwargs) -> TrackedJob | None:
        """Update tracking info for a job.

        Raises ValueError if any kwarg is not a valid TrackedJob field.
        """
        job = self._jobs.get(emx2_job_id)
        if job is None:
            return None
        unknown = set(kwargs) - self._VALID_FIELDS
        if unknown:
            raise ValueError(f"Unknown TrackedJob fields: {unknown}")
        for key, value in kwargs.items():
            setattr(job, key, value)
        self._persist(job)
        return job

    def remove(self, emx2_job_id: str) -> TrackedJob | None:
        """Stop tracking a job (when it reaches a terminal state)."""
        job = self._jobs.pop(emx2_job_id, None)
        if job:
            self._remove_from_db(emx2_job_id)
            logger.debug("Untracked job %s", emx2_job_id)
        return job

    def get(self, emx2_job_id: str) -> TrackedJob | None:
        """Get tracking info for a job."""
        return self._jobs.get(emx2_job_id)

    def active_jobs(self) -> list[TrackedJob]:
        """Return all actively tracked jobs."""
        return list(self._jobs.values())

    def active_count(self) -> int:
        """Return count of tracked jobs."""
        return len(self._jobs)

    def slurm_ids(self) -> list[str]:
        """Return all tracked Slurm job IDs (for cancellation on shutdown)."""
        return [j.slurm_job_id for j in self._jobs.values() if j.slurm_job_id]

    def reconcile_from_server(self, server_jobs: list[dict]) -> None:
        """
        On restart, reconcile local tracking state with server-known jobs.

        Takes a list of non-terminal jobs assigned to this worker from the EMX2
        server and re-populates the tracker. The daemon loop will then check
        Slurm state for each and report accordingly.
        """
        for job_data in server_jobs:
            emx2_id = job_data.get("id")
            if emx2_id and emx2_id not in self._jobs:
                self.track(
                    emx2_job_id=emx2_id,
                    slurm_job_id=job_data.get("slurm_job_id"),
                    status=job_data.get("status", "CLAIMED"),
                    processor=job_data.get("processor"),
                    profile=job_data.get("profile"),
                )
                logger.info("Recovered tracking for job %s", emx2_id)

    def close(self) -> None:
        """Close the underlying TinyDB (flushes any buffered writes)."""
        if self._db is not None:
            self._db.close()
