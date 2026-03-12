"""Slurm execution backend: real sbatch/squeue/scancel integration."""

from __future__ import annotations

import json
import logging
import shlex
from pathlib import Path

from .backend import (
    ExecutionBackend,
    SubmitResult,
    StatusResult,
    SLURM_TO_HPC_STATUS,
    _stage_input_artifacts,
)
from .client import HpcClient
from .config import DaemonConfig
from .profiles import resolve_profile
from .slurm import (
    SlurmJobInfo,
    cancel_job,
    generate_batch_script,
    query_status,
    submit_job,
)

logger = logging.getLogger(__name__)

NON_TERMINAL_SLURM_STATES = frozenset(
    {
        "PENDING",
        "RUNNING",
        "CONFIGURING",
        "COMPLETING",
        "STAGE_OUT",
        "SUSPENDED",
        "REQUEUED",
        "RESIZING",
        "SIGNALING",
    }
)


def _normalize_container_args(parameters: dict) -> list[str]:
    args = parameters.get("args", [])
    if args is None:
        return []
    if not isinstance(args, list):
        raise ValueError("parameters.args must be a list")

    normalized: list[str] = []
    for arg in args:
        if isinstance(arg, (str, int, float, bool)):
            normalized.append(str(arg))
            continue
        raise ValueError("parameters.args must contain only scalar values")
    return normalized


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
            "time=%s, sif=%s, container_entrypoint=%s, host_entrypoint=%s, output_residence=%s, log_residence=%s",
            processor,
            profile,
            resolved.partition,
            resolved.cpus,
            resolved.memory,
            resolved.time,
            resolved.sif_image,
            resolved.container_entrypoint,
            resolved.host_entrypoint,
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

        # Determine execution invocation from profile + structured job parameters
        container_command = None
        environment = None
        parameters = job.get("parameters")
        if isinstance(parameters, str):
            try:
                parameters = json.loads(parameters)
            except (json.JSONDecodeError, TypeError):
                parameters = {}
        if not isinstance(parameters, dict):
            parameters = {}

        environment = parameters.get("environment")
        if environment is not None and not isinstance(environment, dict):
            raise ValueError("parameters.environment must be an object")

        if not resolved.host_entrypoint:
            if not resolved.container_entrypoint:
                raise ValueError(
                    f"Apptainer profile {processor}:{profile} requires container_entrypoint"
                )
            container_args = _normalize_container_args(parameters)
            container_command = shlex.join(
                [resolved.container_entrypoint, *container_args]
            )

        logger.debug(
            "Job %s parameters: %s (container_command=%s, env=%s)",
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
            if not resolved.host_entrypoint
            else None,
            account=self._config.slurm.default_account or None,
            container_command=container_command,
            environment=environment,
            host_entrypoint=resolved.host_entrypoint or None,
            parameters=parameters,
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
        if (
            hpc_status is None
            and info.state not in NON_TERMINAL_SLURM_STATES
            and info.state != "UNKNOWN"
        ):
            hpc_status = "FAILED"
        if hpc_status is None or hpc_status == current_status:
            return None
        return StatusResult(hpc_status=hpc_status, slurm_info=info)

    def query_slurm_info(self, slurm_job_id: str) -> SlurmJobInfo | None:
        return query_status(slurm_job_id)

    def cancel(self, slurm_job_id: str) -> None:
        cancel_job(slurm_job_id)
