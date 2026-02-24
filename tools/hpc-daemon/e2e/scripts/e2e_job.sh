#!/bin/bash
# Happy-path e2e job: writes output files, exits 0.
set -euo pipefail

echo "e2e_job.sh running for job ${HPC_JOB_ID}"
echo "Parameters: ${HPC_PARAMETERS}"

# Write result files to the output directory
echo "Hello from e2e job ${HPC_JOB_ID}" > "${HPC_OUTPUT_DIR}/result.txt"

cat > "${HPC_OUTPUT_DIR}/result.json" <<EOF
{
  "job_id": "${HPC_JOB_ID}",
  "status": "success",
  "message": "e2e test completed"
}
EOF

echo "e2e_job.sh completed successfully"
