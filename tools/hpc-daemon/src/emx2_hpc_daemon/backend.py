"""Execution backend strategy: abstracts Slurm, shell, and simulated execution.

The daemon delegates all execution operations to an ExecutionBackend,
keeping a single code path regardless of execution mode.
"""

from __future__ import annotations

import json
import logging
import os
import signal
import shutil
import subprocess
import tempfile
from abc import ABC, abstractmethod
from dataclasses import dataclass
from pathlib import Path

from .client import HpcClient, format_links
from .config import DaemonConfig
from .hashing import compute_tree_hash_from_paths
from .profiles import resolve_profile
from .slurm import (
    SlurmJobInfo,
    cancel_job,
    generate_batch_script,
    query_status,
    submit_job,
)

logger = logging.getLogger(__name__)


# Map Slurm states to EMX2 HPC job statuses
SLURM_TO_HPC_STATUS = {
    "PENDING": None,  # Still waiting in Slurm queue, no transition needed
    "RUNNING": "STARTED",
    "COMPLETED": "COMPLETED",
    "FAILED": "FAILED",
    "CANCELLED": "CANCELLED",
    "TIMEOUT": "FAILED",
    "OUT_OF_MEMORY": "FAILED",
    "NODE_FAIL": "FAILED",
}


def _verify_artifact_hash(artifact: dict, artifact_dir: Path, artifact_id: str) -> None:
    """Verify SHA-256 of staged input artifact files against expected hash.

    Raises ValueError("input_hash_mismatch") on mismatch.
    """
    expected_hash = artifact.get("sha256")
    if not expected_hash:
        logger.warning(
            "Artifact %s has no sha256 — skipping hash verification",
            artifact_id,
        )
        return

    # Collect all files in the artifact directory
    if not artifact_dir.is_dir():
        # Single file — check if it's a file directly
        if artifact_dir.is_file():
            actual_hash, _ = compute_tree_hash_from_paths(
                [(artifact_dir.name, artifact_dir)]
            )
        else:
            logger.warning(
                "Artifact %s directory not found at %s — skipping hash verification",
                artifact_id,
                artifact_dir,
            )
            return
    else:
        file_list = sorted(f for f in artifact_dir.rglob("*") if f.is_file())
        if not file_list:
            logger.warning(
                "No files found in artifact %s at %s — skipping hash verification",
                artifact_id,
                artifact_dir,
            )
            return

        # Build (relative_path, file_path) pairs
        pairs: list[tuple[str, Path]] = []
        for f in file_list:
            rel_path = str(f.relative_to(artifact_dir))
            pairs.append((rel_path, f))
        actual_hash, _ = compute_tree_hash_from_paths(pairs)

    if actual_hash != expected_hash:
        raise ValueError(
            f"input_hash_mismatch: artifact {artifact_id} "
            f"expected={expected_hash} actual={actual_hash}"
        )
    logger.debug("Hash verified for artifact %s: %s", artifact_id, actual_hash)


@dataclass
class SubmitResult:
    """Result of submitting a job to an execution backend."""

    slurm_job_id: str
    work_dir: str | None = None
    input_dir: str | None = None
    output_dir: str | None = None


@dataclass
class StatusResult:
    """Result of a status query: the new HPC status plus Slurm metadata."""

    hpc_status: str  # e.g. "STARTED", "COMPLETED", "FAILED"
    slurm_info: SlurmJobInfo  # full Slurm metadata for detail strings


class ExecutionBackend(ABC):
    """Strategy interface for job execution."""

    @abstractmethod
    def submit(self, job: dict, client: HpcClient) -> SubmitResult:
        """Submit a job for execution. Returns tracking info.

        Raises ValueError if the job cannot be submitted (e.g. no matching profile).
        """

    @abstractmethod
    def query_status(
        self, slurm_job_id: str, current_status: str
    ) -> StatusResult | None:
        """Query the current status of a submitted job.

        Returns a StatusResult with the new HPC status and Slurm metadata,
        or None if the status hasn't changed.
        """

    def query_slurm_info(self, slurm_job_id: str) -> SlurmJobInfo | None:
        """Query raw Slurm info without status mapping. Override for real Slurm."""
        return None

    @abstractmethod
    def cancel(self, slurm_job_id: str) -> None:
        """Cancel a running job."""


class SlurmBackend(ExecutionBackend):
    """Real Slurm execution via sbatch/squeue/scancel."""

    def __init__(self, config: DaemonConfig):
        self._config = config

    def submit(self, job: dict, client: HpcClient) -> SubmitResult:
        job_id = job["id"]
        processor = job.get("processor", "")
        profile = job.get("profile", "")

        resolved = resolve_profile(self._config, processor, profile)
        if resolved is None:
            raise ValueError(f"No profile for {processor}:{profile}")

        logger.debug(
            "Resolved profile for %s:%s → partition=%s, cpus=%d, mem=%s, "
            "time=%s, sif=%s, entrypoint=%s, output_residence=%s, log_residence=%s",
            processor,
            profile,
            resolved.partition,
            resolved.cpus,
            resolved.memory,
            resolved.time,
            resolved.sif_image,
            resolved.entrypoint,
            resolved.output_residence,
            resolved.log_residence,
        )

        # Create working directories
        base_dir = Path(self._config.apptainer.tmp_dir) / job_id
        work_dir = base_dir / "work"
        input_dir = base_dir / "input"
        output_dir = base_dir / "output"
        for d in (work_dir, input_dir, output_dir):
            d.mkdir(parents=True, exist_ok=True)

        # Stage input artifacts
        _stage_input_artifacts(job, str(input_dir), client)

        # Determine container command from job parameters
        container_command = None
        environment = None
        parameters = job.get("parameters")
        if parameters:
            if isinstance(parameters, str):
                try:
                    parameters = json.loads(parameters)
                except (json.JSONDecodeError, TypeError):
                    parameters = {}
            if isinstance(parameters, dict):
                container_command = parameters.get("command")
                environment = parameters.get("environment")

        logger.debug(
            "Job %s parameters: %s (command=%s, env=%s)",
            job_id,
            job.get("parameters"),
            container_command,
            environment,
        )

        # Generate batch script
        script_content = generate_batch_script(
            job_id=job_id,
            sif_image=resolved.sif_image,
            partition=resolved.partition,
            cpus=resolved.cpus,
            memory=resolved.memory,
            time_limit=resolved.time,
            work_dir=str(work_dir),
            input_dir=str(input_dir),
            output_dir=str(output_dir),
            sbatch_args=resolved.sbatch_args,
            bind_paths=self._config.apptainer.bind_paths
            if not resolved.entrypoint
            else None,
            account=self._config.slurm.default_account or None,
            container_command=container_command,
            environment=environment,
            entrypoint=resolved.entrypoint or None,
            parameters=parameters if isinstance(parameters, dict) else None,
        )

        script_path = base_dir / "job.sbatch"
        script_path.write_text(script_content)

        # Submit to Slurm
        slurm_id = submit_job(script_path)
        logger.info("Submitted job %s as Slurm job %s", job_id, slurm_id)

        return SubmitResult(
            slurm_job_id=slurm_id,
            work_dir=str(work_dir),
            input_dir=str(input_dir),
            output_dir=str(output_dir),
        )

    def query_status(
        self, slurm_job_id: str, current_status: str
    ) -> StatusResult | None:
        info = query_status(slurm_job_id)
        hpc_status = SLURM_TO_HPC_STATUS.get(info.state)
        if hpc_status is None or hpc_status == current_status:
            return None
        return StatusResult(hpc_status=hpc_status, slurm_info=info)

    def query_slurm_info(self, slurm_job_id: str) -> SlurmJobInfo | None:
        return query_status(slurm_job_id)

    def cancel(self, slurm_job_id: str) -> None:
        cancel_job(slurm_job_id)


def _stage_input_artifacts(job: dict, input_dir: str, client: HpcClient) -> None:
    """Download or symlink input artifacts to the job's input directory.

    For posix artifacts with file:// content_url, creates symlinks.
    For managed artifacts, downloads via GET.
    """
    inputs = job.get("inputs")
    if not inputs:
        logger.debug("Job %s has no input artifacts", job.get("id", "?"))
        return

    if isinstance(inputs, str):
        try:
            inputs = json.loads(inputs)
        except (json.JSONDecodeError, TypeError):
            logger.warning("Could not parse job inputs: %s", inputs)
            return

    logger.debug(
        "Staging %d input artifact(s) for job %s: %s",
        len(inputs) if isinstance(inputs, list) else 0,
        job.get("id", "?"),
        inputs,
    )

    artifact_ids = _normalize_input_artifact_ids(inputs)
    logger.debug(
        "Normalized input artifacts for job %s: %s",
        job.get("id", "?"),
        artifact_ids,
    )
    for artifact_id in artifact_ids:
        try:
            artifact = client.get_artifact(artifact_id)
            residence = artifact.get("residence", "managed")
            content_url = artifact.get("content_url")
            logger.debug(
                "Input artifact %s: type=%s, format=%s, "
                "residence=%s, status=%s, sha256=%s, "
                "size_bytes=%s, content_url=%s",
                artifact_id,
                artifact.get("type"),
                artifact.get("format"),
                residence,
                artifact.get("status"),
                artifact.get("sha256"),
                artifact.get("size_bytes"),
                content_url,
            )

            # Log artifact's HATEOAS links
            links = artifact.get("_links", {})
            if links:
                logger.debug(
                    "Input artifact %s _links: %s",
                    artifact_id,
                    format_links(links),
                )

            if residence == "posix" and content_url and content_url.startswith("file://"):
                # Symlink posix artifact directory
                posix_path = content_url[len("file://") :]
                link_path = Path(input_dir) / artifact_id
                link_path.symlink_to(posix_path)
                logger.info(
                    "Symlinked posix artifact %s: %s -> %s",
                    artifact_id,
                    link_path,
                    posix_path,
                )
                # Verify hash for posix artifacts
                _verify_artifact_hash(
                    artifact, Path(posix_path), artifact_id
                )
            else:
                # Download managed artifact files
                artifact_dir = Path(input_dir) / artifact_id
                if artifact_dir.exists():
                    if artifact_dir.is_dir() and not artifact_dir.is_symlink():
                        shutil.rmtree(artifact_dir)
                    else:
                        artifact_dir.unlink()
                artifact_dir.mkdir(parents=True, exist_ok=True)
                files = client.list_artifact_files(artifact_id)
                logger.debug(
                    "Artifact %s has %d file(s): %s",
                    artifact_id,
                    len(files),
                    [f"{f.get('path')} ({f.get('size_bytes', '?')}b)" for f in files],
                )
                downloaded = client.download_artifact_files(
                    artifact_id, str(artifact_dir)
                )
                logger.info(
                    "Staged %d files from artifact %s",
                    len(downloaded),
                    artifact_id,
                )
                # Verify hash for managed artifacts
                _verify_artifact_hash(artifact, artifact_dir, artifact_id)
        except Exception:
            logger.exception("Failed to stage artifact %s", artifact_id)
            raise


def _normalize_input_artifact_ids(inputs: object) -> list[str]:
    """Normalize supported job input formats into unique artifact IDs.

    Supported forms:
    - ["id-1", "id-2"]
    - [{"artifact_id": "id-1"}]
    - [{"dataset": "id-1"}]
    - {"dataset": "id-1", "reference": "id-2"}
    """
    items: list[object]
    if isinstance(inputs, list):
        items = inputs
    elif isinstance(inputs, dict):
        items = [inputs]
    else:
        return []

    ids: list[str] = []
    seen: set[str] = set()

    def add_if_new(candidate: object) -> None:
        if isinstance(candidate, str):
            artifact_id = candidate.strip()
            if artifact_id and artifact_id not in seen:
                seen.add(artifact_id)
                ids.append(artifact_id)

    for item in items:
        if isinstance(item, str):
            add_if_new(item)
            continue
        if not isinstance(item, dict):
            continue

        artifact_id = item.get("artifact_id")
        if isinstance(artifact_id, str) and artifact_id.strip():
            add_if_new(artifact_id)
            continue

        # Convenience named-reference forms, e.g. {"dataset": "id-1"}.
        for value in item.values():
            add_if_new(value)

    return ids


class SimulatedBackend(ExecutionBackend):
    """Simulated execution for testing without a real Slurm cluster."""

    def submit(self, job: dict, client: HpcClient) -> SubmitResult:
        import tempfile
        from datetime import datetime, timezone
        from pathlib import Path

        slurm_job_id = f"sim-{job['id'][:8]}"

        # Create a temporary output directory with simulated files
        work_dir = Path(tempfile.mkdtemp(prefix=f"hpc-sim-{slurm_job_id}-"))
        output_dir = work_dir / "output"
        output_dir.mkdir()

        now = datetime.now(tz=timezone.utc).isoformat()
        processor = job.get("processor", "unknown")
        job_id = job.get("id", "unknown")

        # Simulated log files
        (output_dir / f"slurm-{slurm_job_id}.out").write_text(
            f"[{now}] Simulated Slurm stdout for job {job_id}\n"
            f"[{now}] Processor: {processor}\n"
            f"[{now}] Job completed successfully.\n"
        )
        (output_dir / "container-stdout.log").write_text(
            f"[{now}] Simulated container stdout for {processor}\n"
        )
        (output_dir / "container-stderr.log").write_text(
            f"[{now}] Simulated container stderr (no errors)\n"
        )

        # Simulated output file
        (output_dir / "result.txt").write_text(
            f"Simulated output for job {job_id}\n"
            f"Processor: {processor}\n"
            f"Completed: {now}\n"
        )

        return SubmitResult(
            slurm_job_id=slurm_job_id,
            work_dir=str(work_dir),
            output_dir=str(output_dir),
        )

    def query_status(
        self, slurm_job_id: str, current_status: str
    ) -> StatusResult | None:
        hpc_status = {"SUBMITTED": "STARTED", "STARTED": "COMPLETED"}.get(
            current_status
        )
        if hpc_status is None:
            return None
        simulated_state = {"STARTED": "RUNNING", "COMPLETED": "COMPLETED"}.get(
            hpc_status, hpc_status
        )
        return StatusResult(
            hpc_status=hpc_status,
            slurm_info=SlurmJobInfo(
                state=simulated_state,
                exit_code="0:0" if hpc_status == "COMPLETED" else "",
                reason="None",
                node_list="simulated",
                elapsed="00:00:01",
            ),
        )

    def cancel(self, slurm_job_id: str) -> None:
        pass


class ShellBackend(ExecutionBackend):
    """Local shell execution via subprocess — no Slurm required.

    Runs entrypoint scripts directly on the host with the same environment
    variable contract as SlurmBackend (HPC_JOB_ID, HPC_INPUT_DIR, etc.).
    Processes are tracked by a synthetic job ID and polled for completion.
    """

    def __init__(self, config: DaemonConfig):
        self._config = config
        # Map synthetic job ID → (Popen, output_dir)
        self._processes: dict[str, tuple[subprocess.Popen, str]] = {}

    def submit(self, job: dict, client: HpcClient) -> SubmitResult:
        job_id = job["id"]
        processor = job.get("processor", "")
        profile = job.get("profile", "")

        resolved = resolve_profile(self._config, processor, profile)
        if resolved is None:
            raise ValueError(f"No profile for {processor}:{profile}")

        if not resolved.entrypoint:
            raise ValueError(
                f"Shell backend requires an entrypoint for {processor}:{profile}. "
                "Set 'entrypoint' in the profile config."
            )

        # Create working directories
        tmp_dir = self._config.apptainer.tmp_dir or tempfile.gettempdir()
        base_dir = Path(tmp_dir) / job_id
        work_dir = base_dir / "work"
        input_dir = base_dir / "input"
        output_dir = base_dir / "output"
        for d in (work_dir, input_dir, output_dir):
            d.mkdir(parents=True, exist_ok=True)

        # Stage input artifacts
        _stage_input_artifacts(job, str(input_dir), client)

        # Build environment
        parameters = job.get("parameters")
        if parameters and isinstance(parameters, str):
            try:
                parameters = json.loads(parameters)
            except (json.JSONDecodeError, TypeError):
                parameters = {}

        env = os.environ.copy()
        env["HPC_JOB_ID"] = job_id
        env["HPC_INPUT_DIR"] = str(input_dir)
        env["HPC_OUTPUT_DIR"] = str(output_dir)
        env["HPC_WORK_DIR"] = str(work_dir)
        env["HPC_PARAMETERS"] = json.dumps(parameters) if isinstance(parameters, dict) else "{}"

        if isinstance(parameters, dict):
            extra_env = parameters.get("environment")
            if isinstance(extra_env, dict):
                env.update({k: str(v) for k, v in extra_env.items()})

        # Launch the entrypoint
        stdout_log = output_dir / "shell-stdout.log"
        stderr_log = output_dir / "shell-stderr.log"
        proc = subprocess.Popen(
            [resolved.entrypoint],
            env=env,
            cwd=str(work_dir),
            stdout=stdout_log.open("w"),
            stderr=stderr_log.open("w"),
        )

        synthetic_id = f"shell-{proc.pid}"
        self._processes[synthetic_id] = (proc, str(output_dir))
        logger.info(
            "Shell backend launched job %s as PID %d (id=%s)",
            job_id, proc.pid, synthetic_id,
        )

        return SubmitResult(
            slurm_job_id=synthetic_id,
            work_dir=str(work_dir),
            input_dir=str(input_dir),
            output_dir=str(output_dir),
        )

    def query_status(
        self, slurm_job_id: str, current_status: str
    ) -> StatusResult | None:
        entry = self._processes.get(slurm_job_id)
        if entry is None:
            return None

        proc, _ = entry
        ret = proc.poll()

        if ret is None:
            # Still running
            if current_status != "STARTED":
                return StatusResult(
                    hpc_status="STARTED",
                    slurm_info=SlurmJobInfo(
                        state="RUNNING",
                        node_list="localhost",
                    ),
                )
            return None

        # Process finished
        if ret == 0:
            hpc_status = "COMPLETED"
            state = "COMPLETED"
        else:
            hpc_status = "FAILED"
            state = "FAILED"

        return StatusResult(
            hpc_status=hpc_status,
            slurm_info=SlurmJobInfo(
                state=state,
                exit_code=f"{ret}:0",
                reason="NonZeroExit" if ret != 0 else "None",
                node_list="localhost",
            ),
        )

    def cancel(self, slurm_job_id: str) -> None:
        entry = self._processes.get(slurm_job_id)
        if entry is None:
            return
        proc, _ = entry
        if proc.poll() is None:
            logger.info("Sending SIGTERM to PID %d (%s)", proc.pid, slurm_job_id)
            os.kill(proc.pid, signal.SIGTERM)
