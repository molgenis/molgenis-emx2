"""Scenario-driven daemon tests covering realistic submit/monitor flows."""

from __future__ import annotations

import hashlib
import textwrap
import time
from pathlib import Path
from unittest.mock import MagicMock

import pytest

from emx2_hpc_daemon.backend import StatusResult
from emx2_hpc_daemon.config import ProfileEntry
from emx2_hpc_daemon.daemon import HpcDaemon
from emx2_hpc_daemon.slurm import SlurmJobInfo
from emx2_hpc_daemon.testkit import make_artifact_factory


def _base_client_mock() -> MagicMock:
    client = MagicMock()
    client.poll_pending_jobs.return_value = []
    client.claim_job.return_value = {}
    client.transition_job.return_value = {}
    client.complete_job.return_value = {}
    client.create_artifact.side_effect = make_artifact_factory(prefix="scenario")
    client.upload_artifact_file.return_value = {}
    client.register_artifact_file.return_value = {}
    client.commit_artifact.return_value = {}
    client.get_job.return_value = {"status": "STARTED"}
    client.list_artifact_files.return_value = []
    client.download_artifact_files.return_value = []
    return client


@pytest.fixture
def daemon_factory(sample_config, tmp_path):
    daemons: list[HpcDaemon] = []

    def _create(*, backend: str = "simulate", profile: ProfileEntry | None = None) -> HpcDaemon:
        sample_config.apptainer.tmp_dir = str(tmp_path / "work")
        sample_config.worker.state_db = str(tmp_path / f"state-{len(daemons)}.db")

        if profile is not None:
            sample_config.profiles["scenario:default"] = profile

        daemon = HpcDaemon(sample_config, backend=backend)
        daemon.client = _base_client_mock()
        daemons.append(daemon)
        return daemon

    yield _create

    for daemon in daemons:
        daemon.tracker.close()


def _write_shell_entrypoint(path: Path) -> str:
    path.write_text(
        textwrap.dedent(
            """\
            #!/bin/bash
            set -euo pipefail
            mkdir -p "$HPC_OUTPUT_DIR/nested/results" "$HPC_OUTPUT_DIR/logs"
            cat "$HPC_INPUT_DIR/art-managed/managed/input.txt" > "$HPC_OUTPUT_DIR/nested/results/combined.txt"
            cat "$HPC_INPUT_DIR/art-posix/posix.txt" >> "$HPC_OUTPUT_DIR/nested/results/combined.txt"
            echo "shell run for $HPC_JOB_ID" > "$HPC_OUTPUT_DIR/logs/pipeline.log"
            """
        )
    )
    path.chmod(0o755)
    return str(path)


def _install_input_artifact_stubs(client: MagicMock, tmp_path: Path) -> None:
    managed_content = b"managed-input\n"
    posix_content = b"posix-input\n"
    managed_sha = hashlib.sha256(managed_content).hexdigest()
    posix_sha = hashlib.sha256(posix_content).hexdigest()

    posix_root = tmp_path / "posix-source"
    posix_root.mkdir(parents=True, exist_ok=True)
    (posix_root / "posix.txt").write_bytes(posix_content)

    artifacts = {
        "art-managed": {
            "id": "art-managed",
            "residence": "managed",
            "status": "COMMITTED",
            "sha256": managed_sha,
            "_links": {},
        },
        "art-posix": {
            "id": "art-posix",
            "residence": "posix",
            "status": "COMMITTED",
            "sha256": posix_sha,
            "content_url": f"file://{posix_root}",
            "_links": {},
        },
    }

    client.get_artifact.side_effect = lambda artifact_id: artifacts[artifact_id]
    client.list_artifact_files.side_effect = (
        lambda artifact_id: [{"path": "managed/input.txt"}] if artifact_id == "art-managed" else []
    )

    def _download(artifact_id: str, dest_dir: str) -> list[str]:
        target = Path(dest_dir) / "managed" / "input.txt"
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(managed_content)
        return [str(target)]

    client.download_artifact_files.side_effect = _download


@pytest.mark.parametrize(
    "inputs",
    [
        ["art-managed", "art-posix"],
        [{"artifact_id": "art-managed"}, {"artifact_id": "art-posix"}],
        [{"dataset": "art-managed"}, {"reference": "art-posix"}],
        {"dataset": "art-managed", "reference": "art-posix"},
    ],
)
def test_submit_stages_all_supported_input_forms(daemon_factory, tmp_path: Path, inputs):
    entrypoint = _write_shell_entrypoint(tmp_path / "entrypoint.sh")
    daemon = daemon_factory(
        backend="shell",
        profile=ProfileEntry(entrypoint=entrypoint),
    )
    _install_input_artifact_stubs(daemon.client, tmp_path)

    job = {
        "id": f"job-input-{hash(str(inputs)) & 0xFFFF}",
        "processor": "scenario",
        "profile": "default",
        "inputs": inputs,
        "parameters": {"task": "stage-inputs"},
    }

    daemon._submit_job(job)

    tracked = daemon.tracker.get(job["id"])
    assert tracked is not None
    assert tracked.status == "SUBMITTED"
    input_dir = Path(tracked.input_dir)

    managed_path = input_dir / "art-managed" / "managed" / "input.txt"
    assert managed_path.read_text() == "managed-input\n"

    posix_link = input_dir / "art-posix"
    assert posix_link.is_symlink()
    assert (posix_link / "posix.txt").read_text() == "posix-input\n"


@pytest.mark.parametrize("residence", ["managed", "posix"])
def test_completion_uploads_nested_paths_for_managed_and_posix(
    daemon_factory,
    tmp_path: Path,
    residence: str,
):
    profile = ProfileEntry(
        sif_image="/tmp/fake.sif",
        output_residence=residence,
        log_residence=residence,
    )
    daemon = daemon_factory(backend="simulate", profile=profile)

    output_dir = tmp_path / "output"
    (output_dir / "nested" / "results").mkdir(parents=True)
    (output_dir / "logs").mkdir(parents=True)
    (output_dir / "nested" / "results" / "output.txt").write_text("payload")
    (output_dir / "logs" / "pipeline.log").write_text("log payload")

    daemon.tracker.track(
        emx2_job_id="job-nested-residence",
        slurm_job_id="sim-nested-residence",
        status="STARTED",
        output_dir=str(output_dir),
        processor="scenario",
        profile="default",
    )

    daemon._monitor_running_jobs()

    assert daemon.client.complete_job.call_count == 1

    create_calls = daemon.client.create_artifact.call_args_list
    assert len(create_calls) == 2
    assert {c.kwargs["residence"] for c in create_calls} == {residence}

    if residence == "managed":
        uploaded_paths = sorted(
            c.kwargs["path"] for c in daemon.client.upload_artifact_file.call_args_list
        )
        assert uploaded_paths == ["logs/pipeline.log", "nested/results/output.txt"]
        assert daemon.client.register_artifact_file.call_count == 0
    else:
        registered_paths = sorted(
            c.kwargs["path"] for c in daemon.client.register_artifact_file.call_args_list
        )
        assert registered_paths == ["logs/pipeline.log", "nested/results/output.txt"]
        assert daemon.client.upload_artifact_file.call_count == 0


def test_completion_retry_is_idempotent_and_reuses_uploaded_artifacts(
    daemon_factory,
    tmp_path: Path,
):
    profile = ProfileEntry(sif_image="/tmp/fake.sif")
    daemon = daemon_factory(backend="simulate", profile=profile)

    output_dir = tmp_path / "output"
    (output_dir / "nested").mkdir(parents=True)
    (output_dir / "nested" / "result.txt").write_text("ok")
    (output_dir / "runtime.log").write_text("log")

    daemon.tracker.track(
        emx2_job_id="job-retry-idempotent",
        slurm_job_id="sim-retry-idempotent",
        status="STARTED",
        output_dir=str(output_dir),
        processor="scenario",
        profile="default",
    )

    daemon.client.complete_job.side_effect = [RuntimeError("transient"), {}]

    daemon._monitor_running_jobs()

    tracked_after_failure = daemon.tracker.get("job-retry-idempotent")
    assert tracked_after_failure is not None
    assert tracked_after_failure.log_artifact_id is not None
    assert tracked_after_failure.output_artifact_id is not None
    assert daemon.client.create_artifact.call_count == 2
    assert daemon.client.complete_job.call_count == 1

    daemon._monitor_running_jobs()

    assert daemon.client.complete_job.call_count == 2
    assert daemon.client.create_artifact.call_count == 2
    assert daemon.tracker.get("job-retry-idempotent") is None

    first = daemon.client.complete_job.call_args_list[0]
    second = daemon.client.complete_job.call_args_list[1]
    assert first.kwargs["log_artifact_id"] == second.kwargs["log_artifact_id"]
    assert first.kwargs["output_artifact_id"] == second.kwargs["output_artifact_id"]


def test_server_cancel_wins_over_terminal_status_race(daemon_factory, tmp_path: Path):
    profile = ProfileEntry(sif_image="/tmp/fake.sif")
    daemon = daemon_factory(backend="simulate", profile=profile)

    output_dir = tmp_path / "output"
    output_dir.mkdir(parents=True)
    (output_dir / "result.txt").write_text("done")

    daemon.tracker.track(
        emx2_job_id="job-cancel-race",
        slurm_job_id="sim-cancel-race",
        status="STARTED",
        output_dir=str(output_dir),
        processor="scenario",
        profile="default",
    )

    daemon.client.get_job.return_value = {"status": "CANCELLED"}
    daemon._backend.cancel = MagicMock()
    daemon._backend.query_status = MagicMock(
        return_value=StatusResult(
            hpc_status="COMPLETED",
            slurm_info=SlurmJobInfo(state="COMPLETED", exit_code="0:0", reason="None"),
        )
    )

    daemon._monitor_running_jobs()

    daemon._backend.cancel.assert_called_once_with("sim-cancel-race")
    assert daemon.client.complete_job.call_count == 0
    assert daemon._backend.query_status.call_count == 0
    assert daemon.tracker.get("job-cancel-race") is None


def test_claim_timeout_marks_job_failed_and_untracks(daemon_factory):
    profile = ProfileEntry(sif_image="/tmp/fake.sif", claim_timeout_seconds=1)
    daemon = daemon_factory(backend="simulate", profile=profile)

    daemon.tracker.track(
        emx2_job_id="job-claim-timeout",
        slurm_job_id="sim-claim-timeout",
        status="SUBMITTED",
        processor="scenario",
        profile="default",
        claimed_at=time.monotonic() - 10,
    )

    daemon._backend.query_slurm_info = MagicMock(
        return_value=SlurmJobInfo(state="PENDING", reason="Resources")
    )

    daemon._monitor_running_jobs()

    assert daemon.tracker.get("job-claim-timeout") is None
    daemon.client.transition_job.assert_called_once()
    args, kwargs = daemon.client.transition_job.call_args
    assert args[0] == "job-claim-timeout"
    assert args[1] == "FAILED"
    assert "claim_timeout_seconds" in kwargs["detail"]


def test_execution_timeout_cancels_slurm_and_marks_failed(daemon_factory):
    profile = ProfileEntry(
        sif_image="/tmp/fake.sif",
        claim_timeout_seconds=999,
        execution_timeout_seconds=1,
    )
    daemon = daemon_factory(backend="simulate", profile=profile)

    daemon.tracker.track(
        emx2_job_id="job-exec-timeout",
        slurm_job_id="sim-exec-timeout",
        status="STARTED",
        processor="scenario",
        profile="default",
        claimed_at=time.monotonic() - 10,
    )

    daemon._backend.cancel = MagicMock()
    daemon._backend.query_slurm_info = MagicMock(
        return_value=SlurmJobInfo(state="RUNNING", reason="None")
    )

    daemon._monitor_running_jobs()

    daemon._backend.cancel.assert_called_once_with("sim-exec-timeout")
    assert daemon.tracker.get("job-exec-timeout") is None
    daemon.client.transition_job.assert_called_once()
    args, kwargs = daemon.client.transition_job.call_args
    assert args[0] == "job-exec-timeout"
    assert args[1] == "FAILED"
    assert "execution_timeout_seconds" in kwargs["detail"]
