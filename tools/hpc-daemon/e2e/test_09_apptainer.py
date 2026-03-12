"""Apptainer-backed lifecycle: submit -> complete -> verify container outputs."""

import json
from pathlib import Path

from conftest import create_job, wait_for_job_status
from emx2_hpc_daemon.testkit import (
    create_and_commit_managed_artifact,
    deterministic_temp_dir,
)


def test_apptainer_job_completes_with_bound_input_and_outputs(hpc_client):
    input_bytes = b"message-from-input-artifact\n"
    committed_input = create_and_commit_managed_artifact(
        hpc_client,
        artifact_type="blob",
        name="e2e-apptainer-input",
        files={"message.txt": input_bytes},
    )
    input_id = committed_input["id"]

    command = (
        "/bin/sh -c "
        f"'IFS= read -r message < /input/{input_id}/message.txt; "
        'printf "%s\\n" "$message" > /output/copied-message.txt; '
        'printf "Hello from apptainer job %s\\n" "$EMX2_JOB_ID" > /output/result.txt; '
        'printf "{\\"job_id\\":\\"%s\\",\\"mode\\":\\"apptainer\\",\\"input\\":\\"%s\\"}\\n" '
        '"$EMX2_JOB_ID" "$message" > /output/result.json; '
        'printf "container-stdout\\n"\''
    )

    resp = create_job(
        hpc_client,
        processor="e2e-test",
        profile="apptainer",
        inputs=[{"artifact_id": input_id}],
        parameters={"command": command},
    )
    job_id = resp["id"]
    assert resp["status"] == "PENDING"

    job = wait_for_job_status(hpc_client, job_id, "COMPLETED", timeout=180)
    assert job.get("slurm_job_id"), "slurm_job_id should be set on completed job"

    output_id = job.get("output_artifact_id")
    assert output_id, "output_artifact_id should be set"
    artifact = hpc_client.get_artifact(output_id)
    assert artifact["status"] == "COMMITTED"

    files = hpc_client.list_artifact_files(output_id)
    file_paths = sorted(f.get("path", f.get("id")) for f in files)
    assert "copied-message.txt" in file_paths
    assert "result.txt" in file_paths
    assert "result.json" in file_paths

    # container-stdout.log and container-stderr.log are classified as log
    # files (*.log pattern) and go into the log artifact, not the output artifact.
    log_id = job.get("log_artifact_id")
    assert log_id, "log_artifact_id should be set"
    log_artifact = hpc_client.get_artifact(log_id)
    assert log_artifact["status"] == "COMMITTED"
    log_files = hpc_client.list_artifact_files(log_id)
    log_paths = sorted(f.get("path", f.get("id")) for f in log_files)
    assert "container-stdout.log" in log_paths
    assert "container-stderr.log" in log_paths

    with deterministic_temp_dir(f"e2e-apptainer-{job_id}") as tmpdir:
        tmpdir = Path(tmpdir)
        for name in ("copied-message.txt", "result.txt", "result.json"):
            hpc_client.download_artifact_file(output_id, name, str(tmpdir / name))
        for name in ("container-stdout.log", "container-stderr.log"):
            hpc_client.download_artifact_file(log_id, name, str(tmpdir / name))

        copied = (tmpdir / "copied-message.txt").read_text().strip()
        assert copied == "message-from-input-artifact"

        result_text = (tmpdir / "result.txt").read_text()
        assert job_id in result_text
        assert "Hello from apptainer job" in result_text

        result_json = json.loads((tmpdir / "result.json").read_text())
        assert result_json["job_id"] == job_id
        assert result_json["mode"] == "apptainer"
        assert result_json["input"] == "message-from-input-artifact"

        stdout_text = (tmpdir / "container-stdout.log").read_text()
        assert "container-stdout" in stdout_text

        stderr_text = (tmpdir / "container-stderr.log").read_text()
        # Apptainer may emit warnings about missing passwd/group/tmp in
        # minimal containers — filter those before asserting no real errors.
        meaningful_stderr = "\n".join(
            line for line in stderr_text.splitlines() if not line.startswith("WARNING:")
        )
        assert meaningful_stderr == ""
