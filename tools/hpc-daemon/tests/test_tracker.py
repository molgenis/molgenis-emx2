"""Tests for the job tracker state management."""

import pytest

from emx2_hpc_daemon.tracker import JobTracker


def test_track_and_retrieve():
    tracker = JobTracker()
    tracker.track("job-1", slurm_job_id="12345", status="SUBMITTED")
    job = tracker.get("job-1")
    assert job is not None
    assert job.slurm_job_id == "12345"
    assert job.status == "SUBMITTED"


def test_update_tracked_job():
    tracker = JobTracker()
    tracker.track("job-1", status="CLAIMED")
    tracker.update("job-1", status="SUBMITTED", slurm_job_id="999")
    job = tracker.get("job-1")
    assert job.status == "SUBMITTED"
    assert job.slurm_job_id == "999"


def test_remove_tracked_job():
    tracker = JobTracker()
    tracker.track("job-1")
    assert tracker.active_count() == 1
    tracker.remove("job-1")
    assert tracker.active_count() == 0
    assert tracker.get("job-1") is None


def test_slurm_ids():
    tracker = JobTracker()
    tracker.track("job-1", slurm_job_id="111")
    tracker.track("job-2", slurm_job_id="222")
    tracker.track("job-3")  # no slurm id yet
    assert set(tracker.slurm_ids()) == {"111", "222"}


def test_reconcile_from_server():
    tracker = JobTracker()
    server_jobs = [
        {"id": "job-a", "slurm_job_id": "100", "status": "STARTED"},
        {"id": "job-b", "slurm_job_id": "200", "status": "SUBMITTED"},
    ]
    tracker.reconcile_from_server(server_jobs)
    assert tracker.active_count() == 2
    assert tracker.get("job-a").slurm_job_id == "100"
    assert tracker.get("job-b").status == "SUBMITTED"


def test_update_rejects_unknown_fields():
    tracker = JobTracker()
    tracker.track("job-1", status="CLAIMED")
    with pytest.raises(ValueError, match="Unknown TrackedJob fields"):
        tracker.update("job-1", bogus_field="oops")


def test_profile_key_property():
    tracker = JobTracker()
    tracker.track("job-1", processor="text-embedding", profile="gpu-medium")
    assert tracker.get("job-1").profile_key == "text-embedding:gpu-medium"

    tracker.track("job-2", processor="text-embedding", profile=None)
    assert tracker.get("job-2").profile_key == "text-embedding:"


def test_last_progress_hash_default():
    tracker = JobTracker()
    job = tracker.track("job-1", status="STARTED")
    assert job.last_progress_hash is None


def test_last_progress_hash_update():
    tracker = JobTracker()
    tracker.track("job-1", status="STARTED")
    tracker.update("job-1", last_progress_hash="abc123")
    assert tracker.get("job-1").last_progress_hash == "abc123"
