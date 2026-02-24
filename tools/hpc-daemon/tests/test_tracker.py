"""Tests for the job tracker state management."""

import time

import pytest

from emx2_hpc_daemon.tracker import JobTracker, _mono_to_wall, _wall_to_mono


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


# --- Persistence tests (TinyDB) ---


def test_persist_and_reload(tmp_path):
    """Track jobs, close tracker, open a new one — jobs should be restored."""
    db_path = tmp_path / "state.json"

    t1 = JobTracker(state_db_path=db_path)
    t1.track("job-1", slurm_job_id="111", status="SUBMITTED",
             output_dir="/tmp/j1/output", work_dir="/tmp/j1",
             processor="proc", profile="prof")
    t1.track("job-2", slurm_job_id="222", status="STARTED",
             output_dir="/tmp/j2/output")
    t1.close()

    t2 = JobTracker(state_db_path=db_path)
    t2.load_from_db()

    assert t2.active_count() == 2
    j1 = t2.get("job-1")
    assert j1 is not None
    assert j1.slurm_job_id == "111"
    assert j1.status == "SUBMITTED"
    assert j1.output_dir == "/tmp/j1/output"
    assert j1.work_dir == "/tmp/j1"
    assert j1.processor == "proc"
    assert j1.profile == "prof"

    j2 = t2.get("job-2")
    assert j2 is not None
    assert j2.slurm_job_id == "222"
    assert j2.status == "STARTED"
    t2.close()


def test_persist_update(tmp_path):
    """Updates should be persisted through to the DB."""
    db_path = tmp_path / "state.json"

    t1 = JobTracker(state_db_path=db_path)
    t1.track("job-1", status="CLAIMED")
    t1.update("job-1", status="SUBMITTED", slurm_job_id="999")
    t1.close()

    t2 = JobTracker(state_db_path=db_path)
    t2.load_from_db()
    j = t2.get("job-1")
    assert j.status == "SUBMITTED"
    assert j.slurm_job_id == "999"
    t2.close()


def test_persist_remove(tmp_path):
    """Removed jobs should be deleted from the DB."""
    db_path = tmp_path / "state.json"

    t1 = JobTracker(state_db_path=db_path)
    t1.track("job-1", status="SUBMITTED")
    t1.track("job-2", status="STARTED")
    t1.remove("job-1")
    t1.close()

    t2 = JobTracker(state_db_path=db_path)
    t2.load_from_db()
    assert t2.active_count() == 1
    assert t2.get("job-1") is None
    assert t2.get("job-2") is not None
    t2.close()


def test_claimed_at_survives_reload(tmp_path):
    """claimed_at (monotonic→wall→monotonic) should be approximately preserved."""
    db_path = tmp_path / "state.json"

    mono_before = time.monotonic()
    t1 = JobTracker(state_db_path=db_path)
    t1.track("job-1", status="SUBMITTED")
    original_claimed = t1.get("job-1").claimed_at
    t1.close()

    t2 = JobTracker(state_db_path=db_path)
    t2.load_from_db()
    restored_claimed = t2.get("job-1").claimed_at

    # The restored value should be close to the original (within a few seconds
    # of wall-clock drift, which is negligible in a unit test)
    assert abs(restored_claimed - original_claimed) < 2.0
    # And both should be >= mono_before
    assert restored_claimed >= mono_before - 1.0
    t2.close()


def test_no_db_works_fine():
    """Tracker without a state DB should work identically to before."""
    tracker = JobTracker()
    tracker.track("job-1", status="SUBMITTED")
    tracker.update("job-1", status="STARTED")
    tracker.remove("job-1")
    assert tracker.active_count() == 0
    tracker.close()  # should not raise


def test_load_from_db_without_db():
    """load_from_db on a tracker with no DB is a no-op."""
    tracker = JobTracker()
    result = tracker.load_from_db()
    assert result == {}


def test_wall_mono_roundtrip():
    """Wall-clock ↔ monotonic conversion should be approximately identity."""
    mono = time.monotonic()
    wall = _mono_to_wall(mono)
    restored = _wall_to_mono(wall)
    assert abs(restored - mono) < 0.1


def test_wall_mono_zero():
    """Zero values should pass through unchanged."""
    assert _wall_to_mono(0.0) == 0.0
    assert _mono_to_wall(0.0) == 0.0
