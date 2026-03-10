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


def test_timed_out_job_transitions_to_failed(hpc_client):
    """Slow job with short timeout should fail with a timeout transition detail."""
    resp = create_job(
        hpc_client,
        processor="e2e-test",
        profile="slow",
        timeout_seconds=15,
    )
    job_id = resp["id"]
    assert resp["status"] == "PENDING"

    job = wait_for_job_status(hpc_client, job_id, "FAILED", timeout=180)
    assert job["status"] == "FAILED"

    transitions = hpc_client._request("GET", f"/api/hpc/jobs/{job_id}/transitions")
    failure_details = [
        (item.get("detail") or "")
        for item in transitions.get("items", [])
        if item.get("to_status") == "FAILED"
    ]
    assert failure_details, "Expected at least one FAILED transition detail"
    assert any("timeout" in detail.lower() for detail in failure_details), failure_details
