"""Execution backend strategy: abstracts Slurm vs simulated execution.

The daemon delegates all Slurm-touching operations to an ExecutionBackend,
keeping a single code path regardless of execution mode.
"""

from __future__ import annotations

import json
import logging
from abc import ABC, abstractmethod
from dataclasses import dataclass
from pathlib import Path

from .client import HpcClient
from .config import DaemonConfig
from .profiles import resolve_profile
from .slurm import (
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


@dataclass
class SubmitResult:
    """Result of submitting a job to an execution backend."""

    slurm_job_id: str
    work_dir: str | None = None
    input_dir: str | None = None
    output_dir: str | None = None


class ExecutionBackend(ABC):
    """Strategy interface for job execution."""

    @abstractmethod
    def submit(self, job: dict, client: HpcClient) -> SubmitResult:
        """Submit a job for execution. Returns tracking info.

        Raises ValueError if the job cannot be submitted (e.g. no matching profile).
        """

    @abstractmethod
    def query_status(self, slurm_job_id: str, current_status: str) -> str | None:
        """Query the current status of a submitted job.

        Returns the new HPC status string, or None if unchanged.
        """

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

        # Create working directories
        base_dir = Path(self._config.apptainer.tmp_dir) / job_id
        work_dir = base_dir / "work"
        input_dir = base_dir / "input"
        output_dir = base_dir / "output"
        for d in (work_dir, input_dir, output_dir):
            d.mkdir(parents=True, exist_ok=True)

        # Stage input artifacts
        self._stage_input_artifacts(job, str(input_dir), client)

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
            extra_args=resolved.extra_args,
            bind_paths=self._config.apptainer.bind_paths,
            account=self._config.slurm.default_account or None,
            container_command=container_command,
            environment=environment,
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

    def query_status(self, slurm_job_id: str, current_status: str) -> str | None:
        slurm_state = query_status(slurm_job_id)
        hpc_status = SLURM_TO_HPC_STATUS.get(slurm_state)
        if hpc_status is None or hpc_status == current_status:
            return None
        return hpc_status

    def cancel(self, slurm_job_id: str) -> None:
        cancel_job(slurm_job_id)

    @staticmethod
    def _stage_input_artifacts(job: dict, input_dir: str, client: HpcClient) -> None:
        """Download input artifacts to the job's input directory."""
        inputs = job.get("inputs")
        if not inputs:
            return

        if isinstance(inputs, str):
            try:
                inputs = json.loads(inputs)
            except (json.JSONDecodeError, TypeError):
                logger.warning("Could not parse job inputs: %s", inputs)
                return

        if isinstance(inputs, list):
            for item in inputs:
                artifact_id = item if isinstance(item, str) else item.get("artifact_id")
                if artifact_id:
                    try:
                        downloaded = client.download_artifact_files(
                            artifact_id, input_dir
                        )
                        logger.info(
                            "Staged %d files from artifact %s",
                            len(downloaded),
                            artifact_id,
                        )
                    except Exception:
                        logger.exception(
                            "Failed to stage artifact %s", artifact_id
                        )


class SimulatedBackend(ExecutionBackend):
    """Simulated execution for testing without a real Slurm cluster."""

    def submit(self, job: dict, client: HpcClient) -> SubmitResult:
        return SubmitResult(slurm_job_id=f"sim-{job['id'][:8]}")

    def query_status(self, slurm_job_id: str, current_status: str) -> str | None:
        return {"SUBMITTED": "STARTED", "STARTED": "COMPLETED"}.get(current_status)

    def cancel(self, slurm_job_id: str) -> None:
        pass
