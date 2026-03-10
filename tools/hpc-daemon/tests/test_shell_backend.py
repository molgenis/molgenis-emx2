"""Tests for ShellBackend — local subprocess execution without Slurm.

Requirement coverage: REQ-DAEMON-ENV-001, REQ-DAEMON-STAGE-001.
"""

from __future__ import annotations

import stat
import textwrap
from pathlib import Path
from unittest.mock import MagicMock

import pytest

from emx2_hpc_daemon.backend import ShellBackend
from emx2_hpc_daemon.config import ProfileEntry


@pytest.fixture
def shell_config(sample_config, tmp_path):
    """Config with entrypoint profiles for shell backend testing."""
    entrypoint = tmp_path / "entrypoint.sh"
    entrypoint.write_text(
        textwrap.dedent("""\
            #!/bin/bash
            echo "hello from $HPC_JOB_ID" > "$HPC_OUTPUT_DIR/result.txt"
        """)
    )
    entrypoint.chmod(entrypoint.stat().st_mode | stat.S_IEXEC)

    failing_ep = tmp_path / "fail.sh"
    failing_ep.write_text(
        textwrap.dedent("""\
            #!/bin/bash
            echo "about to fail" >&2
            exit 1
        """)
    )
    failing_ep.chmod(failing_ep.stat().st_mode | stat.S_IEXEC)

    sample_config.profiles["shell-test:default"] = ProfileEntry(
        entrypoint=str(entrypoint),
    )
    sample_config.profiles["shell-test:fail"] = ProfileEntry(
        entrypoint=str(failing_ep),
    )
    sample_config.apptainer.tmp_dir = str(tmp_path / "work")
    return sample_config


@pytest.fixture
def mock_client():
    client = MagicMock()
    client.get_artifact.return_value = {
        "id": "art-1",
        "residence": "managed",
        "status": "COMMITTED",
        "_links": {},
    }
    client.list_artifact_files.return_value = []
    client.download_artifact_files.return_value = []
    return client


class TestShellBackendSubmit:
    def test_submit_creates_directories_and_launches(self, shell_config, mock_client):
        backend = ShellBackend(shell_config)
        job = {"id": "job-shell-001", "processor": "shell-test", "profile": "default"}

        result = backend.submit(job, mock_client)

        assert result.slurm_job_id.startswith("shell-")
        assert Path(result.work_dir).is_dir()
        assert Path(result.input_dir).is_dir()
        assert Path(result.output_dir).is_dir()

    def test_submit_requires_entrypoint(self, shell_config, mock_client):
        """Profile without entrypoint raises ValueError."""
        job = {
            "id": "job-no-ep",
            "processor": "text-embedding",
            "profile": "gpu-medium",
        }
        backend = ShellBackend(shell_config)
        with pytest.raises(ValueError, match="entrypoint"):
            backend.submit(job, mock_client)

    def test_submit_no_matching_profile_raises(self, shell_config, mock_client):
        backend = ShellBackend(shell_config)
        job = {"id": "job-bad", "processor": "nonexistent", "profile": "nope"}
        with pytest.raises(ValueError, match="No profile"):
            backend.submit(job, mock_client)

    def test_submit_sets_hpc_env_vars(self, shell_config, mock_client):
        """The subprocess should receive HPC_JOB_ID, HPC_INPUT_DIR, etc."""
        backend = ShellBackend(shell_config)
        job = {"id": "job-env-001", "processor": "shell-test", "profile": "default"}

        result = backend.submit(job, mock_client)

        # Wait for process to complete so we can check output
        import time

        for _ in range(50):
            sr = backend.query_status(result.slurm_job_id, "SUBMITTED")
            if sr and sr.hpc_status in ("COMPLETED", "FAILED"):
                break
            time.sleep(0.1)

        # Verify the entrypoint could read HPC_JOB_ID
        output_file = Path(result.output_dir) / "result.txt"
        assert output_file.exists(), "Entrypoint should write to HPC_OUTPUT_DIR"
        content = output_file.read_text()
        assert "job-env-001" in content

    def test_submit_with_parameters(self, shell_config, mock_client, tmp_path):
        """Parameters should be available as HPC_PARAMETERS env var."""
        ep = tmp_path / "param_test.sh"
        ep.write_text(
            textwrap.dedent("""\
                #!/bin/bash
                echo "$HPC_PARAMETERS" > "$HPC_OUTPUT_DIR/params.txt"
            """)
        )
        ep.chmod(ep.stat().st_mode | stat.S_IEXEC)
        shell_config.profiles["param-test:default"] = ProfileEntry(
            entrypoint=str(ep),
        )

        backend = ShellBackend(shell_config)
        job = {
            "id": "job-params-001",
            "processor": "param-test",
            "profile": "default",
            "parameters": '{"key": "value"}',
        }

        result = backend.submit(job, mock_client)

        import json
        import time

        for _ in range(50):
            sr = backend.query_status(result.slurm_job_id, "SUBMITTED")
            if sr and sr.hpc_status in ("COMPLETED", "FAILED"):
                break
            time.sleep(0.1)

        params_file = Path(result.output_dir) / "params.txt"
        assert params_file.exists()
        parsed = json.loads(params_file.read_text().strip())
        assert parsed == {"key": "value"}


class TestShellBackendQueryStatus:
    def test_running_process_returns_started(self, shell_config, mock_client, tmp_path):
        """A long-running process should report STARTED."""
        ep = tmp_path / "slow.sh"
        ep.write_text("#!/bin/bash\nsleep 30\n")
        ep.chmod(ep.stat().st_mode | stat.S_IEXEC)
        shell_config.profiles["slow:default"] = ProfileEntry(entrypoint=str(ep))

        backend = ShellBackend(shell_config)
        job = {"id": "job-slow", "processor": "slow", "profile": "default"}
        result = backend.submit(job, mock_client)

        sr = backend.query_status(result.slurm_job_id, "SUBMITTED")
        assert sr is not None
        assert sr.hpc_status == "STARTED"

        # Clean up
        backend.cancel(result.slurm_job_id)

    def test_already_started_returns_none(self, shell_config, mock_client, tmp_path):
        """If already STARTED and still running, returns None (no change)."""
        ep = tmp_path / "slow2.sh"
        ep.write_text("#!/bin/bash\nsleep 30\n")
        ep.chmod(ep.stat().st_mode | stat.S_IEXEC)
        shell_config.profiles["slow2:default"] = ProfileEntry(entrypoint=str(ep))

        backend = ShellBackend(shell_config)
        job = {"id": "job-slow2", "processor": "slow2", "profile": "default"}
        result = backend.submit(job, mock_client)

        sr = backend.query_status(result.slurm_job_id, "STARTED")
        assert sr is None  # No status change

        backend.cancel(result.slurm_job_id)

    def test_completed_process_returns_completed(self, shell_config, mock_client):
        backend = ShellBackend(shell_config)
        job = {"id": "job-done", "processor": "shell-test", "profile": "default"}
        result = backend.submit(job, mock_client)

        import time

        for _ in range(50):
            sr = backend.query_status(result.slurm_job_id, "STARTED")
            if sr and sr.hpc_status == "COMPLETED":
                assert sr.slurm_info.exit_code == "0:0"
                return
            time.sleep(0.1)
        pytest.fail("Process did not complete in time")

    def test_failed_process_returns_failed(self, shell_config, mock_client):
        backend = ShellBackend(shell_config)
        job = {"id": "job-fail", "processor": "shell-test", "profile": "fail"}
        result = backend.submit(job, mock_client)

        import time

        for _ in range(50):
            sr = backend.query_status(result.slurm_job_id, "STARTED")
            if sr and sr.hpc_status == "FAILED":
                assert sr.slurm_info.reason == "NonZeroExit"
                return
            time.sleep(0.1)
        pytest.fail("Process did not fail in time")

    def test_unknown_job_returns_none(self, shell_config):
        backend = ShellBackend(shell_config)
        assert backend.query_status("shell-999999", "STARTED") is None


class TestShellBackendCancel:
    def test_cancel_terminates_process(self, shell_config, mock_client, tmp_path):
        ep = tmp_path / "long.sh"
        ep.write_text("#!/bin/bash\nsleep 60\n")
        ep.chmod(ep.stat().st_mode | stat.S_IEXEC)
        shell_config.profiles["long:default"] = ProfileEntry(entrypoint=str(ep))

        backend = ShellBackend(shell_config)
        job = {"id": "job-cancel", "processor": "long", "profile": "default"}
        result = backend.submit(job, mock_client)

        import time

        time.sleep(0.2)  # Let it start
        backend.cancel(result.slurm_job_id)
        time.sleep(0.5)  # Let SIGTERM propagate

        sr = backend.query_status(result.slurm_job_id, "STARTED")
        assert sr is not None
        assert sr.hpc_status == "FAILED"  # Non-zero exit from SIGTERM

    def test_cancel_unknown_job_is_noop(self, shell_config):
        backend = ShellBackend(shell_config)
        backend.cancel("shell-nonexistent")  # Should not raise
