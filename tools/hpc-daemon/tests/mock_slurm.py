"""Mock Slurm commands for testing without a real cluster.

Provides mock implementations of sbatch, squeue, and sacct that can be
used by patching subprocess.run in tests.
"""

from __future__ import annotations

import subprocess
from dataclasses import dataclass, field


@dataclass
class MockSlurmCluster:
    """Simulates a Slurm cluster for testing.

    Tracks submitted jobs and their states. Use with unittest.mock.patch
    to intercept subprocess.run calls.
    """

    _next_job_id: int = 100000
    _jobs: dict[str, str] = field(default_factory=dict)  # job_id -> state

    def submit(self) -> str:
        """Submit a 'job' and return its ID."""
        job_id = str(self._next_job_id)
        self._next_job_id += 1
        self._jobs[job_id] = "PENDING"
        return job_id

    def set_state(self, job_id: str, state: str) -> None:
        """Set the state of a job."""
        self._jobs[job_id] = state

    def get_state(self, job_id: str) -> str:
        """Get the state of a job."""
        return self._jobs.get(job_id, "UNKNOWN")

    def mock_subprocess_run(
        self, cmd: list[str], **kwargs
    ) -> subprocess.CompletedProcess:
        """Mock subprocess.run for Slurm commands."""
        program = cmd[0]

        if program == "sbatch":
            job_id = self.submit()
            return subprocess.CompletedProcess(
                cmd, returncode=0, stdout=f"{job_id}\n", stderr=""
            )

        elif program == "squeue":
            # Extract job ID from args
            job_id = None
            for i, arg in enumerate(cmd):
                if arg == "-j" and i + 1 < len(cmd):
                    job_id = cmd[i + 1]
                    break

            if job_id and job_id in self._jobs:
                state = self._jobs[job_id]
                if state in ("PENDING", "RUNNING"):
                    # Format: State|Reason|NodeList
                    reason = "Priority" if state == "PENDING" else "None"
                    node = "" if state == "PENDING" else "mock-node-01"
                    return subprocess.CompletedProcess(
                        cmd,
                        returncode=0,
                        stdout=f"{state}|{reason}|{node}\n",
                        stderr="",
                    )
            # Job not in squeue (finished or not found)
            return subprocess.CompletedProcess(cmd, returncode=0, stdout="", stderr="")

        elif program == "sacct":
            # Extract job ID from args
            job_id = None
            for i, arg in enumerate(cmd):
                if arg == "-j" and i + 1 < len(cmd):
                    job_id = cmd[i + 1]
                    break

            if job_id and job_id in self._jobs:
                state = self._jobs[job_id]
                # Format: State|ExitCode|Reason|NodeList|Elapsed
                exit_code = "0:0" if state == "COMPLETED" else "1:0"
                reason = "None" if state == "COMPLETED" else state
                return subprocess.CompletedProcess(
                    cmd,
                    returncode=0,
                    stdout=f"{state}|{exit_code}|{reason}|mock-node-01|00:01:00\n",
                    stderr="",
                )
            return subprocess.CompletedProcess(
                cmd, returncode=0, stdout="UNKNOWN||||00:00:00\n", stderr=""
            )

        elif program == "scancel":
            job_id = cmd[1] if len(cmd) > 1 else None
            if job_id and job_id in self._jobs:
                self._jobs[job_id] = "CANCELLED"
            return subprocess.CompletedProcess(cmd, returncode=0, stdout="", stderr="")

        return subprocess.CompletedProcess(
            cmd, returncode=127, stdout="", stderr=f"mock: unknown command {program}"
        )
