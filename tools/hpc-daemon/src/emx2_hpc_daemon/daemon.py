"""Main daemon loop: register -> poll -> claim -> submit -> monitor -> report.

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
import json
import logging
import signal
import socket
import time
from pathlib import Path

from .backend import (
    ExecutionBackend,
    ShellBackend,
    SimulatedBackend,
    SlurmBackend,
    _normalize_input_artifact_ids,
)
from .client import ClaimConflict, HpcClient, NotFoundError
from .config import DaemonConfig
from .daemon_monitor import monitor_running_jobs, recover_jobs
from .profiles import derive_capabilities
from .tracker import JobTracker

logger = logging.getLogger(__name__)


class HpcDaemon:
    """The main daemon orchestrating HPC job execution."""

    def __init__(self, config: DaemonConfig, backend: str = "slurm"):
        self.config = config
        self._backend: ExecutionBackend = self._make_backend(backend, config)
        self.client = HpcClient(
            base_url=config.emx2.base_url,
            worker_id=config.emx2.worker_id,
            worker_secret=config.emx2.worker_secret,
            auth_mode=config.emx2.auth_mode,
        )
        state_db_path = self._resolve_state_db(config)
        self.tracker = JobTracker(state_db_path=state_db_path)
        self._running = True
        self._hostname = socket.gethostname()
        self._heartbeat_interval = config.worker.heartbeat_interval_seconds
        self._last_heartbeat = 0.0
        if backend != "slurm":
            logger.info(
                "Running with %s backend — no Slurm commands will be executed",
                backend,
            )

    @staticmethod
    def _make_backend(backend: str, config: DaemonConfig) -> ExecutionBackend:
        if backend == "simulate":
            return SimulatedBackend()
        if backend == "shell":
            return ShellBackend(config)
        return SlurmBackend(config)

    @staticmethod
    def _resolve_state_db(config: DaemonConfig) -> Path:
        """Resolve the state DB path from config or default."""
        if config.worker.state_db:
            return Path(config.worker.state_db)
        default_dir = Path.home() / ".local" / "share" / "hpc-daemon"
        return default_dir / "state.db"

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
            self.register()

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
            self.tracker.close()
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
            self.register()
            self._recover_jobs()
            self._poll_and_claim()
            self._monitor_running_jobs()
            logger.info(
                "Single cycle complete, %d active jobs", self.tracker.active_count()
            )
        finally:
            self.tracker.close()
            self.client.close()

    def register(self) -> None:
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
        """On startup, recover tracking state for non-terminal jobs."""
        recover_jobs(self.client, self.tracker, self.config, self._backend)

    def _maybe_heartbeat(self) -> None:
        """Send periodic heartbeat to keep worker registration alive."""
        if time.monotonic() - self._last_heartbeat > self._heartbeat_interval:
            try:
                self.client.heartbeat()
                self._last_heartbeat = time.monotonic()
                logger.debug("Heartbeat sent")
            except NotFoundError:
                logger.warning(
                    "Heartbeat rejected because worker is no longer registered; re-registering"
                )
                self.register()
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
                if (
                    self.tracker.active_count()
                    >= self.config.worker.max_concurrent_jobs
                ):
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

    @staticmethod
    def _compute_parameters_hash(parameters) -> str | None:
        """Compute a SHA-256 hash of the job parameters for provenance tracking."""
        if not parameters:
            return None
        raw = json.dumps(parameters, sort_keys=True, separators=(",", ":"))
        return hashlib.sha256(raw.encode()).hexdigest()

    @staticmethod
    def _extract_input_artifact_ids(inputs) -> str | None:
        """Extract input artifact IDs as a JSON array string."""
        ids = _normalize_input_artifact_ids(inputs)
        if not ids:
            return None
        return json.dumps(ids)

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

        def ensure_claimed_tracking() -> None:
            if self.tracker.get(job_id) is not None:
                return
            self.tracker.track(
                emx2_job_id=job_id,
                status="CLAIMED",
                processor=job.get("processor"),
                profile=job.get("profile"),
                submit_user=job.get("submit_user"),
                input_artifact_ids=self._extract_input_artifact_ids(job.get("inputs")),
                parameters_hash=self._compute_parameters_hash(job.get("parameters")),
                timeout_seconds=job.get("timeout_seconds"),
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
                submit_user=job.get("submit_user"),
                input_artifact_ids=self._extract_input_artifact_ids(job.get("inputs")),
                parameters_hash=self._compute_parameters_hash(job.get("parameters")),
                timeout_seconds=job.get("timeout_seconds"),
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
            ensure_claimed_tracking()
            try:
                self.client.transition_job(job_id, "FAILED", detail=str(e))
                self.tracker.remove(job_id)
            except Exception:
                logger.exception(
                    "Failed to report submit validation failure for job %s", job_id
                )
        except Exception as e:
            logger.exception("Failed to submit job %s", job_id)
            ensure_claimed_tracking()
            try:
                self.client.transition_job(job_id, "FAILED", detail=str(e))
                self.tracker.remove(job_id)
            except Exception:
                logger.exception("Failed to report submit failure for job %s", job_id)

    def _monitor_running_jobs(self) -> None:
        """Check status of all tracked jobs and report transitions."""
        monitor_running_jobs(self)

    def _check_progress_file(self, tracked) -> None:
        """Read .hpc_progress.jsonl and relay updates to EMX2."""
        from .daemon_progress import check_progress_file

        check_progress_file(self.client, tracked)

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
