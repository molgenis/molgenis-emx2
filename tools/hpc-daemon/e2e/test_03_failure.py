"""Failed job: exits non-zero → FAILED status + log artifact."""

from conftest import create_job, wait_for_job_status


def test_failed_job_has_log_artifact(hpc_client):
    """Submit a job that fails, verify FAILED status and log artifact."""
    resp = create_job(hpc_client, processor="e2e-test", profile="fail")
    job_id = resp["id"]
    assert resp["status"] == "PENDING"

    job = wait_for_job_status(hpc_client, job_id, "FAILED", timeout=120)
    assert job["status"] == "FAILED"

    # Log artifact should exist with captured stderr
    log_id = job.get("log_artifact_id")
    assert log_id, "log_artifact_id should be set on failed job"
    log_artifact = hpc_client.get_artifact(log_id)
    assert log_artifact["status"] == "COMMITTED"
