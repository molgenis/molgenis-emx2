"""Slurm interaction via subprocess: sbatch, squeue, scancel.

All commands are run with a timeout to prevent hangs. The module provides
a clean interface for the daemon to submit jobs, check status, and cancel.
"""

from __future__ import annotations

import json as _json
import logging
import re
import subprocess
import textwrap
from pathlib import Path

logger = logging.getLogger(__name__)

COMMAND_TIMEOUT = 30  # seconds


class SlurmError(Exception):
    """Raised when a Slurm command fails."""


def submit_job(script_path: str | Path) -> str:
    """
    Submit a job script via sbatch.

    Returns the Slurm job ID.
    Raises SlurmError if submission fails.
    """
    result = subprocess.run(
        ["sbatch", "--parsable", str(script_path)],
        capture_output=True,
        text=True,
        timeout=COMMAND_TIMEOUT,
    )
    if result.returncode != 0:
        raise SlurmError(f"sbatch failed: {result.stderr.strip()}")

    # --parsable output is: job_id or job_id;cluster_name
    slurm_id = result.stdout.strip().split(";")[0]
    if not slurm_id.isdigit():
        raise SlurmError(f"Unexpected sbatch output: {result.stdout.strip()}")

    logger.info("Submitted Slurm job %s from %s", slurm_id, script_path)
    return slurm_id


def query_status(slurm_job_id: str) -> str:
    """
    Query the status of a Slurm job via squeue/sacct.

    Returns one of: PENDING, RUNNING, COMPLETED, FAILED, CANCELLED, TIMEOUT, UNKNOWN.
    First tries squeue (for active jobs), falls back to sacct (for finished jobs).
    """
    # Try squeue first (for running/pending jobs)
    try:
        result = subprocess.run(
            ["squeue", "-j", slurm_job_id, "-h", "-o", "%T"],
            capture_output=True,
            text=True,
            timeout=COMMAND_TIMEOUT,
        )
        if result.returncode == 0 and result.stdout.strip():
            return result.stdout.strip()
    except (subprocess.TimeoutExpired, FileNotFoundError):
        pass

    # Fall back to sacct (for completed jobs)
    try:
        result = subprocess.run(
            [
                "sacct",
                "-j",
                slurm_job_id,
                "-n",
                "-X",
                "-o",
                "State",
                "--parsable2",
            ],
            capture_output=True,
            text=True,
            timeout=COMMAND_TIMEOUT,
        )
        if result.returncode == 0 and result.stdout.strip():
            # sacct may return states like "COMPLETED", "FAILED", "CANCELLED by ..."
            state = result.stdout.strip().split("\n")[0]
            # Normalize "CANCELLED by 1000" → "CANCELLED"
            state = re.split(r"\s+by\s+", state)[0]
            return state
    except (subprocess.TimeoutExpired, FileNotFoundError):
        pass

    return "UNKNOWN"


def cancel_job(slurm_job_id: str) -> None:
    """Cancel a Slurm job via scancel."""
    try:
        result = subprocess.run(
            ["scancel", slurm_job_id],
            capture_output=True,
            text=True,
            timeout=COMMAND_TIMEOUT,
        )
        if result.returncode != 0:
            logger.warning("scancel %s failed: %s", slurm_job_id, result.stderr.strip())
        else:
            logger.info("Cancelled Slurm job %s", slurm_job_id)
    except subprocess.TimeoutExpired:
        logger.error("scancel %s timed out", slurm_job_id)


def _format_sbatch_directives(
    job_id: str,
    partition: str,
    cpus: int,
    memory: str,
    time_limit: str,
    output_dir: str,
    account: str | None = None,
    extra_args: list[str] | None = None,
) -> str:
    """Format the #SBATCH directive block."""
    lines = textwrap.dedent(f"""\
        #SBATCH --job-name=emx2-{job_id[:8]}
        #SBATCH --partition={partition}
        #SBATCH --cpus-per-task={cpus}
        #SBATCH --mem={memory}
        #SBATCH --time={time_limit}
        #SBATCH --output={output_dir}/slurm-%j.out
        #SBATCH --error={output_dir}/slurm-%j.err""")
    if account:
        lines += f"\n#SBATCH --account={account}"
    for arg in extra_args or []:
        lines += f"\n#SBATCH {arg}"
    return lines


def _format_env_exports(environment: dict[str, str] | None) -> str:
    """Format extra environment variable exports."""
    if not environment:
        return ""
    return "\n".join(f'export {k}="{v}"' for k, v in environment.items())


def _format_entrypoint_body(
    job_id: str,
    work_dir: str,
    input_dir: str,
    output_dir: str,
    entrypoint: str,
    environment: dict[str, str] | None = None,
    parameters: dict | None = None,
) -> str:
    """Format the entrypoint/wrapper execution body."""
    params_json = _json.dumps(parameters) if parameters else "{}"
    env_exports = _format_env_exports(environment)
    env_block = f"\n{env_exports}" if env_exports else ""

    return textwrap.dedent(f"""\
        # Entrypoint wrapper execution
        export HPC_JOB_ID="{job_id}"
        export HPC_INPUT_DIR="{input_dir}"
        export HPC_OUTPUT_DIR="{output_dir}"
        export HPC_WORK_DIR="{work_dir}"
        export HPC_PARAMETERS='{params_json}'{env_block}

        exec {entrypoint}""")


def _format_apptainer_body(
    job_id: str,
    sif_image: str,
    work_dir: str,
    input_dir: str,
    output_dir: str,
    bind_paths: list[str] | None = None,
    container_command: str | None = None,
    environment: dict[str, str] | None = None,
) -> str:
    """Format the Apptainer container execution body."""
    bind_parts = [
        f"{input_dir}:/input:ro",
        f"{output_dir}:/output",
        f"{work_dir}:/work",
    ]
    bind_parts.extend(bind_paths or [])
    bind_str = ",".join(bind_parts)

    env_flags = f'--env "EMX2_JOB_ID={job_id}"'
    for k, v in (environment or {}).items():
        env_flags += f' --env "{k}={v}"'

    if container_command is None:
        container_command = (
            '/bin/bash -c \'echo "Container started"; ls /input; echo "Done"\''
        )

    return textwrap.dedent(f"""\
        # Run container via Apptainer
        STDOUT_LOG={output_dir}/container-stdout.log
        STDERR_LOG={output_dir}/container-stderr.log

        EXIT_CODE=0
        apptainer exec \\
          --cleanenv \\
          --bind {bind_str} \\
          --pwd /work \\
          {env_flags} \\
          {sif_image} \\
          {container_command} \\
          > "$STDOUT_LOG" 2> "$STDERR_LOG" \\
          || EXIT_CODE=$?

        echo "Exit code: $EXIT_CODE"
        echo "End time: $(date -Iseconds)"

        # Show last lines of stderr if failed
        if [ "$EXIT_CODE" -ne 0 ] && [ -s "$STDERR_LOG" ]; then
            echo "--- Last 20 lines of stderr ---"
            tail -20 "$STDERR_LOG"
            echo "--- End stderr ---"
        fi

        exit $EXIT_CODE""")


def generate_batch_script(
    job_id: str,
    sif_image: str,
    partition: str,
    cpus: int,
    memory: str,
    time_limit: str,
    work_dir: str,
    input_dir: str,
    output_dir: str,
    extra_args: list[str] | None = None,
    bind_paths: list[str] | None = None,
    account: str | None = None,
    container_command: str | None = None,
    environment: dict[str, str] | None = None,
    entrypoint: str | None = None,
    parameters: dict | None = None,
) -> str:
    """
    Generate a Slurm batch script.

    Supports two execution modes:

    - **Apptainer mode** (``sif_image`` set): runs the workload inside an
      Apptainer container with bind-mounted directories.
    - **Wrapper/entrypoint mode** (``entrypoint`` set): exports well-defined
      env vars and ``exec``s the wrapper script directly on the host.
    """
    sbatch = _format_sbatch_directives(
        job_id, partition, cpus, memory, time_limit, output_dir,
        account=account, extra_args=extra_args,
    )

    preamble = textwrap.dedent(f"""\
        set -euo pipefail

        export EMX2_JOB_ID="{job_id}"
        echo "EMX2 HPC Job: {job_id}"
        echo "Slurm Job ID: $SLURM_JOB_ID"
        echo "Start time: $(date -Iseconds)"
    """)

    if entrypoint:
        body = _format_entrypoint_body(
            job_id, work_dir, input_dir, output_dir, entrypoint,
            environment=environment, parameters=parameters,
        )
    else:
        body = _format_apptainer_body(
            job_id, sif_image, work_dir, input_dir, output_dir,
            bind_paths=bind_paths, container_command=container_command,
            environment=environment,
        )

    return f"#!/bin/bash\n{sbatch}\n\n{preamble}\n{body}\n"
