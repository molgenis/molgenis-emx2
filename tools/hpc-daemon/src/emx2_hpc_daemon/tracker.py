"""In-memory tracking of submitted Slurm jobs.

Maintains a mapping of EMX2 job IDs to Slurm job state for the daemon's main
loop. On restart, can reconcile with EMX2 server and Slurm state.
"""

from __future__ import annotations

import logging
import time
from dataclasses import dataclass

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


class JobTracker:
    """Tracks active (non-terminal) jobs managed by this daemon."""

    def __init__(self):
        self._jobs: dict[str, TrackedJob] = {}

    def track(self, emx2_job_id: str, **kwargs) -> TrackedJob:
        """Start tracking a job."""
        if "claimed_at" not in kwargs:
            kwargs["claimed_at"] = time.monotonic()
        job = TrackedJob(emx2_job_id=emx2_job_id, **kwargs)
        self._jobs[emx2_job_id] = job
        logger.debug("Tracking job %s", emx2_job_id)
        return job

    def update(self, emx2_job_id: str, **kwargs) -> TrackedJob | None:
        """Update tracking info for a job."""
        job = self._jobs.get(emx2_job_id)
        if job is None:
            return None
        for key, value in kwargs.items():
            if hasattr(job, key):
                setattr(job, key, value)
        return job

    def remove(self, emx2_job_id: str) -> TrackedJob | None:
        """Stop tracking a job (when it reaches a terminal state)."""
        job = self._jobs.pop(emx2_job_id, None)
        if job:
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
