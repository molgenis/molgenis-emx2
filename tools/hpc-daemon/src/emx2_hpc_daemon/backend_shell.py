"""Shell execution backend: local subprocess execution without Slurm."""

from __future__ import annotations

import json
import logging
import os
import signal
import subprocess
import tempfile
from pathlib import Path

from .backend import (
    ExecutionBackend,
    SubmitResult,
    StatusResult,
    _stage_input_artifacts,
)
from .client import HpcClient
from .config import DaemonConfig
from .profiles import resolve_profile
from .slurm import SlurmJobInfo

logger = logging.getLogger(__name__)


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
        env["HPC_PARAMETERS"] = (
            json.dumps(parameters) if isinstance(parameters, dict) else "{}"
        )

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
            job_id,
            proc.pid,
            synthetic_id,
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
