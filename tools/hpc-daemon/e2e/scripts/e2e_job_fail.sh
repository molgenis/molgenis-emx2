#!/bin/bash
# Failure e2e job: writes to stderr, exits 1.
set -uo pipefail

echo "e2e_job_fail.sh running for job ${HPC_JOB_ID}"
echo "This job is designed to fail" >&2
echo "ERROR: simulated failure for e2e testing" >&2
exit 1
