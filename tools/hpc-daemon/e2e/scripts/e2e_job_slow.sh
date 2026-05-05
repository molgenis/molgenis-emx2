#!/bin/bash
# Slow e2e job: writes a marker then sleeps (for cancellation testing).
set -euo pipefail

echo "e2e_job_slow.sh running for job ${HPC_JOB_ID}"

# Write a marker so tests know the job has started
echo "started" > "${HPC_OUTPUT_DIR}/marker.txt"

# Sleep long enough for the test to cancel us
sleep 90

echo "e2e_job_slow.sh completed (should not reach here if cancelled)"
