"""Cancel propagation: cancel via API → scancel in Slurm."""

from conftest import create_job, wait_for_job_status


def test_cancel_propagates_to_slurm(hpc_client):
    """Submit a slow job, wait for STARTED, cancel, verify CANCELLED."""
    resp = create_job(hpc_client, processor="e2e-test", profile="slow")
    job_id = resp["id"]
    assert resp["status"] == "PENDING"

    # Wait for the job to actually start running in Slurm
    job = wait_for_job_status(hpc_client, job_id, "STARTED", timeout=120)
    assert job["status"] == "STARTED"

    # Cancel via API
    hpc_client.cancel_job(job_id)

    # Wait for CANCELLED status
    job = wait_for_job_status(hpc_client, job_id, "CANCELLED", timeout=60)
    assert job["status"] == "CANCELLED"
