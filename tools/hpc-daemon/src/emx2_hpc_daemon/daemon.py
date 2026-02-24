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
import json
import logging
import mimetypes
import signal
import socket
import time
from pathlib import Path

from .backend import ExecutionBackend, SimulatedBackend, SlurmBackend
from .client import ClaimConflict, HpcClient, format_links
from .config import DaemonConfig
from .profiles import derive_capabilities, resolve_profile
from .slurm import SlurmJobInfo
from .tracker import JobTracker

logger = logging.getLogger(__name__)

TERMINAL_STATUSES = frozenset({"COMPLETED", "FAILED", "CANCELLED"})


def _build_slurm_detail(info: SlurmJobInfo) -> str:
    """Build a structured detail string from Slurm job metadata.

    Example: "slurm_state=OUT_OF_MEMORY; exit_code=137:0; reason=OutOfMemory; node=gpu-01; elapsed=00:45:12"
    """
    parts = [f"slurm_state={info.state}"]
    if info.exit_code and info.exit_code != "0:0":
        parts.append(f"exit_code={info.exit_code}")
    if info.reason and info.reason not in ("None", ""):
        parts.append(f"reason={info.reason}")
    if info.node_list:
        parts.append(f"node={info.node_list}")
    if info.elapsed:
        parts.append(f"elapsed={info.elapsed}")
    return "; ".join(parts)


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
        state_db_path = self._resolve_state_db(config)
        self.tracker = JobTracker(state_db_path=state_db_path)
        self._running = True
        self._hostname = socket.gethostname()
        self._heartbeat_interval = 120  # seconds
        self._last_heartbeat = 0.0
        if simulate:
            logger.info("Running in SIMULATE mode — no real Slurm commands will be executed")

    @staticmethod
    def _resolve_state_db(config: DaemonConfig) -> Path:
        """Resolve the state DB path from config or default."""
        if config.worker.state_db:
            return Path(config.worker.state_db)
        default_dir = Path.home() / ".local" / "share" / "hpc-daemon"
        return default_dir / "state.json"

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
        """On startup, recover tracking state for non-terminal jobs.

        Strategy:
        1. Load local state DB (has output_dir, claimed_at, etc.)
        2. Query server for CLAIMED/SUBMITTED/STARTED jobs for this worker
        3. For each server job:
           a. If in local DB: update status from server (server is authority)
           b. If NOT in local DB: derive output_dir from config, add to tracker
        4. For each local DB job NOT on server: remove (stale)
        """
        # Step 1: load persisted state from previous invocations
        self.tracker.load_from_db()
        local_before = set(j.emx2_job_id for j in self.tracker.active_jobs())

        # Step 2: fetch server-known in-flight jobs for this worker
        in_flight_statuses = ("CLAIMED", "SUBMITTED", "STARTED")
        try:
            server_jobs: dict[str, dict] = {}
            for status in in_flight_statuses:
                jobs = self.client.list_jobs(status=status)
                for j in jobs:
                    if j.get("worker_id") == self.config.emx2.worker_id:
                        server_jobs[j["id"]] = j

            # Step 3: reconcile — server is authority for status
            for emx2_id, job_data in server_jobs.items():
                local = self.tracker.get(emx2_id)
                if local is not None:
                    # Update status from server (it's authoritative)
                    server_status = job_data.get("status", local.status)
                    if local.status != server_status:
                        self.tracker.update(emx2_id, status=server_status)
                        logger.debug(
                            "Updated job %s status from DB=%s to server=%s",
                            emx2_id, local.status, server_status,
                        )
                else:
                    # Not in local DB — derive dirs from config
                    output_dir = str(
                        Path(self.config.apptainer.tmp_dir) / emx2_id / "output"
                    )
                    self.tracker.track(
                        emx2_job_id=emx2_id,
                        slurm_job_id=job_data.get("slurm_job_id"),
                        status=job_data.get("status", "CLAIMED"),
                        processor=job_data.get("processor"),
                        profile=job_data.get("profile"),
                        output_dir=output_dir,
                        work_dir=str(
                            Path(self.config.apptainer.tmp_dir) / emx2_id
                        ),
                    )
                    logger.info(
                        "Recovered job %s from server (no local state, derived dirs)",
                        emx2_id,
                    )

            # Step 4: handle local-only entries not found in in-flight query.
            # These may have reached a terminal state between daemon cycles.
            for emx2_id in local_before - set(server_jobs.keys()):
                local = self.tracker.get(emx2_id)
                try:
                    server_job = self.client.get_job(emx2_id)
                    server_status = server_job.get("status", "")
                    if server_status == "CANCELLED" and local and local.slurm_job_id:
                        logger.info(
                            "Job %s cancelled on server, cancelling Slurm job %s",
                            emx2_id,
                            local.slurm_job_id,
                        )
                        try:
                            self._backend.cancel(local.slurm_job_id)
                        except Exception:
                            logger.exception(
                                "Failed to scancel job %s for cancelled job %s",
                                local.slurm_job_id,
                                emx2_id,
                            )
                    logger.info(
                        "Removed local job %s (server status: %s)",
                        emx2_id,
                        server_status,
                    )
                except Exception:
                    logger.info(
                        "Removed stale local job %s (not found on server)",
                        emx2_id,
                    )
                self.tracker.remove(emx2_id)

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

    @staticmethod
    def _classify_output_files(
        output_dir: str,
    ) -> tuple[list[Path], list[Path]]:
        """Classify files in output_dir into (output_files, log_files).

        Log files: slurm-*.out, slurm-*.err, container-stdout.log,
                   container-stderr.log, and any other *.log files.
        Excluded from both: .hpc_progress.json
        Output files: everything else.
        """
        output_path = Path(output_dir)
        output_files: list[Path] = []
        log_files: list[Path] = []

        for f in output_path.iterdir():
            if not f.is_file():
                continue
            if f.name == ".hpc_progress.json":
                continue
            if (
                f.name.startswith("slurm-")
                or f.name.endswith(".log")
            ):
                log_files.append(f)
            else:
                output_files.append(f)

        return output_files, log_files

    def _resolve_residence(
        self, processor: str | None, profile: str | None
    ) -> str:
        """Resolve artifact residence from profile config, defaulting to 'managed'."""
        residence = "managed"
        if processor:
            resolved = resolve_profile(self.config, processor, profile or "")
            if resolved:
                residence = resolved.artifact_residence
        return residence

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
        output_files, _ = self._classify_output_files(output_dir)

        if not output_files:
            logger.debug("No output files to upload for job %s", job_id)
            return None

        logger.debug(
            "Output files for job %s: %s",
            job_id,
            [f"{f.name} ({f.stat().st_size}b)" for f in output_files],
        )

        residence = self._resolve_residence(processor, profile)
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

    def _upload_log_artifact(
        self,
        job_id: str,
        output_dir: str,
        processor: str | None = None,
        profile: str | None = None,
    ) -> str | None:
        """Upload log files as a separate log artifact. Returns artifact ID or None."""
        _, log_files = self._classify_output_files(output_dir)

        if not log_files:
            logger.debug("No log files to upload for job %s", job_id)
            return None

        logger.debug(
            "Log files for job %s: %s",
            job_id,
            [f"{f.name} ({f.stat().st_size}b)" for f in log_files],
        )

        residence = self._resolve_residence(processor, profile)

        try:
            if residence == "posix":
                return self._register_posix_artifact(
                    job_id, output_dir, log_files, artifact_type="log",
                    name_prefix="log",
                )
            else:
                return self._upload_managed_artifact(
                    job_id, log_files, artifact_type="log", name_prefix="log",
                )
        except Exception:
            logger.exception("Failed to upload log artifacts for job %s", job_id)
            return None

    def _register_posix_artifact(
        self,
        job_id: str,
        output_dir: str,
        output_files: list[Path],
        artifact_type: str = "blob",
        name_prefix: str = "output",
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
            artifact_type=artifact_type,
            residence="posix",
            metadata={"job_id": job_id},
            content_url=content_url,
            name=f"{name_prefix}-{job_id[:8]}",
        )
        artifact_id = artifact["id"]
        logger.debug(
            "Created posix artifact %s, _links: %s",
            artifact_id,
            format_links(artifact.get("_links", {})),
        )

        # Compute hashes and register file metadata before committing
        # Tree hash: single file = file sha256; multi-file = SHA-256 of
        # concatenated "path:sha256" strings sorted by path (matches Java)
        file_hashes = []
        total_size = 0
        for f in sorted(output_files, key=lambda p: p.name):
            content = f.read_bytes()
            fhash = hashlib.sha256(content).hexdigest()
            fsize = len(content)
            file_hashes.append((f.name, fhash))
            total_size += fsize
            content_type = mimetypes.guess_type(f.name)[0] or "application/octet-stream"
            self.client.register_artifact_file(
                artifact_id,
                path=f.name,
                sha256=fhash,
                size_bytes=fsize,
                content_type=content_type,
            )

        if len(file_hashes) == 1:
            tree_hash = file_hashes[0][1]
        else:
            tree_str = "".join(f"{p}:{h}" for p, h in file_hashes)
            tree_hash = hashlib.sha256(tree_str.encode()).hexdigest()

        commit_result = self.client.commit_artifact(
            artifact_id,
            sha256=tree_hash,
            size_bytes=total_size,
        )
        logger.debug(
            "Committed posix artifact %s: sha256=%s, size=%d, _links: %s",
            artifact_id,
            tree_hash,
            total_size,
            format_links(commit_result.get("_links", {})),
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
        self,
        job_id: str,
        output_files: list[Path],
        artifact_type: str = "blob",
        name_prefix: str = "output",
    ) -> str:
        """Upload output files as a managed artifact with binary content."""
        logger.debug(
            "Creating managed artifact for job %s (%d files)",
            job_id,
            len(output_files),
        )
        artifact = self.client.create_artifact(
            artifact_type=artifact_type,
            residence="managed",
            metadata={"job_id": job_id},
            name=f"{name_prefix}-{job_id[:8]}",
        )
        artifact_id = artifact["id"]
        logger.debug(
            "Created managed artifact %s, _links: %s",
            artifact_id,
            format_links(artifact.get("_links", {})),
        )

        total_size = 0
        file_hashes: list[tuple[str, str]] = []  # (path, sha256hex)
        for f in sorted(output_files, key=lambda p: p.name):
            content = f.read_bytes()
            file_hash = hashlib.sha256(content).hexdigest()
            file_hashes.append((f.name, file_hash))
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

        # Tree hash: single file = file sha256; multi-file = SHA-256 of
        # concatenated "path:sha256" strings sorted by path (matches Java)
        if len(file_hashes) == 1:
            tree_hash = file_hashes[0][1]
        else:
            tree_str = "".join(f"{p}:{h}" for p, h in file_hashes)
            tree_hash = hashlib.sha256(tree_str.encode()).hexdigest()

        commit_result = self.client.commit_artifact(
            artifact_id,
            sha256=tree_hash,
            size_bytes=total_size,
        )
        logger.debug(
            "Committed managed artifact %s: sha256=%s, size=%d, _links: %s",
            artifact_id,
            tree_hash,
            total_size,
            format_links(commit_result.get("_links", {})),
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
                    f"for profile {tracked.profile_key}"
                )
                if tracked.slurm_job_id:
                    slurm_info = self._backend.query_slurm_info(tracked.slurm_job_id)
                    if slurm_info:
                        detail += f"; slurm_state={slurm_info.state}"
                        if slurm_info.reason and slurm_info.reason != "None":
                            detail += f"; reason={slurm_info.reason}"
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
                    f"for profile {tracked.profile_key}"
                )
                if tracked.slurm_job_id:
                    slurm_info = self._backend.query_slurm_info(tracked.slurm_job_id)
                    if slurm_info:
                        detail += f"; slurm_state={slurm_info.state}"
                        if slurm_info.reason and slurm_info.reason != "None":
                            detail += f"; reason={slurm_info.reason}"
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

    def _check_progress_file(self, tracked) -> None:
        """Read .hpc_progress.json from output_dir and relay updates to EMX2."""
        if tracked.status != "STARTED" or not tracked.output_dir:
            return

        progress_path = Path(tracked.output_dir) / ".hpc_progress.json"
        if not progress_path.is_file():
            return

        try:
            raw = progress_path.read_bytes()
            content_hash = hashlib.md5(raw).hexdigest()
            if content_hash == tracked.last_progress_hash:
                return

            tracked.last_progress_hash = content_hash
            progress = json.loads(raw)

            parts = []
            if "phase" in progress:
                parts.append(progress["phase"])
            if "message" in progress:
                parts.append(progress["message"])
            if "progress" in progress:
                parts.append(f"{progress['progress']:.0%}")
            detail = "; ".join(parts) if parts else "progress update"

            self.client.transition_job(
                tracked.emx2_job_id,
                "STARTED",
                detail=f"progress: {detail}",
            )
            logger.debug(
                "Relayed progress for job %s: %s",
                tracked.emx2_job_id,
                detail,
            )
        except (json.JSONDecodeError, OSError):
            logger.debug(
                "Could not read progress file for job %s",
                tracked.emx2_job_id,
            )

    def _report_queue_status(self) -> None:
        """Report Slurm queue status for jobs stuck in SUBMITTED state.

        Periodically posts a same-state transition with the Slurm PENDING
        reason so EMX2 has visibility into queued jobs.
        """
        interval = self.config.worker.queue_report_interval_seconds
        if interval <= 0:
            return

        now = time.monotonic()
        for tracked in self.tracker.active_jobs():
            if tracked.status != "SUBMITTED" or not tracked.slurm_job_id:
                continue
            if (now - tracked.last_queue_report) < interval:
                continue

            slurm_info = self._backend.query_slurm_info(tracked.slurm_job_id)
            if slurm_info is None or slurm_info.state != "PENDING":
                continue

            elapsed_s = int(now - tracked.claimed_at)
            detail = _build_slurm_detail(slurm_info) + f"; queued={elapsed_s}s"
            try:
                self.client.transition_job(
                    tracked.emx2_job_id, "SUBMITTED", detail=detail
                )
                tracked.last_queue_report = now
                logger.info(
                    "Queue status for job %s: %s", tracked.emx2_job_id, detail
                )
            except Exception:
                logger.exception(
                    "Failed to report queue status for job %s",
                    tracked.emx2_job_id,
                )

    def _monitor_running_jobs(self) -> None:
        """Check status of all tracked jobs and report transitions."""
        self._check_server_cancellations()
        self._check_profile_timeouts()
        self._report_queue_status()

        for tracked in self.tracker.active_jobs():
            if not tracked.slurm_job_id:
                continue

            # Relay in-flight progress before checking for state changes
            self._check_progress_file(tracked)

            result = self._backend.query_status(
                tracked.slurm_job_id, tracked.status
            )
            if result is None:
                logger.debug(
                    "Job %s (slurm %s): no status change from %s",
                    tracked.emx2_job_id,
                    tracked.slurm_job_id,
                    tracked.status,
                )
                continue

            new_status = result.hpc_status
            info = result.slurm_info

            logger.debug(
                "Job %s (slurm %s): %s → %s",
                tracked.emx2_job_id,
                tracked.slurm_job_id,
                tracked.status,
                new_status,
            )

            try:
                # If the job skipped STARTED (fast jobs go PENDING→COMPLETED
                # in Slurm before the daemon sees RUNNING), insert the
                # intermediate transition that the EMX2 state machine requires.
                if (
                    tracked.status == "SUBMITTED"
                    and new_status in ("COMPLETED", "FAILED")
                ):
                    logger.info(
                        "Job %s skipped STARTED (fast job), inserting intermediate transition",
                        tracked.emx2_job_id,
                    )
                    self.client.transition_job(
                        tracked.emx2_job_id,
                        "STARTED",
                        detail=f"slurm_state={info.state} (fast job, retroactive STARTED)",
                    )
                    tracked.status = "STARTED"
                    self.tracker.update(tracked.emx2_job_id, status="STARTED")

                detail = _build_slurm_detail(info)
                output_artifact_id = None
                log_artifact_id = None

                # On completion or failure, upload artifacts
                if new_status in ("COMPLETED", "FAILED") and tracked.output_dir:
                    # Upload log artifact (useful on both success and failure)
                    log_artifact_id = self._upload_log_artifact(
                        tracked.emx2_job_id,
                        tracked.output_dir,
                        processor=tracked.processor,
                        profile=tracked.profile,
                    )
                    if log_artifact_id:
                        detail += f"; log_artifact={log_artifact_id}"

                    # Upload output artifacts (primarily on success)
                    if new_status == "COMPLETED":
                        output_artifact_id = self._upload_output_artifacts(
                            tracked.emx2_job_id,
                            tracked.output_dir,
                            processor=tracked.processor,
                            profile=tracked.profile,
                        )
                        if output_artifact_id:
                            detail += f"; output_artifact={output_artifact_id}"

                self.client.transition_job(
                    tracked.emx2_job_id,
                    new_status,
                    detail=detail,
                    output_artifact_id=output_artifact_id,
                    log_artifact_id=log_artifact_id,
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
