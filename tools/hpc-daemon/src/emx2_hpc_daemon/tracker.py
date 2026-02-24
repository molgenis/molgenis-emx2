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

import json
import logging
import os
import threading
import time
from contextlib import contextmanager
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
        self._db_path: Path | None = None
        self._lock_handle = None
        self._lock = threading.RLock()

        if state_db_path is not None:
            self._init_db(Path(state_db_path))

    def _init_db(self, path: Path) -> None:
        """Initialise the TinyDB state file."""
        from tinydb import TinyDB

        path.parent.mkdir(parents=True, exist_ok=True)
        self._db_path = path
        try:
            self._lock_handle = open(path.parent / f"{path.name}.lock", "a+b")
        except OSError:
            self._lock_handle = None
            logger.warning(
                "State DB lock file is not writable at %s; continuing without inter-process lock",
                path.parent / f"{path.name}.lock",
            )
        try:
            self._db = TinyDB(str(path))
            self._table = self._db.table("tracked_jobs")
        except (json.JSONDecodeError, ValueError):
            self._recover_corrupt_db()
        except OSError:
            self._db = None
            self._table = None
            logger.warning(
                "State DB is not writable at %s; running without persistence",
                path,
            )
        logger.debug("State DB opened at %s", path)

    @contextmanager
    def _db_guard(self):
        if self._table is None:
            yield
            return

        with self._lock:
            locked = False
            if self._lock_handle is not None:
                try:
                    import fcntl

                    fcntl.flock(self._lock_handle.fileno(), fcntl.LOCK_EX)
                    locked = True
                except (ImportError, OSError):
                    logger.debug(
                        "State DB lock unavailable; continuing without file lock"
                    )
            try:
                yield
            finally:
                if locked and self._lock_handle is not None:
                    try:
                        import fcntl

                        fcntl.flock(self._lock_handle.fileno(), fcntl.LOCK_UN)
                    except (ImportError, OSError):
                        pass

    def _recover_corrupt_db(self) -> None:
        """Rotate a corrupt TinyDB file out of the way and reopen a fresh one."""
        if self._db_path is None:
            raise RuntimeError("Cannot recover TinyDB without a state path")

        if self._db is not None:
            try:
                self._db.close()
            except Exception:
                logger.debug("Failed closing corrupt TinyDB handle", exc_info=True)
        self._db = None
        self._table = None

        backup_path = self._db_path.with_name(
            f"{self._db_path.name}.corrupt-{int(time.time())}"
        )
        if self._db_path.exists():
            try:
                os.replace(self._db_path, backup_path)
                logger.error(
                    "Corrupt TinyDB state file moved to %s; starting fresh state DB",
                    backup_path,
                )
            except OSError:
                logger.exception(
                    "Failed to rotate corrupt TinyDB state file %s", self._db_path
                )
        from tinydb import TinyDB

        self._db = TinyDB(str(self._db_path))
        self._table = self._db.table("tracked_jobs")

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
        try:
            with self._db_guard():
                self._table.upsert(doc, Q.emx2_job_id == job.emx2_job_id)
        except (json.JSONDecodeError, ValueError):
            logger.exception(
                "TinyDB state corruption detected during persist; recovering"
            )
            self._recover_corrupt_db()
            with self._db_guard():
                self._table.upsert(doc, Q.emx2_job_id == job.emx2_job_id)

    def _remove_from_db(self, emx2_job_id: str) -> None:
        """Remove a job row from the DB."""
        if self._table is None:
            return
        from tinydb import Query

        Q = Query()
        try:
            with self._db_guard():
                self._table.remove(Q.emx2_job_id == emx2_job_id)
        except (json.JSONDecodeError, ValueError):
            logger.exception(
                "TinyDB state corruption detected during delete; recovering"
            )
            self._recover_corrupt_db()

    def load_from_db(self) -> dict[str, TrackedJob]:
        """Load all persisted jobs into memory.

        Returns the dict of loaded jobs (also stored in ``self._jobs``).
        Existing in-memory entries are **not** overwritten.
        """
        if self._table is None:
            return self._jobs

        try:
            with self._db_guard():
                docs = list(self._table.all())
        except (json.JSONDecodeError, ValueError):
            logger.exception("TinyDB state corruption detected during load; recovering")
            self._recover_corrupt_db()
            with self._db_guard():
                docs = list(self._table.all())

        for doc in docs:
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
        if self._lock_handle is not None:
            self._lock_handle.close()
            self._lock_handle = None
