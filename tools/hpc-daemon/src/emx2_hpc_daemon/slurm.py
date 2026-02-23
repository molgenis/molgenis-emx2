"""Slurm interaction via subprocess: sbatch, squeue, scancel.

All commands are run with a timeout to prevent hangs. The module provides
a clean interface for the daemon to submit jobs, check status, and cancel.
"""

from __future__ import annotations

import logging
import re
import subprocess
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
) -> str:
    """
    Generate a Slurm batch script for running an Apptainer container.

    Args:
        job_id: EMX2 job identifier.
        sif_image: Path to the .sif container image.
        partition: Slurm partition.
        cpus: Number of CPUs.
        memory: Memory allocation (e.g., "16G").
        time_limit: Wall time (e.g., "01:00:00").
        work_dir: Host working directory (bind-mounted as /work).
        input_dir: Host input directory (bind-mounted as /input:ro).
        output_dir: Host output directory (bind-mounted as /output).
        extra_args: Additional #SBATCH directives.
        bind_paths: Additional bind mount paths.
        account: Slurm account.
        container_command: Command to run inside the container. If None, uses
            a default command that lists inputs and reports success.
        environment: Extra environment variables to pass into the container.

    Returns the script content as a string.
    """
    lines = [
        "#!/bin/bash",
        f"#SBATCH --job-name=emx2-{job_id[:8]}",
        f"#SBATCH --partition={partition}",
        f"#SBATCH --cpus-per-task={cpus}",
        f"#SBATCH --mem={memory}",
        f"#SBATCH --time={time_limit}",
        f"#SBATCH --output={output_dir}/slurm-%j.out",
        f"#SBATCH --error={output_dir}/slurm-%j.err",
    ]

    if account:
        lines.append(f"#SBATCH --account={account}")

    if extra_args:
        for arg in extra_args:
            lines.append(f"#SBATCH {arg}")

    lines.extend(
        [
            "",
            "set -euo pipefail",
            "",
            f'export EMX2_JOB_ID="{job_id}"',
            f'echo "EMX2 HPC Job: {job_id}"',
            f'echo "Slurm Job ID: $SLURM_JOB_ID"',
            f'echo "Start time: $(date -Iseconds)"',
            "",
        ]
    )

    # Build bind mount arguments
    bind_parts = [
        f"{input_dir}:/input:ro",
        f"{output_dir}:/output",
        f"{work_dir}:/work",
    ]
    if bind_paths:
        bind_parts.extend(bind_paths)
    bind_str = ",".join(bind_parts)

    # Build environment flags
    env_flags = f'--env "EMX2_JOB_ID={job_id}"'
    if environment:
        for key, value in environment.items():
            env_flags += f' --env "{key}={value}"'

    # Container command
    if container_command is None:
        container_command = (
            '/bin/bash -c \'echo "Container started"; ls /input; echo "Done"\''
        )

    # Create stdout/stderr log paths
    lines.extend(
        [
            "# Run container via Apptainer",
            f"STDOUT_LOG={output_dir}/container-stdout.log",
            f"STDERR_LOG={output_dir}/container-stderr.log",
            "",
            "EXIT_CODE=0",
            f"apptainer exec \\",
            f"  --cleanenv \\",
            f"  --bind {bind_str} \\",
            f"  --pwd /work \\",
            f"  {env_flags} \\",
            f"  {sif_image} \\",
            f"  {container_command} \\",
            f'  > "$STDOUT_LOG" 2> "$STDERR_LOG" \\',
            "  || EXIT_CODE=$?",
            "",
            f'echo "Exit code: $EXIT_CODE"',
            f'echo "End time: $(date -Iseconds)"',
            "",
            '# Show last lines of stderr if failed',
            'if [ "$EXIT_CODE" -ne 0 ] && [ -s "$STDERR_LOG" ]; then',
            '    echo "--- Last 20 lines of stderr ---"',
            '    tail -20 "$STDERR_LOG"',
            '    echo "--- End stderr ---"',
            "fi",
            "",
            "exit $EXIT_CODE",
        ]
    )

    return "\n".join(lines) + "\n"
