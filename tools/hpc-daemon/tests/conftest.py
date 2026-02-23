"""Shared test fixtures for the HPC daemon test suite."""

from __future__ import annotations

import tempfile
from pathlib import Path

import pytest

from emx2_hpc_daemon.config import (
    ApptainerConfig,
    DaemonConfig,
    EmxConfig,
    ProfileEntry,
    SlurmConfig,
    WorkerConfig,
)


@pytest.fixture
def sample_config() -> DaemonConfig:
    """A sample daemon config for testing."""
    return DaemonConfig(
        emx2=EmxConfig(
            base_url="http://localhost:8080",
            worker_id="test-worker-01",
            shared_secret="test-secret-key",
        ),
        worker=WorkerConfig(poll_interval_seconds=5, max_concurrent_jobs=2),
        slurm=SlurmConfig(default_partition="normal", default_account="test"),
        profiles={
            "text-embedding:gpu-medium": ProfileEntry(
                sif_image="/nfs/images/text-embedding_v3.sif",
                partition="gpu",
                cpus=8,
                memory="64G",
                time="04:00:00",
            ),
            "data-pipeline:default": ProfileEntry(
                sif_image="/nfs/images/data-pipeline.sif",
                partition="normal",
                cpus=4,
                memory="16G",
                time="01:00:00",
            ),
        },
        apptainer=ApptainerConfig(
            bind_paths=["/nfs/data", "/scratch"],
            tmp_dir="/tmp/emx2-hpc-test",
        ),
    )


@pytest.fixture
def tmp_dir():
    """Provide a temporary directory for test artifacts."""
    with tempfile.TemporaryDirectory() as d:
        yield Path(d)
