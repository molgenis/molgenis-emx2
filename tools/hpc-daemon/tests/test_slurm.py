"""Tests for Slurm batch script generation (does not require actual Slurm)."""

from emx2_hpc_daemon.slurm import generate_batch_script


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
