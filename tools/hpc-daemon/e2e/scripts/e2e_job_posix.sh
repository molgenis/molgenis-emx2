#!/bin/bash
# Posix residence e2e job: writes output files, exits 0.
set -euo pipefail

echo "e2e_job_posix.sh running for job ${HPC_JOB_ID}"

# Generate a 1MB sample file
dd if=/dev/urandom of="${HPC_OUTPUT_DIR}/sample.bin" bs=1024 count=1024 2>/dev/null

# Write a text result too
echo "Posix job ${HPC_JOB_ID} completed" > "${HPC_OUTPUT_DIR}/result.txt"

echo "e2e_job_posix.sh completed"
