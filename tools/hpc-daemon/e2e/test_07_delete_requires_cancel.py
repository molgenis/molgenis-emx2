"""DELETE on a non-terminal job MUST return 409 Conflict.

Requirement coverage: REQ-JOB-DELETE-001, REQ-JOB-DELETE-002.
"""

import pytest

from conftest import create_job, wait_for_job_status
from emx2_hpc_daemon.client import NotFoundError, TransitionError


def test_delete_non_terminal_job_returns_409(hpc_client):
    """DELETE on a PENDING job MUST return 409 — caller must cancel first."""
    resp = create_job(hpc_client, processor="e2e-test", profile="bash")
    job_id = resp["id"]
    assert resp["status"] == "PENDING"

    # Attempt DELETE without cancelling
    with pytest.raises(TransitionError) as exc_info:
        hpc_client._request("DELETE", f"/api/hpc/jobs/{job_id}")

    message = str(exc_info.value)
    assert "non-terminal status" in message
    assert "Cancel it first" in message

    # Job should still exist and remain non-terminal.
    job = hpc_client.get_job(job_id)
    assert job["status"] in {"PENDING", "CLAIMED", "SUBMITTED", "STARTED"}

    # Clean up: cancel then delete
    hpc_client.cancel_job(job_id)
    wait_for_job_status(hpc_client, job_id, "CANCELLED", timeout=90)
    hpc_client._request("DELETE", f"/api/hpc/jobs/{job_id}")


def test_delete_terminal_job_succeeds(hpc_client):
    """DELETE on a CANCELLED (terminal) job MUST succeed."""
    resp = create_job(hpc_client, processor="e2e-test", profile="bash")
    job_id = resp["id"]
    assert resp["status"] == "PENDING"

    # Cancel first
    hpc_client.cancel_job(job_id)
    wait_for_job_status(hpc_client, job_id, "CANCELLED", timeout=90)

    # Now DELETE should succeed
    hpc_client._request("DELETE", f"/api/hpc/jobs/{job_id}")

    # Job should be gone
    with pytest.raises(NotFoundError):
        hpc_client.get_job(job_id)


def test_delete_completed_job_with_artifacts(hpc_client):
    """DELETE on a COMPLETED job should also clean up artifacts."""
    resp = create_job(hpc_client, processor="e2e-test", profile="bash")
    job_id = resp["id"]

    job = wait_for_job_status(hpc_client, job_id, "COMPLETED", timeout=120)
    assert job.get("output_artifact_id"), "output_artifact_id should be set"

    # DELETE the completed job
    hpc_client._request("DELETE", f"/api/hpc/jobs/{job_id}")

    # Job should be gone
    with pytest.raises(NotFoundError):
        hpc_client.get_job(job_id)
