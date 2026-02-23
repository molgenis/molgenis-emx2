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
from .profiles import derive_capabilities, resolve_profile
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
        logger.debug(
            "Polling for jobs across %d capabilities: %s",
            len(capabilities),
            [f"{c['processor']}:{c['profile']}" for c in capabilities],
        )
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
        logger.debug(
            "Submitting job %s (processor=%s, profile=%s, inputs=%s)",
            job_id,
            job.get("processor"),
            job.get("profile"),
            job.get("inputs"),
        )
        try:
            result = self._backend.submit(job, self.client)
            self.tracker.track(
                emx2_job_id=job_id,
                slurm_job_id=result.slurm_job_id,
                status="SUBMITTED",
                work_dir=result.work_dir,
                input_dir=result.input_dir,
                output_dir=result.output_dir,
                processor=job.get("processor"),
                profile=job.get("profile"),
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

    def _upload_output_artifacts(
        self,
        job_id: str,
        output_dir: str,
        processor: str | None = None,
        profile: str | None = None,
    ) -> str | None:
        """Register output files as a new artifact. Returns artifact ID or None.

        Supports two modes based on the profile's artifact_residence setting:
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

        logger.debug(
            "Output files for job %s: %s",
            job_id,
            [f"{f.name} ({f.stat().st_size}b)" for f in output_files],
        )

        # Resolve residence from the job's profile; fall back to "managed"
        residence = "managed"
        if processor:
            resolved = resolve_profile(self.config, processor, profile or "")
            if resolved:
                residence = resolved.artifact_residence
        logger.debug(
            "Artifact residence for job %s (processor=%s, profile=%s): %s",
            job_id,
            processor,
            profile,
            residence,
        )

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
        logger.debug(
            "Creating posix artifact for job %s: content_url=%s, files=%d",
            job_id,
            content_url,
            len(output_files),
        )
        artifact = self.client.create_artifact(
            artifact_type="blob",
            residence="posix",
            metadata={"job_id": job_id},
            content_url=content_url,
            name=f"output-{job_id[:8]}",
        )
        artifact_id = artifact["id"]
        logger.debug(
            "Created posix artifact %s, _links: %s",
            artifact_id,
            {
                rel: f"{lnk.get('method', 'GET')} {lnk.get('href', '?')}"
                for rel, lnk in artifact.get("_links", {}).items()
            },
        )

        # Commit immediately (REGISTERED → COMMITTED for external artifacts)
        total_size = sum(f.stat().st_size for f in output_files)
        hasher = hashlib.sha256()
        for f in output_files:
            hasher.update(f.read_bytes())

        commit_result = self.client.commit_artifact(
            artifact_id,
            sha256=hasher.hexdigest(),
            size_bytes=total_size,
        )
        logger.debug(
            "Committed posix artifact %s: sha256=%s, size=%d, _links: %s",
            artifact_id,
            hasher.hexdigest(),
            total_size,
            {
                rel: f"{lnk.get('method', 'GET')} {lnk.get('href', '?')}"
                for rel, lnk in commit_result.get("_links", {}).items()
            },
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
        logger.debug(
            "Creating managed artifact for job %s (%d files)",
            job_id,
            len(output_files),
        )
        artifact = self.client.create_artifact(
            artifact_type="blob",
            residence="managed",
            metadata={"job_id": job_id},
            name=f"output-{job_id[:8]}",
        )
        artifact_id = artifact["id"]
        logger.debug(
            "Created managed artifact %s, _links: %s",
            artifact_id,
            {
                rel: f"{lnk.get('method', 'GET')} {lnk.get('href', '?')}"
                for rel, lnk in artifact.get("_links", {}).items()
            },
        )

        total_size = 0
        hasher = hashlib.sha256()
        for f in output_files:
            content = f.read_bytes()
            hasher.update(content)
            total_size += len(content)
            logger.debug(
                "Uploading file %s (%d bytes) to artifact %s",
                f.name,
                len(content),
                artifact_id,
            )
            self.client.upload_artifact_file(
                artifact_id,
                path=f.name,
                file_content=content,
            )

        commit_result = self.client.commit_artifact(
            artifact_id,
            sha256=hasher.hexdigest(),
            size_bytes=total_size,
        )
        logger.debug(
            "Committed managed artifact %s: sha256=%s, size=%d, _links: %s",
            artifact_id,
            hasher.hexdigest(),
            total_size,
            {
                rel: f"{lnk.get('method', 'GET')} {lnk.get('href', '?')}"
                for rel, lnk in commit_result.get("_links", {}).items()
            },
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

    def _check_profile_timeouts(self) -> None:
        """Check if any tracked jobs have exceeded their profile timeouts."""
        now = time.monotonic()
        for tracked in list(self.tracker.active_jobs()):
            profile_key = f"{tracked.processor}:{tracked.profile or ''}"
            resolved = resolve_profile(
                self.config, tracked.processor or "", tracked.profile or ""
            )
            if resolved is None:
                continue

            elapsed = now - tracked.claimed_at

            if (
                tracked.status == "SUBMITTED"
                and resolved.claim_timeout_seconds > 0
                and elapsed > resolved.claim_timeout_seconds
            ):
                detail = (
                    f"timeout: claim_timeout_seconds "
                    f"({resolved.claim_timeout_seconds}s) exceeded "
                    f"for profile {profile_key}"
                )
                logger.warning(
                    "Job %s exceeded claim timeout: %s",
                    tracked.emx2_job_id,
                    detail,
                )
                try:
                    self.client.transition_job(
                        tracked.emx2_job_id, "FAILED", detail=detail
                    )
                except Exception:
                    logger.exception(
                        "Failed to report timeout for job %s",
                        tracked.emx2_job_id,
                    )
                self.tracker.remove(tracked.emx2_job_id)

            elif (
                tracked.status == "STARTED"
                and resolved.execution_timeout_seconds > 0
                and elapsed > resolved.execution_timeout_seconds
            ):
                detail = (
                    f"timeout: execution_timeout_seconds "
                    f"({resolved.execution_timeout_seconds}s) exceeded "
                    f"for profile {profile_key}"
                )
                logger.warning(
                    "Job %s exceeded execution timeout: %s",
                    tracked.emx2_job_id,
                    detail,
                )
                if tracked.slurm_job_id:
                    try:
                        self._backend.cancel(tracked.slurm_job_id)
                    except Exception:
                        logger.exception(
                            "Failed to scancel job %s",
                            tracked.slurm_job_id,
                        )
                try:
                    self.client.transition_job(
                        tracked.emx2_job_id, "FAILED", detail=detail
                    )
                except Exception:
                    logger.exception(
                        "Failed to report timeout for job %s",
                        tracked.emx2_job_id,
                    )
                self.tracker.remove(tracked.emx2_job_id)

    def _monitor_running_jobs(self) -> None:
        """Check status of all tracked jobs and report transitions."""
        self._check_server_cancellations()
        self._check_profile_timeouts()

        for tracked in self.tracker.active_jobs():
            if not tracked.slurm_job_id:
                continue

            new_status = self._backend.query_status(
                tracked.slurm_job_id, tracked.status
            )
            if new_status is None:
                logger.debug(
                    "Job %s (slurm %s): no status change from %s",
                    tracked.emx2_job_id,
                    tracked.slurm_job_id,
                    tracked.status,
                )
                continue

            logger.debug(
                "Job %s (slurm %s): %s → %s",
                tracked.emx2_job_id,
                tracked.slurm_job_id,
                tracked.status,
                new_status,
            )

            try:
                detail = f"Slurm state: {new_status}"
                output_artifact_id = None

                # On successful completion, upload output artifacts
                if new_status == "COMPLETED" and tracked.output_dir:
                    output_artifact_id = self._upload_output_artifacts(
                        tracked.emx2_job_id,
                        tracked.output_dir,
                        processor=tracked.processor,
                        profile=tracked.profile,
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
