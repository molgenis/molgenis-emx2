"""Posix artifact lifecycle: submit -> complete -> verify file metadata."""

from conftest import create_job, wait_for_job_status


def test_posix_job_completes_with_artifacts(hpc_client):
    """Submit a posix-profile job, verify artifact has file metadata."""
    resp = create_job(hpc_client, processor="e2e-test", profile="posix")
    job_id = resp["id"]
    assert resp["status"] == "PENDING"

    job = wait_for_job_status(hpc_client, job_id, "COMPLETED", timeout=120)

    # Output artifact should be posix + committed
    output_id = job.get("output_artifact_id")
    assert output_id
    artifact = hpc_client.get_artifact(output_id)
    assert artifact["status"] == "COMMITTED"
    assert artifact.get("residence") == "posix"
    assert artifact.get("content_url", "").startswith("file://")

    # File metadata should be registered (not empty)
    files = hpc_client.list_artifact_files(output_id)
    file_paths = sorted(f.get("path", f.get("id")) for f in files)
    assert "result.txt" in file_paths
    assert "sample.bin" in file_paths

    # Verify sample.bin is ~1MB
    sample = next(f for f in files if f.get("path") == "sample.bin")
    assert int(sample.get("size_bytes", 0)) >= 1024 * 1024
    assert sample.get("sha256")  # hash should be set

    # Log artifact should also be posix + committed
    log_id = job.get("log_artifact_id")
    assert log_id
    log_artifact = hpc_client.get_artifact(log_id)
    assert log_artifact["status"] == "COMMITTED"
    assert log_artifact.get("residence") == "posix"

    # Log artifact should have file metadata too
    log_files = hpc_client.list_artifact_files(log_id)
    assert len(log_files) > 0  # at least slurm-*.out
