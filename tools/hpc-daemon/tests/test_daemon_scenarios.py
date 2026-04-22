"""Scenario-driven daemon tests covering realistic submit/monitor flows."""

from __future__ import annotations

import hashlib
import json
import textwrap
import time
from pathlib import Path
from unittest.mock import MagicMock

import pytest

from emx2_hpc_daemon.backend import StatusResult
from emx2_hpc_daemon.client import NotFoundError
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

    def _create(
        *, backend: str = "simulate", profile: ProfileEntry | None = None
    ) -> HpcDaemon:
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


def test_heartbeat_not_found_reregisters_worker(daemon_factory):
    daemon = daemon_factory(backend="simulate")
    daemon._heartbeat_interval = 1
    daemon._last_heartbeat = 0.0
    daemon.client.heartbeat.side_effect = NotFoundError("worker missing")
    daemon.client.register_worker.return_value = {}

    daemon._maybe_heartbeat()

    daemon.client.heartbeat.assert_called_once()
    daemon.client.register_worker.assert_called_once()
    assert daemon._last_heartbeat > 0.0


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
        lambda artifact_id: [{"path": "managed/input.txt"}]
        if artifact_id == "art-managed"
        else []
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
def test_submit_stages_all_supported_input_forms(
    daemon_factory, tmp_path: Path, inputs
):
    entrypoint = _write_shell_entrypoint(tmp_path / "entrypoint.sh")
    daemon = daemon_factory(
        backend="shell",
        profile=ProfileEntry(host_entrypoint=entrypoint),
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
    assert json.loads(tracked.input_artifact_ids or "[]") == [
        "art-managed",
        "art-posix",
    ]
    input_dir = Path(tracked.input_dir)

    managed_path = input_dir / "art-managed" / "managed" / "input.txt"
    assert managed_path.read_text() == "managed-input\n"

    posix_link = input_dir / "art-posix"
    assert posix_link.is_symlink()
    assert (posix_link / "posix.txt").read_text() == "posix-input\n"


def test_submit_fails_with_input_hash_mismatch_detail(daemon_factory, tmp_path: Path):
    entrypoint = _write_shell_entrypoint(tmp_path / "entrypoint.sh")
    daemon = daemon_factory(
        backend="shell",
        profile=ProfileEntry(host_entrypoint=entrypoint),
    )

    daemon.client.get_artifact.return_value = {
        "id": "art-bad",
        "residence": "managed",
        "status": "COMMITTED",
        "sha256": "0" * 64,
        "_links": {},
    }
    daemon.client.list_artifact_files.return_value = [{"path": "input.txt"}]
    daemon.client.download_artifact_files.return_value = []

    job = {
        "id": "job-input-hash-mismatch",
        "processor": "scenario",
        "profile": "default",
        "inputs": ["art-bad"],
    }

    daemon._submit_job(job)

    assert daemon.tracker.get(job["id"]) is None
    daemon.client.transition_job.assert_called_once()
    args, kwargs = daemon.client.transition_job.call_args
    assert args[0] == "job-input-hash-mismatch"
    assert args[1] == "FAILED"
    assert "input_hash_mismatch" in kwargs["detail"]


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
            c.kwargs["path"]
            for c in daemon.client.register_artifact_file.call_args_list
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


def test_submission_timeout_marks_job_failed_and_untracks(daemon_factory):
    profile = ProfileEntry(sif_image="/tmp/fake.sif", submission_timeout_seconds=1)
    daemon = daemon_factory(backend="simulate", profile=profile)

    daemon.tracker.track(
        emx2_job_id="job-submission-timeout",
        slurm_job_id=None,
        status="CLAIMED",
        processor="scenario",
        profile="default",
        claimed_at=time.monotonic() - 10,
    )

    daemon._monitor_running_jobs()

    assert daemon.tracker.get("job-submission-timeout") is None
    daemon.client.transition_job.assert_called_once()
    args, kwargs = daemon.client.transition_job.call_args
    assert args[0] == "job-submission-timeout"
    assert args[1] == "FAILED"
    assert "submission_timeout_seconds" in kwargs["detail"]


@pytest.mark.parametrize(
    "slurm_state",
    ["PREEMPTED", "BOOT_FAIL", "DEADLINE", "REVOKED", "SPECIAL_EXIT"],
)
def test_unmapped_terminal_slurm_states_now_fail(
    daemon_factory, tmp_path: Path, slurm_state: str
):
    daemon = daemon_factory(
        backend="simulate",
        profile=ProfileEntry(sif_image="/tmp/fake.sif"),
    )
    output_dir = tmp_path / f"output-{slurm_state.lower()}"
    output_dir.mkdir(parents=True)
    (output_dir / "stderr.log").write_text("boom")

    daemon.tracker.track(
        emx2_job_id=f"job-{slurm_state.lower()}",
        slurm_job_id=f"sim-{slurm_state.lower()}",
        status="STARTED",
        output_dir=str(output_dir),
        processor="scenario",
        profile="default",
    )
    daemon._backend.query_status = MagicMock(
        return_value=StatusResult(
            hpc_status="FAILED",
            slurm_info=SlurmJobInfo(state=slurm_state, reason=slurm_state),
        )
    )

    daemon._monitor_running_jobs()

    daemon.client.complete_job.assert_called_once()
    args, kwargs = daemon.client.complete_job.call_args
    assert args[0] == f"job-{slurm_state.lower()}"
    assert args[1] == "FAILED"
    assert slurm_state in kwargs["detail"]
    assert daemon.tracker.get(f"job-{slurm_state.lower()}") is None


def test_unknown_state_grace_window_then_fail(daemon_factory, tmp_path: Path):
    daemon = daemon_factory(
        backend="simulate",
        profile=ProfileEntry(sif_image="/tmp/fake.sif"),
    )
    output_dir = tmp_path / "output-unknown"
    output_dir.mkdir(parents=True)
    (output_dir / "stderr.log").write_text("unknown failure")

    daemon.tracker.track(
        emx2_job_id="job-unknown",
        slurm_job_id="sim-unknown",
        status="STARTED",
        output_dir=str(output_dir),
        processor="scenario",
        profile="default",
    )
    daemon._backend.query_status = MagicMock(return_value=None)
    daemon._backend.query_slurm_info = MagicMock(
        return_value=SlurmJobInfo(state="UNKNOWN")
    )

    daemon._monitor_running_jobs()
    tracked = daemon.tracker.get("job-unknown")
    assert tracked is not None
    assert tracked.unknown_since > 0
    assert daemon.client.complete_job.call_count == 0

    daemon.tracker.update(
        "job-unknown",
        unknown_since=time.monotonic() - 121,
    )
    daemon._monitor_running_jobs()

    daemon.client.complete_job.assert_called_once()
    args, kwargs = daemon.client.complete_job.call_args
    assert args[0] == "job-unknown"
    assert args[1] == "FAILED"
    assert "slurm_state=UNKNOWN" in kwargs["detail"]
    assert daemon.tracker.get("job-unknown") is None


def test_timeout_failure_keeps_job_tracked_when_server_transition_fails(daemon_factory):
    profile = ProfileEntry(sif_image="/tmp/fake.sif", submission_timeout_seconds=1)
    daemon = daemon_factory(backend="simulate", profile=profile)
    daemon.client.transition_job.side_effect = RuntimeError("server down")

    daemon.tracker.track(
        emx2_job_id="job-timeout-transition-fails",
        slurm_job_id=None,
        status="CLAIMED",
        processor="scenario",
        profile="default",
        claimed_at=time.monotonic() - 10,
    )

    daemon._monitor_running_jobs()

    tracked = daemon.tracker.get("job-timeout-transition-fails")
    assert tracked is not None
    assert tracked.status == "CLAIMED"


def test_recovery_preserves_claimed_at_and_timeout_seconds(daemon_factory):
    daemon = daemon_factory(
        backend="simulate",
        profile=ProfileEntry(sif_image="/tmp/fake.sif"),
    )
    daemon.client.list_jobs.side_effect = [
        [
            {
                "id": "job-recover",
                "worker_id": daemon.config.emx2.worker_id,
                "status": "STARTED",
                "processor": "scenario",
                "profile": "default",
                "slurm_job_id": "slurm-recover",
            }
        ],
        [],
        [],
    ]
    daemon.client.get_job.return_value = {
        "id": "job-recover",
        "worker_id": daemon.config.emx2.worker_id,
        "status": "STARTED",
        "processor": "scenario",
        "profile": "default",
        "slurm_job_id": "slurm-recover",
        "claimed_at": "2026-01-01T00:00:00Z",
        "timeout_seconds": 600,
    }

    before = time.monotonic()
    daemon._recover_jobs()
    after = time.monotonic()

    tracked = daemon.tracker.get("job-recover")
    assert tracked is not None
    assert tracked.timeout_seconds == 600
    assert tracked.claimed_at < before
    assert tracked.claimed_at < after


def test_per_job_monitor_exception_does_not_abort_other_jobs(
    daemon_factory, tmp_path: Path
):
    daemon = daemon_factory(
        backend="simulate",
        profile=ProfileEntry(sif_image="/tmp/fake.sif"),
    )
    bad_dir = tmp_path / "bad-output"
    bad_dir.mkdir(parents=True)
    (bad_dir / ".hpc_progress.jsonl").mkdir()
    good_dir = tmp_path / "good-output"
    good_dir.mkdir(parents=True)
    (good_dir / "result.txt").write_text("ok")

    daemon.tracker.track(
        emx2_job_id="job-bad",
        slurm_job_id="slurm-bad",
        status="STARTED",
        output_dir=str(bad_dir),
        processor="scenario",
        profile="default",
    )
    daemon.tracker.track(
        emx2_job_id="job-good",
        slurm_job_id="slurm-good",
        status="STARTED",
        output_dir=str(good_dir),
        processor="scenario",
        profile="default",
    )

    def _query_status(slurm_job_id, current_status):
        if slurm_job_id == "slurm-bad":
            return None
        return StatusResult(
            hpc_status="COMPLETED",
            slurm_info=SlurmJobInfo(state="COMPLETED", exit_code="0:0"),
        )

    def _query_info(slurm_job_id):
        if slurm_job_id == "slurm-bad":
            return SlurmJobInfo(state="RUNNING")
        return SlurmJobInfo(state="COMPLETED")

    daemon._backend.query_status = MagicMock(side_effect=_query_status)
    daemon._backend.query_slurm_info = MagicMock(side_effect=_query_info)

    daemon._monitor_running_jobs()

    assert daemon.client.complete_job.call_count == 1
    args, _ = daemon.client.complete_job.call_args
    assert args[0] == "job-good"
    assert daemon.tracker.get("job-good") is None


def test_submit_failure_reporting_does_not_escape_and_keeps_claimed_tracking(
    daemon_factory,
):
    daemon = daemon_factory(
        backend="simulate",
        profile=ProfileEntry(sif_image="/tmp/fake.sif"),
    )
    daemon._backend.submit = MagicMock(side_effect=ValueError("sbatch rejected"))
    daemon.client.transition_job.side_effect = RuntimeError("server down")

    daemon._submit_job(
        {
            "id": "job-submit-report-fails",
            "processor": "scenario",
            "profile": "default",
            "timeout_seconds": 300,
        }
    )

    tracked = daemon.tracker.get("job-submit-report-fails")
    assert tracked is not None
    assert tracked.status == "CLAIMED"
    assert tracked.timeout_seconds == 300


@pytest.mark.parametrize("slurm_state", ["SUSPENDED", "REQUEUED"])
def test_notable_slurm_state_posts_same_state_transition(
    daemon_factory, slurm_state: str
):
    daemon = daemon_factory(
        backend="simulate",
        profile=ProfileEntry(sif_image="/tmp/fake.sif"),
    )
    daemon.config.worker.queue_report_interval_seconds = 0  # no throttle

    daemon.tracker.track(
        emx2_job_id=f"job-{slurm_state.lower()}",
        slurm_job_id=f"sim-{slurm_state.lower()}",
        status="STARTED",
        processor="scenario",
        profile="default",
    )
    daemon._backend.query_status = MagicMock(return_value=None)
    daemon._backend.query_slurm_info = MagicMock(
        return_value=SlurmJobInfo(state=slurm_state, reason="AdminAction")
    )

    daemon._monitor_running_jobs()

    # Job stays tracked (not terminal)
    tracked = daemon.tracker.get(f"job-{slurm_state.lower()}")
    assert tracked is not None
    assert tracked.status == "STARTED"

    # Same-state transition posted with Slurm state in detail
    daemon.client.transition_job.assert_called_once()
    args, kwargs = daemon.client.transition_job.call_args
    assert args[0] == f"job-{slurm_state.lower()}"
    assert args[1] == "STARTED"  # same-state
    assert slurm_state in kwargs["detail"]


@pytest.mark.parametrize("slurm_state", ["SUSPENDED", "REQUEUED"])
def test_notable_slurm_state_is_throttled(daemon_factory, slurm_state: str):
    daemon = daemon_factory(
        backend="simulate",
        profile=ProfileEntry(sif_image="/tmp/fake.sif"),
    )
    daemon.config.worker.queue_report_interval_seconds = 9999  # always throttled

    daemon.tracker.track(
        emx2_job_id=f"job-{slurm_state.lower()}-throttle",
        slurm_job_id=f"sim-{slurm_state.lower()}-throttle",
        status="STARTED",
        processor="scenario",
        profile="default",
    )
    # Simulate a recent report so the throttle window hasn't elapsed
    daemon.tracker.update(
        f"job-{slurm_state.lower()}-throttle",
        last_queue_report=time.monotonic(),
    )
    daemon._backend.query_status = MagicMock(return_value=None)
    daemon._backend.query_slurm_info = MagicMock(
        return_value=SlurmJobInfo(state=slurm_state, reason="AdminAction")
    )

    daemon._monitor_running_jobs()

    # Throttled — no transition posted
    daemon.client.transition_job.assert_not_called()
    # Job still tracked
    assert daemon.tracker.get(f"job-{slurm_state.lower()}-throttle") is not None


def test_progress_file_relay_posts_structured_fields(daemon_factory, tmp_path: Path):
    daemon = daemon_factory(
        backend="simulate",
        profile=ProfileEntry(sif_image="/tmp/fake.sif"),
    )
    output_dir = tmp_path / "output"
    output_dir.mkdir(parents=True)
    (output_dir / ".hpc_progress.jsonl").write_text(
        '{"phase":"sorting","message":"step 3 of 10","progress":0.3}\n',
        encoding="utf-8",
    )
    daemon.tracker.track(
        emx2_job_id="job-progress-relay",
        slurm_job_id="sim-progress-relay",
        status="STARTED",
        output_dir=str(output_dir),
        processor="scenario",
        profile="default",
    )
    tracked = daemon.tracker.get("job-progress-relay")
    assert tracked is not None

    daemon._check_progress_file(tracked)

    daemon.client.transition_job.assert_called_once()
    args, kwargs = daemon.client.transition_job.call_args
    assert args[0] == "job-progress-relay"
    assert args[1] == "STARTED"
    assert kwargs["phase"] == "sorting"
    assert kwargs["message"] == "step 3 of 10"
    assert kwargs["progress"] == 0.3
    assert "progress:" in kwargs["detail"]

    # Unchanged file should not emit a second update.
    daemon._check_progress_file(tracked)
    assert daemon.client.transition_job.call_count == 1


def test_fast_job_path_relays_progress_before_completion(
    daemon_factory, tmp_path: Path
):
    daemon = daemon_factory(
        backend="simulate",
        profile=ProfileEntry(sif_image="/tmp/fake.sif"),
    )
    output_dir = tmp_path / "output"
    output_dir.mkdir(parents=True)
    (output_dir / ".hpc_progress.jsonl").write_text(
        '{"phase":"finalizing","message":"wrapping up","progress":0.9}\n',
        encoding="utf-8",
    )
    daemon.tracker.track(
        emx2_job_id="job-fast-progress",
        slurm_job_id="sim-fast-progress",
        status="SUBMITTED",
        output_dir=str(output_dir),
        processor="scenario",
        profile="default",
    )
    daemon._backend.query_status = MagicMock(
        return_value=StatusResult(
            hpc_status="COMPLETED",
            slurm_info=SlurmJobInfo(state="COMPLETED", exit_code="0:0", reason="None"),
        )
    )

    daemon._monitor_running_jobs()

    started_progress_calls = [
        call
        for call in daemon.client.transition_job.call_args_list
        if call.args[0] == "job-fast-progress"
        and call.args[1] == "STARTED"
        and call.kwargs.get("phase") == "finalizing"
        and call.kwargs.get("message") == "wrapping up"
        and call.kwargs.get("progress") == 0.9
    ]
    assert started_progress_calls, "Expected structured progress relay on fast-job path"
    daemon.client.complete_job.assert_called_once()
