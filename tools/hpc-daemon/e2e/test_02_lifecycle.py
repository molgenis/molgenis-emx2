"""Happy path: submit → complete → artifacts."""

import tempfile

from conftest import create_job, wait_for_job_status


def test_job_completes_with_artifacts(hpc_client):
    """Submit a job, wait for COMPLETED, verify artifacts."""
    # Submit
    resp = create_job(hpc_client, processor="e2e-test", profile="bash")
    job_id = resp["id"]
    assert resp["status"] == "PENDING"

    # Wait for completion
    job = wait_for_job_status(hpc_client, job_id, "COMPLETED", timeout=120)

    # Slurm job ID should be set
    assert job.get("slurm_job_id"), "slurm_job_id should be set on completed job"

    # Output artifact should exist and be committed
    output_id = job.get("output_artifact_id")
    assert output_id, "output_artifact_id should be set"
    artifact = hpc_client.get_artifact(output_id)
    assert artifact["status"] == "COMMITTED"

    # Log artifact should exist and be committed
    log_id = job.get("log_artifact_id")
    assert log_id, "log_artifact_id should be set"
    log_artifact = hpc_client.get_artifact(log_id)
    assert log_artifact["status"] == "COMMITTED"

    # Download and verify result.txt content
    files = hpc_client.list_artifact_files(output_id)
    file_paths = [f.get("path", f.get("id")) for f in files]
    assert "result.txt" in file_paths, f"Expected result.txt in {file_paths}"

    with tempfile.TemporaryDirectory() as tmpdir:
        hpc_client.download_artifact_file(
            output_id, "result.txt", f"{tmpdir}/result.txt"
        )
        with open(f"{tmpdir}/result.txt") as f:
            content = f.read()
        assert job_id in content, f"Expected job_id in result.txt, got: {content}"
        assert "Hello from e2e job" in content
