"""Main daemon loop: register → poll → claim → submit → monitor → report.

Single-threaded event loop:
1. Register worker with EMX2 server, advertising capabilities
2. Poll for pending jobs matching our capabilities
3. Claim a job (atomic, server-side)
4. Download input artifacts to job workdir
5. Generate batch script and submit to Slurm via sbatch
6. Monitor running Slurm jobs via squeue/sacct
7. Upload output artifacts on completion
8. Report status transitions back to EMX2
9. Sleep for configured interval, repeat

On SIGTERM: stop accepting new jobs, wait for in-flight transitions, exit.
Slurm jobs continue independently after daemon shutdown.
"""

from __future__ import annotations

import hashlib
import logging
import signal
import socket
import time
from pathlib import Path

from .backend import ExecutionBackend, SimulatedBackend, SlurmBackend
from .client import ClaimConflict, HpcClient
from .config import DaemonConfig
from .profiles import derive_capabilities
from .tracker import JobTracker

logger = logging.getLogger(__name__)

TERMINAL_STATUSES = frozenset({"COMPLETED", "FAILED", "CANCELLED"})


class HpcDaemon:
    """The main daemon orchestrating HPC job execution."""

    def __init__(self, config: DaemonConfig, simulate: bool = False):
        self.config = config
        self._backend: ExecutionBackend = (
            SimulatedBackend() if simulate else SlurmBackend(config)
        )
        self.client = HpcClient(
            base_url=config.emx2.base_url,
            worker_id=config.emx2.worker_id,
            shared_secret=config.emx2.shared_secret,
            auth_mode=config.emx2.auth_mode,
        )
        self.tracker = JobTracker()
        self._running = True
        self._hostname = socket.gethostname()
        self._heartbeat_interval = 120  # seconds
        self._last_heartbeat = 0.0
        if simulate:
            logger.info("Running in SIMULATE mode — no real Slurm commands will be executed")

    def run(self) -> None:
        """Main entry point — run the daemon loop until interrupted."""
        signal.signal(signal.SIGTERM, self._handle_shutdown)
        signal.signal(signal.SIGINT, self._handle_shutdown)

        logger.info(
            "Starting HPC daemon worker_id=%s on %s",
            self.config.emx2.worker_id,
            self._hostname,
        )

        try:
            # Step 1: Register with EMX2
            self._register()

            # Step 2: Recover any in-flight jobs from a previous run
            self._recover_jobs()

            # Main loop
            while self._running:
                try:
                    self._poll_and_claim()
                    self._monitor_running_jobs()
                    self._maybe_heartbeat()
                except Exception:
                    logger.exception("Error in daemon loop")

                if self._running:
                    time.sleep(self.config.worker.poll_interval_seconds)
        finally:
            self.client.close()
        logger.info("Daemon stopped")

    def run_once(self) -> None:
        """Run a single poll-claim-monitor cycle, then exit."""
        logger.info(
            "Running single cycle worker_id=%s on %s",
            self.config.emx2.worker_id,
            self._hostname,
        )
        try:
            self._register()
            self._recover_jobs()
            self._poll_and_claim()
            self._monitor_running_jobs()
            logger.info(
                "Single cycle complete, %d active jobs", self.tracker.active_count()
            )
        finally:
            self.client.close()

    def _register(self) -> None:
        """Register this worker with capabilities derived from profiles."""
        capabilities = derive_capabilities(self.config)
        try:
            self.client.register_worker(self._hostname, capabilities)
            self._last_heartbeat = time.monotonic()
            logger.info(
                "Registered worker %s with %d capabilities",
                self.config.emx2.worker_id,
                len(capabilities),
            )
        except Exception:
            logger.exception("Failed to register worker")

    def _recover_jobs(self) -> None:
        """On startup, recover tracking state for non-terminal jobs assigned to this worker."""
        try:
            # Query EMX2 for jobs assigned to this worker that are not terminal
            jobs = self.client.poll_pending_jobs()
            # Filter out PENDING jobs — they haven't been claimed by us yet,
            # so they should go through the normal _poll_and_claim() → _submit_job() path.
            # Only recover jobs that are already in-flight (CLAIMED/SUBMITTED/STARTED).
            self.tracker.reconcile_from_server(
                [j for j in jobs if j.get("status") != "PENDING"]
            )
            if self.tracker.active_count() > 0:
                logger.info(
                    "Recovered %d in-flight jobs from previous run",
                    self.tracker.active_count(),
                )
        except Exception:
            logger.exception("Failed to recover jobs from server")

    def _maybe_heartbeat(self) -> None:
        """Send periodic heartbeat to keep worker registration alive."""
        if time.monotonic() - self._last_heartbeat > self._heartbeat_interval:
            try:
                self.client.heartbeat()
                self._last_heartbeat = time.monotonic()
                logger.debug("Heartbeat sent")
            except Exception:
                logger.warning("Failed to send heartbeat")

    def _poll_and_claim(self) -> None:
        """Poll for pending jobs and attempt to claim one."""
        if self.tracker.active_count() >= self.config.worker.max_concurrent_jobs:
            logger.debug(
                "At max concurrent jobs (%d), skipping poll",
                self.config.worker.max_concurrent_jobs,
            )
            return

        # Poll for each capability
        capabilities = derive_capabilities(self.config)
        for cap in capabilities:
            if not self._running:
                break

            try:
                jobs = self.client.poll_pending_jobs(
                    processor=cap["processor"],
                    profile=cap["profile"],
                )
            except Exception:
                logger.exception("Failed to poll for jobs")
                continue

            for job in jobs:
                if self.tracker.active_count() >= self.config.worker.max_concurrent_jobs:
                    return

                job_id = job["id"]
                try:
                    claimed = self.client.claim_job(job_id)
                    logger.info("Claimed job %s", job_id)
                    self._submit_job(claimed)
                except ClaimConflict:
                    logger.debug("Job %s already claimed by another worker", job_id)
                except Exception:
                    logger.exception("Failed to claim/submit job %s", job_id)

    def _submit_job(self, job: dict) -> None:
        """Submit a job via the execution backend."""
        job_id = job["id"]
        try:
            result = self._backend.submit(job, self.client)
            self.tracker.track(
                emx2_job_id=job_id,
                slurm_job_id=result.slurm_job_id,
                status="SUBMITTED",
                work_dir=result.work_dir,
                input_dir=result.input_dir,
                output_dir=result.output_dir,
            )
            self.client.transition_job(
                job_id,
                "SUBMITTED",
                detail=f"Slurm job {result.slurm_job_id}",
                slurm_job_id=result.slurm_job_id,
            )
            logger.info("Submitted job %s as %s", job_id, result.slurm_job_id)
        except ValueError as e:
            logger.error("Cannot submit job %s: %s", job_id, e)
            self.client.transition_job(job_id, "FAILED", detail=str(e))
        except Exception as e:
            logger.exception("Failed to submit job %s", job_id)
            self.client.transition_job(job_id, "FAILED", detail=str(e))

    def _upload_output_artifacts(self, job_id: str, output_dir: str) -> str | None:
        """Register output files as a new artifact. Returns artifact ID or None.

        Supports two modes based on config.emx2.artifact_residence:
        - posix: registers path only (no binary upload), content_url = file:// URI
        - managed: uploads binary content to EMX2 server
        """
        output_path = Path(output_dir)
        output_files = [
            f
            for f in output_path.iterdir()
            if f.is_file() and not f.name.startswith("slurm-")
        ]

        if not output_files:
            logger.debug("No output files to upload for job %s", job_id)
            return None

        residence = self.config.emx2.artifact_residence

        try:
            if residence == "posix":
                return self._register_posix_artifact(job_id, output_dir, output_files)
            else:
                return self._upload_managed_artifact(job_id, output_files)
        except Exception:
            logger.exception("Failed to upload output artifacts for job %s", job_id)
            return None

    def _register_posix_artifact(
        self, job_id: str, output_dir: str, output_files: list[Path]
    ) -> str:
        """Register a posix artifact with file metadata only (no binary upload)."""
        content_url = f"file://{output_dir}"
        artifact = self.client.create_artifact(
            artifact_type="blob",
            fmt="mixed",
            residence="posix",
            metadata={"job_id": job_id},
            content_url=content_url,
        )
        artifact_id = artifact["id"]

        # Commit immediately (REGISTERED → COMMITTED for external artifacts)
        total_size = sum(f.stat().st_size for f in output_files)
        hasher = hashlib.sha256()
        for f in output_files:
            hasher.update(f.read_bytes())

        self.client.commit_artifact(
            artifact_id,
            sha256=hasher.hexdigest(),
            size_bytes=total_size,
        )
        logger.info(
            "Registered posix artifact %s (%d files) for job %s at %s",
            artifact_id,
            len(output_files),
            job_id,
            content_url,
        )
        return artifact_id

    def _upload_managed_artifact(
        self, job_id: str, output_files: list[Path]
    ) -> str:
        """Upload output files as a managed artifact with binary content."""
        artifact = self.client.create_artifact(
            artifact_type="blob",
            fmt="mixed",
            residence="managed",
            metadata={"job_id": job_id},
        )
        artifact_id = artifact["id"]

        total_size = 0
        hasher = hashlib.sha256()
        for f in output_files:
            content = f.read_bytes()
            hasher.update(content)
            total_size += len(content)
            self.client.upload_artifact_file(
                artifact_id,
                path=f.name,
                file_content=content,
                role="output",
            )

        self.client.commit_artifact(
            artifact_id,
            sha256=hasher.hexdigest(),
            size_bytes=total_size,
        )
        logger.info(
            "Uploaded %d output files as artifact %s for job %s",
            len(output_files),
            artifact_id,
            job_id,
        )
        return artifact_id

    def _check_server_cancellations(self) -> None:
        """Check if any tracked jobs were cancelled server-side and propagate."""
        for tracked in self.tracker.active_jobs():
            try:
                job = self.client.get_job(tracked.emx2_job_id)
                server_status = job.get("status", "")
                if server_status == "CANCELLED" and tracked.status != "CANCELLED":
                    logger.info(
                        "Job %s cancelled server-side, cancelling Slurm job %s",
                        tracked.emx2_job_id,
                        tracked.slurm_job_id,
                    )
                    if tracked.slurm_job_id:
                        self._backend.cancel(tracked.slurm_job_id)
                    tracked.status = "CANCELLED"
                    self.tracker.remove(tracked.emx2_job_id)
            except Exception:
                logger.debug(
                    "Could not check cancellation status for job %s",
                    tracked.emx2_job_id,
                )

    def _monitor_running_jobs(self) -> None:
        """Check status of all tracked jobs and report transitions."""
        self._check_server_cancellations()

        for tracked in self.tracker.active_jobs():
            if not tracked.slurm_job_id:
                continue

            new_status = self._backend.query_status(
                tracked.slurm_job_id, tracked.status
            )
            if new_status is None:
                continue

            try:
                detail = f"Slurm state: {new_status}"
                output_artifact_id = None

                # On successful completion, upload output artifacts
                if new_status == "COMPLETED" and tracked.output_dir:
                    output_artifact_id = self._upload_output_artifacts(
                        tracked.emx2_job_id, tracked.output_dir
                    )
                    if output_artifact_id:
                        detail += f"; output artifact: {output_artifact_id}"

                self.client.transition_job(
                    tracked.emx2_job_id,
                    new_status,
                    detail=detail,
                    output_artifact_id=output_artifact_id,
                )
                tracked.status = new_status
                logger.info(
                    "Job %s transitioned to %s",
                    tracked.emx2_job_id,
                    new_status,
                )

                # Remove from tracker if terminal
                if new_status in TERMINAL_STATUSES:
                    self.tracker.remove(tracked.emx2_job_id)

            except Exception:
                logger.exception(
                    "Failed to report transition for job %s", tracked.emx2_job_id
                )

    def _handle_shutdown(self, signum, frame) -> None:
        """Handle SIGTERM/SIGINT: stop accepting new jobs and exit gracefully.

        Slurm jobs are NOT cancelled on shutdown — they continue running
        independently. On next daemon startup, the recovery logic will pick
        them up.
        """
        logger.info(
            "Shutdown signal received (%s), stopping gracefully. %d tracked jobs will continue in Slurm.",
            signal.Signals(signum).name,
            self.tracker.active_count(),
        )
        self._running = False
