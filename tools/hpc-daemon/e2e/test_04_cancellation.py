"""Cancel propagation across lifecycle states."""

from conftest import create_job, wait_for_job_status


def test_cancel_at_claimed_state(hpc_client, worker_client):
    """Cancel a claimed (not yet submitted) job."""
    resp = create_job(hpc_client, processor="e2e-test", profile="bash")
    job_id = resp["id"]
    assert resp["status"] == "PENDING"

    claim = worker_client.claim_job(job_id)
    assert claim["status"] == "CLAIMED"

    hpc_client.cancel_job(job_id)
    job = wait_for_job_status(hpc_client, job_id, "CANCELLED", timeout=90)
    assert job["status"] == "CANCELLED"


def test_cancel_at_submitted_state(hpc_client, vm_run):
    """Cancel after sbatch accepted the job but before execution starts."""
    # Drain node to keep the job pending after submit (SUBMITTED state).
    vm_run("sudo -n scontrol update NodeName=localhost State=DRAIN Reason=e2e-submitted-cancel")
    try:
        resp = create_job(hpc_client, processor="e2e-test", profile="slow")
        job_id = resp["id"]
        assert resp["status"] == "PENDING"

        job = wait_for_job_status(hpc_client, job_id, "SUBMITTED", timeout=150)
        assert job["status"] == "SUBMITTED"

        hpc_client.cancel_job(job_id)
        job = wait_for_job_status(hpc_client, job_id, "CANCELLED", timeout=120)
        assert job["status"] == "CANCELLED"
    finally:
        vm_run("sudo -n scontrol update NodeName=localhost State=RESUME", check=False)


def test_cancel_at_started_state(hpc_client):
    """Cancel a running Slurm job and verify terminal CANCELLED state."""
    resp = create_job(hpc_client, processor="e2e-test", profile="slow")
    job_id = resp["id"]
    assert resp["status"] == "PENDING"

    job = wait_for_job_status(hpc_client, job_id, "STARTED", timeout=150)
    assert job["status"] == "STARTED"

    hpc_client.cancel_job(job_id)
    job = wait_for_job_status(hpc_client, job_id, "CANCELLED", timeout=120)
    assert job["status"] == "CANCELLED"
