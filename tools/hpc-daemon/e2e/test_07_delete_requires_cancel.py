"""DELETE on a non-terminal job MUST return 409 Conflict.

Requirement coverage: REQ-JOB-DELETE-001, REQ-JOB-DELETE-002.
"""

import httpx
import pytest

from conftest import create_job, wait_for_job_status


def test_delete_non_terminal_job_returns_409(hpc_client):
    """DELETE on a PENDING job MUST return 409 — caller must cancel first."""
    resp = create_job(hpc_client, processor="e2e-test", profile="bash")
    job_id = resp["id"]
    assert resp["status"] == "PENDING"

    # Attempt DELETE without cancelling
    with pytest.raises(httpx.HTTPStatusError) as exc_info:
        hpc_client._request("DELETE", f"/api/hpc/jobs/{job_id}")

    assert exc_info.value.response.status_code == 409
    body = exc_info.value.response.json()
    # RFC 9457 ProblemDetail shape
    assert "title" in body
    assert body["status"] == 409

    # Job should still exist and be PENDING
    job = hpc_client.get_job(job_id)
    assert job["status"] == "PENDING"

    # Clean up: cancel then delete
    hpc_client.transition_job(job_id, "CANCELLED")
    hpc_client._request("DELETE", f"/api/hpc/jobs/{job_id}")


def test_delete_terminal_job_succeeds(hpc_client):
    """DELETE on a CANCELLED (terminal) job MUST succeed."""
    resp = create_job(hpc_client, processor="e2e-test", profile="bash")
    job_id = resp["id"]
    assert resp["status"] == "PENDING"

    # Cancel first
    hpc_client.transition_job(job_id, "CANCELLED")
    job = hpc_client.get_job(job_id)
    assert job["status"] == "CANCELLED"

    # Now DELETE should succeed
    hpc_client._request("DELETE", f"/api/hpc/jobs/{job_id}")

    # Job should be gone
    with pytest.raises(httpx.HTTPStatusError) as exc_info:
        hpc_client.get_job(job_id)
    assert exc_info.value.response.status_code == 404


def test_delete_completed_job_with_artifacts(hpc_client):
    """DELETE on a COMPLETED job should also clean up artifacts."""
    resp = create_job(hpc_client, processor="e2e-test", profile="bash")
    job_id = resp["id"]

    job = wait_for_job_status(hpc_client, job_id, "COMPLETED", timeout=120)
    assert job.get("output_artifact_id"), "output_artifact_id should be set"

    # DELETE the completed job
    hpc_client._request("DELETE", f"/api/hpc/jobs/{job_id}")

    # Job should be gone
    with pytest.raises(httpx.HTTPStatusError) as exc_info:
        hpc_client.get_job(job_id)
    assert exc_info.value.response.status_code == 404
