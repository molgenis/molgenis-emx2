"""Tests for Slurm batch script generation (does not require actual Slurm)."""

import json

from emx2_hpc_daemon.slurm import generate_batch_script


# --- Apptainer mode (existing) ---


def test_generate_batch_script_basic():
    script = generate_batch_script(
        job_id="abc12345-6789",
        sif_image="/nfs/images/test.sif",
        partition="gpu",
        cpus=8,
        memory="64G",
        time_limit="04:00:00",
        work_dir="/scratch/work",
        input_dir="/scratch/input",
        output_dir="/scratch/output",
    )
    assert script.startswith("#!/bin/bash")
    assert "#SBATCH --partition=gpu" in script
    assert "#SBATCH --cpus-per-task=8" in script
    assert "#SBATCH --mem=64G" in script
    assert "#SBATCH --time=04:00:00" in script
    assert "/nfs/images/test.sif" in script
    assert "/scratch/input:/input:ro" in script
    assert "/scratch/output:/output" in script


def test_generate_batch_script_with_account_and_binds():
    script = generate_batch_script(
        job_id="test-job",
        sif_image="/test.sif",
        partition="normal",
        cpus=4,
        memory="16G",
        time_limit="01:00:00",
        work_dir="/work",
        input_dir="/input",
        output_dir="/output",
        account="emx2",
        bind_paths=["/nfs/data", "/scratch"],
    )
    assert "#SBATCH --account=emx2" in script
    # Bind paths are now integrated into the main --bind argument
    assert "/nfs/data" in script
    assert "/scratch" in script


def test_generate_batch_script_with_extra_args():
    script = generate_batch_script(
        job_id="test-job",
        sif_image="/test.sif",
        partition="normal",
        cpus=2,
        memory="8G",
        time_limit="00:30:00",
        work_dir="/work",
        input_dir="/input",
        output_dir="/output",
        extra_args=["--gres=gpu:1", "--constraint=v100"],
    )
    assert "#SBATCH --gres=gpu:1" in script
    assert "#SBATCH --constraint=v100" in script


# --- Entrypoint/wrapper mode ---


def test_generate_batch_script_with_entrypoint():
    script = generate_batch_script(
        job_id="ep-job-1234",
        sif_image="",
        partition="gpu",
        cpus=16,
        memory="128G",
        time_limit="08:00:00",
        work_dir="/scratch/work",
        input_dir="/scratch/input",
        output_dir="/scratch/output",
        entrypoint="/nfs/scripts/vtm-pipeline.sh",
    )
    assert script.startswith("#!/bin/bash")
    assert "#SBATCH --partition=gpu" in script
    assert "#SBATCH --cpus-per-task=16" in script

    # Entrypoint env vars
    assert 'export HPC_JOB_ID="ep-job-1234"' in script
    assert 'export HPC_INPUT_DIR="/scratch/input"' in script
    assert 'export HPC_OUTPUT_DIR="/scratch/output"' in script
    assert 'export HPC_WORK_DIR="/scratch/work"' in script
    assert "export HPC_PARAMETERS='{}'" in script
    assert "exec /nfs/scripts/vtm-pipeline.sh" in script

    # Must NOT contain apptainer
    assert "apptainer" not in script


def test_generate_batch_script_with_entrypoint_and_environment():
    script = generate_batch_script(
        job_id="ep-env-job",
        sif_image="",
        partition="normal",
        cpus=4,
        memory="16G",
        time_limit="01:00:00",
        work_dir="/work",
        input_dir="/input",
        output_dir="/output",
        entrypoint="/nfs/scripts/run.sh",
        environment={"MODEL_NAME": "llama-3", "BATCH_SIZE": "256"},
    )
    assert 'export MODEL_NAME="llama-3"' in script
    assert 'export BATCH_SIZE="256"' in script
    assert "exec /nfs/scripts/run.sh" in script
    assert "apptainer" not in script


def test_generate_batch_script_entrypoint_with_parameters():
    params = {"command": "vtm evaluate config.toml", "environment": {"GPU": "0"}}
    script = generate_batch_script(
        job_id="ep-param-job",
        sif_image="",
        partition="gpu",
        cpus=8,
        memory="64G",
        time_limit="04:00:00",
        work_dir="/work",
        input_dir="/input",
        output_dir="/output",
        entrypoint="/nfs/scripts/run.sh",
        parameters=params,
    )
    # HPC_PARAMETERS should contain the JSON-encoded parameters
    assert "HPC_PARAMETERS=" in script
    # Extract the JSON string from the script and verify it round-trips
    for line in script.splitlines():
        if "HPC_PARAMETERS=" in line:
            json_str = line.split("'", 1)[1].rsplit("'", 1)[0]
            parsed = json.loads(json_str)
            assert parsed == params
            break
    else:
        raise AssertionError("HPC_PARAMETERS line not found")
