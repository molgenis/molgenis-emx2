"""Simulated execution backend: for testing without a real Slurm cluster."""

from __future__ import annotations

import tempfile
from datetime import datetime, timezone
from pathlib import Path

from .backend import (
    ExecutionBackend,
    SubmitResult,
    StatusResult,
)
from .client import HpcClient
from .slurm import SlurmJobInfo


class SimulatedBackend(ExecutionBackend):
    """Simulated execution for testing without a real Slurm cluster."""

    def submit(self, job: dict, client: HpcClient) -> SubmitResult:
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
