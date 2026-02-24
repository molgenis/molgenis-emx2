"""Tests for the mock Slurm cluster and integration with slurm module."""

from __future__ import annotations

from unittest.mock import patch

from emx2_hpc_daemon.slurm import cancel_job, query_status, submit_job

from mock_slurm import MockSlurmCluster


class TestMockSlurmCluster:
    def test_submit_returns_incrementing_ids(self):
        cluster = MockSlurmCluster()
        id1 = cluster.submit()
        id2 = cluster.submit()
        assert id1 != id2
        assert id1.isdigit()
        assert id2.isdigit()

    def test_state_transitions(self):
        cluster = MockSlurmCluster()
        job_id = cluster.submit()
        assert cluster.get_state(job_id) == "PENDING"

        cluster.set_state(job_id, "RUNNING")
        assert cluster.get_state(job_id) == "RUNNING"

        cluster.set_state(job_id, "COMPLETED")
        assert cluster.get_state(job_id) == "COMPLETED"

    def test_unknown_job_returns_unknown(self):
        cluster = MockSlurmCluster()
        assert cluster.get_state("999999") == "UNKNOWN"


class TestSlurmModuleWithMock:
    def test_submit_job(self, tmp_dir):
        cluster = MockSlurmCluster()
        script = tmp_dir / "test.sbatch"
        script.write_text("#!/bin/bash\necho test\n")

        with patch("subprocess.run", side_effect=cluster.mock_subprocess_run):
            slurm_id = submit_job(script)

        assert slurm_id.isdigit()
        assert cluster.get_state(slurm_id) == "PENDING"

    def test_query_status_running(self, tmp_dir):
        cluster = MockSlurmCluster()
        script = tmp_dir / "test.sbatch"
        script.write_text("#!/bin/bash\necho test\n")

        with patch("subprocess.run", side_effect=cluster.mock_subprocess_run):
            slurm_id = submit_job(script)
            cluster.set_state(slurm_id, "RUNNING")
            info = query_status(slurm_id)

        assert info.state == "RUNNING"
        assert info.node_list == "mock-node-01"

    def test_query_status_completed(self, tmp_dir):
        cluster = MockSlurmCluster()
        script = tmp_dir / "test.sbatch"
        script.write_text("#!/bin/bash\necho test\n")

        with patch("subprocess.run", side_effect=cluster.mock_subprocess_run):
            slurm_id = submit_job(script)
            cluster.set_state(slurm_id, "COMPLETED")
            # Completed jobs are found by sacct (not squeue)
            info = query_status(slurm_id)

        assert info.state == "COMPLETED"
        assert info.exit_code == "0:0"
        assert info.elapsed == "00:01:00"

    def test_cancel_job(self, tmp_dir):
        cluster = MockSlurmCluster()
        script = tmp_dir / "test.sbatch"
        script.write_text("#!/bin/bash\necho test\n")

        with patch("subprocess.run", side_effect=cluster.mock_subprocess_run):
            slurm_id = submit_job(script)
            cancel_job(slurm_id)

        assert cluster.get_state(slurm_id) == "CANCELLED"

    def test_full_lifecycle(self, tmp_dir):
        """Simulate a complete job lifecycle: submit → pending → running → completed."""
        cluster = MockSlurmCluster()
        script = tmp_dir / "test.sbatch"
        script.write_text("#!/bin/bash\necho test\n")

        with patch("subprocess.run", side_effect=cluster.mock_subprocess_run):
            slurm_id = submit_job(script)
            assert query_status(slurm_id).state == "PENDING"

            cluster.set_state(slurm_id, "RUNNING")
            assert query_status(slurm_id).state == "RUNNING"

            cluster.set_state(slurm_id, "COMPLETED")
            assert query_status(slurm_id).state == "COMPLETED"
