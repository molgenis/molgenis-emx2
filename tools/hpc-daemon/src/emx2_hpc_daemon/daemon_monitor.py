"""Job monitoring, completion, and recovery logic extracted from HpcDaemon.

Functions that need many daemon attributes accept the daemon instance
as a context object. Simpler functions receive explicit dependencies.
"""

from __future__ import annotations

import logging
import time
from pathlib import Path
from typing import TYPE_CHECKING

from ._generated import TERMINAL_STATUSES
from .backend import ExecutionBackend
from .client import HpcClient
from .config import DaemonConfig
from .daemon_artifacts import upload_log_artifact, upload_output_artifacts
from .daemon_progress import check_progress_file
from .profiles import resolve_profile
from .slurm import SlurmJobInfo
from .tracker import JobTracker, TrackedJob

if TYPE_CHECKING:
    from .daemon import HpcDaemon

logger = logging.getLogger(__name__)


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


def monitor_running_jobs(daemon: HpcDaemon) -> None:
    """Check status of all tracked jobs and report transitions."""
    check_server_cancellations(daemon.client, daemon.tracker, daemon._backend)
    check_profile_timeouts(daemon.client, daemon.tracker, daemon.config, daemon._backend)
    report_queue_status(daemon.client, daemon.tracker, daemon.config, daemon._backend)

    for tracked in daemon.tracker.active_jobs():
        if not tracked.slurm_job_id:
            continue
        check_progress_file(daemon.client, tracked)
        process_job_status_change(daemon, tracked)


def process_job_status_change(daemon: HpcDaemon, tracked: TrackedJob) -> None:
    """Query Slurm for a single job and report any status change to EMX2."""
    result = daemon._backend.query_status(tracked.slurm_job_id, tracked.status)
    if result is None:
        logger.debug(
            "Job %s (slurm %s): no status change from %s",
            tracked.emx2_job_id,
            tracked.slurm_job_id,
            tracked.status,
        )
        return

    new_status = result.hpc_status
    info = result.slurm_info

    logger.debug(
        "Job %s (slurm %s): %s -> %s",
        tracked.emx2_job_id,
        tracked.slurm_job_id,
        tracked.status,
        new_status,
    )

    try:
        # If the job skipped STARTED (fast jobs go PENDING->COMPLETED
        # in Slurm before the daemon sees RUNNING), insert the
        # intermediate transition that the EMX2 state machine requires.
        if tracked.status == "SUBMITTED" and new_status in (
            "COMPLETED",
            "FAILED",
        ):
            logger.info(
                "Job %s skipped STARTED (fast job), inserting intermediate transition",
                tracked.emx2_job_id,
            )
            daemon.client.transition_job(
                tracked.emx2_job_id,
                "STARTED",
                detail=f"slurm_state={info.state} (fast job, retroactive STARTED)",
            )
            tracked.status = "STARTED"
            daemon.tracker.update(tracked.emx2_job_id, status="STARTED")
            # Fast jobs can finish before we observe RUNNING. After inserting
            # the required STARTED state, force one progress-file pass so
            # .hpc_progress.jsonl updates are not lost.
            check_progress_file(daemon.client, tracked)

        # Terminal transitions use phased completion with tracker
        # checkpointing to avoid duplicate artifacts on crash recovery.
        if new_status in TERMINAL_STATUSES:
            complete_job(daemon, tracked, new_status, info)
        else:
            # Non-terminal transition (e.g. SUBMITTED -> STARTED)
            detail = _build_slurm_detail(info)
            daemon.client.transition_job(
                tracked.emx2_job_id,
                new_status,
                detail=detail,
            )
            tracked.status = new_status
            daemon.tracker.update(tracked.emx2_job_id, status=new_status)
            logger.info(
                "Job %s transitioned to %s",
                tracked.emx2_job_id,
                new_status,
            )

    except Exception:
        logger.exception(
            "Failed to report transition for job %s", tracked.emx2_job_id
        )


def complete_job(
    daemon: HpcDaemon,
    tracked: TrackedJob,
    new_status: str,
    info: SlurmJobInfo,
) -> None:
    """Phased completion with tracker checkpointing for crash recovery.

    Phases:
    1. Upload log artifact (skip if already done on a previous attempt)
    2. Upload output artifact (skip if already done)
    3. Call /complete endpoint (idempotent)
    4. Remove from tracker

    Each phase is checkpointed in the tracker's SQLite DB so that a
    crash mid-sequence can resume without creating duplicate artifacts.
    """
    detail = _build_slurm_detail(info)

    # Phase 1: Upload log artifact (skip if already recorded)
    if tracked.log_artifact_id is None and tracked.output_dir:
        log_artifact_id = upload_log_artifact(
            daemon.client,
            daemon.config,
            tracked,
            tracked.output_dir,
            processor=tracked.processor,
            profile=tracked.profile,
        )
        if log_artifact_id:
            daemon.tracker.update(
                tracked.emx2_job_id,
                log_artifact_id=log_artifact_id,
                completion_phase="log_uploaded",
            )
            tracked.log_artifact_id = log_artifact_id
    log_artifact_id = tracked.log_artifact_id
    if log_artifact_id:
        detail += f"; log_artifact={log_artifact_id}"

    # Phase 2: Upload output artifact (skip if already recorded)
    output_artifact_id = tracked.output_artifact_id
    if output_artifact_id is None and new_status == "COMPLETED" and tracked.output_dir:
        output_artifact_id = upload_output_artifacts(
            daemon.client,
            daemon.config,
            tracked,
            tracked.output_dir,
            processor=tracked.processor,
            profile=tracked.profile,
        )
        if output_artifact_id:
            daemon.tracker.update(
                tracked.emx2_job_id,
                output_artifact_id=output_artifact_id,
                completion_phase="output_uploaded",
            )
            tracked.output_artifact_id = output_artifact_id
    if output_artifact_id:
        detail += f"; output_artifact={output_artifact_id}"

    # Phase 3: Atomic completion via /complete endpoint
    daemon.tracker.update(tracked.emx2_job_id, completion_phase="transitioning")
    daemon.client.complete_job(
        tracked.emx2_job_id,
        new_status,
        detail=detail,
        slurm_job_id=tracked.slurm_job_id,
        output_artifact_id=output_artifact_id,
        log_artifact_id=log_artifact_id,
    )
    tracked.status = new_status
    logger.info(
        "Job %s completed with status %s",
        tracked.emx2_job_id,
        new_status,
    )

    # Phase 4: Remove from tracker
    daemon.tracker.remove(tracked.emx2_job_id)


def check_server_cancellations(
    client: HpcClient, tracker: JobTracker, backend: ExecutionBackend
) -> None:
    """Check if any tracked jobs were cancelled server-side and propagate."""
    for tracked in tracker.active_jobs():
        try:
            job = client.get_job(tracked.emx2_job_id)
            server_status = job.get("status", "")
            if server_status == "CANCELLED" and tracked.status != "CANCELLED":
                logger.info(
                    "Job %s cancelled server-side, cancelling Slurm job %s",
                    tracked.emx2_job_id,
                    tracked.slurm_job_id,
                )
                if tracked.slurm_job_id:
                    backend.cancel(tracked.slurm_job_id)
                tracked.status = "CANCELLED"
                tracker.remove(tracked.emx2_job_id)
        except Exception:
            logger.debug(
                "Could not check cancellation status for job %s",
                tracked.emx2_job_id,
            )


def _fail_job_with_timeout(
    client: HpcClient,
    tracker: JobTracker,
    backend: ExecutionBackend,
    tracked: TrackedJob,
    detail: str,
    cancel_slurm: bool,
) -> None:
    """Cancel the Slurm job (if requested), transition to FAILED, and untrack."""
    if cancel_slurm and tracked.slurm_job_id:
        try:
            backend.cancel(tracked.slurm_job_id)
        except Exception:
            logger.exception("Failed to scancel job %s", tracked.slurm_job_id)
    try:
        client.transition_job(tracked.emx2_job_id, "FAILED", detail=detail)
    except Exception:
        logger.exception(
            "Failed to report timeout for job %s", tracked.emx2_job_id
        )
    tracker.remove(tracked.emx2_job_id)


def _append_slurm_detail(
    detail: str, backend: ExecutionBackend, slurm_job_id: str | None
) -> str:
    """Append Slurm state info to a timeout detail string."""
    if not slurm_job_id:
        return detail
    slurm_info = backend.query_slurm_info(slurm_job_id)
    if slurm_info:
        detail += f"; slurm_state={slurm_info.state}"
        if slurm_info.reason and slurm_info.reason != "None":
            detail += f"; reason={slurm_info.reason}"
    return detail


def check_profile_timeouts(
    client: HpcClient,
    tracker: JobTracker,
    config: DaemonConfig,
    backend: ExecutionBackend,
) -> None:
    """Check if any tracked jobs have exceeded their per-job or profile timeouts."""
    now = time.monotonic()
    for tracked in list(tracker.active_jobs()):
        elapsed = now - tracked.claimed_at

        # Per-job timeout (set by the submitter) takes priority.
        # Applies to any non-terminal status once the job is tracked.
        if (
            tracked.timeout_seconds is not None
            and tracked.timeout_seconds > 0
            and elapsed > tracked.timeout_seconds
        ):
            detail = (
                f"timeout: job timeout_seconds "
                f"({tracked.timeout_seconds}s) exceeded after {int(elapsed)}s"
            )
            detail = _append_slurm_detail(detail, backend, tracked.slurm_job_id)
            logger.warning(
                "Job %s exceeded per-job timeout: %s",
                tracked.emx2_job_id,
                detail,
            )
            _fail_job_with_timeout(
                client, tracker, backend, tracked, detail,
                cancel_slurm=(tracked.status in ("SUBMITTED", "STARTED")),
            )
            continue

        # Profile-level timeouts from daemon config
        resolved = resolve_profile(
            config, tracked.processor or "", tracked.profile or ""
        )
        if resolved is None:
            continue

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
            detail = _append_slurm_detail(detail, backend, tracked.slurm_job_id)
            logger.warning(
                "Job %s exceeded claim timeout: %s",
                tracked.emx2_job_id,
                detail,
            )
            _fail_job_with_timeout(
                client, tracker, backend, tracked, detail, cancel_slurm=False
            )

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
            detail = _append_slurm_detail(detail, backend, tracked.slurm_job_id)
            logger.warning(
                "Job %s exceeded execution timeout: %s",
                tracked.emx2_job_id,
                detail,
            )
            _fail_job_with_timeout(
                client, tracker, backend, tracked, detail, cancel_slurm=True
            )


def recover_jobs(
    client: HpcClient,
    tracker: JobTracker,
    config: DaemonConfig,
    backend: ExecutionBackend,
) -> None:
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
    tracker.load_from_db()
    local_before = set(j.emx2_job_id for j in tracker.active_jobs())

    # Step 2: fetch server-known in-flight jobs for this worker
    in_flight_statuses = ("CLAIMED", "SUBMITTED", "STARTED")
    try:
        server_jobs: dict[str, dict] = {}
        for status in in_flight_statuses:
            jobs = client.list_jobs(status=status)
            for j in jobs:
                if j.get("worker_id") == config.emx2.worker_id:
                    server_jobs[j["id"]] = j

        # Step 3: reconcile -- server is authority for status
        for emx2_id, job_data in server_jobs.items():
            local = tracker.get(emx2_id)
            if local is not None:
                # Update status from server (it's authoritative)
                server_status = job_data.get("status", local.status)
                if local.status != server_status:
                    tracker.update(emx2_id, status=server_status)
                    logger.debug(
                        "Updated job %s status from DB=%s to server=%s",
                        emx2_id,
                        local.status,
                        server_status,
                    )
            else:
                # Not in local DB -- derive dirs from config
                output_dir = str(
                    Path(config.apptainer.tmp_dir) / emx2_id / "output"
                )
                tracker.track(
                    emx2_job_id=emx2_id,
                    slurm_job_id=job_data.get("slurm_job_id"),
                    status=job_data.get("status", "CLAIMED"),
                    processor=job_data.get("processor"),
                    profile=job_data.get("profile"),
                    output_dir=output_dir,
                    work_dir=str(Path(config.apptainer.tmp_dir) / emx2_id),
                )
                logger.info(
                    "Recovered job %s from server (no local state, derived dirs)",
                    emx2_id,
                )

        # Step 4: handle local-only entries not found in in-flight query.
        # These may have reached a terminal state between daemon cycles.
        for emx2_id in local_before - set(server_jobs.keys()):
            local = tracker.get(emx2_id)
            try:
                server_job = client.get_job(emx2_id)
                server_status = server_job.get("status", "")
                if server_status == "CANCELLED" and local and local.slurm_job_id:
                    logger.info(
                        "Job %s cancelled on server, cancelling Slurm job %s",
                        emx2_id,
                        local.slurm_job_id,
                    )
                    try:
                        backend.cancel(local.slurm_job_id)
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
                logger.warning(
                    "Removed stale local job %s (not found on server)",
                    emx2_id,
                    exc_info=True,
                )
            tracker.remove(emx2_id)

        if tracker.active_count() > 0:
            logger.info(
                "Recovered %d in-flight jobs from previous run",
                tracker.active_count(),
            )
    except Exception:
        logger.exception(
            "Failed to recover jobs from server (%d locally loaded jobs)",
            len(local_before),
        )


def report_queue_status(
    client: HpcClient,
    tracker: JobTracker,
    config: DaemonConfig,
    backend: ExecutionBackend,
) -> None:
    """Report Slurm queue status for jobs stuck in SUBMITTED state.

    Periodically posts a same-state transition with the Slurm PENDING
    reason so EMX2 has visibility into queued jobs.
    """
    interval = config.worker.queue_report_interval_seconds
    if interval <= 0:
        return

    now = time.monotonic()
    for tracked in tracker.active_jobs():
        if tracked.status != "SUBMITTED" or not tracked.slurm_job_id:
            continue
        if (now - tracked.last_queue_report) < interval:
            continue

        slurm_info = backend.query_slurm_info(tracked.slurm_job_id)
        if slurm_info is None or slurm_info.state != "PENDING":
            continue

        elapsed_s = int(now - tracked.claimed_at)
        detail = _build_slurm_detail(slurm_info) + f"; queued={elapsed_s}s"
        try:
            client.transition_job(
                tracked.emx2_job_id, "SUBMITTED", detail=detail
            )
            tracked.last_queue_report = now
            logger.info("Queue status for job %s: %s", tracked.emx2_job_id, detail)
        except Exception:
            logger.exception(
                "Failed to report queue status for job %s",
                tracked.emx2_job_id,
            )
