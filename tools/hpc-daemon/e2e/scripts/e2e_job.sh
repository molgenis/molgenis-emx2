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

# Also write nested outputs to verify recursive artifact path handling.
mkdir -p "${HPC_OUTPUT_DIR}/results/nested/deeper"
mkdir -p "${HPC_OUTPUT_DIR}/reports/2026/03"
echo "nested hello ${HPC_JOB_ID}" > "${HPC_OUTPUT_DIR}/results/nested/output.txt"
cat > "${HPC_OUTPUT_DIR}/results/nested/deeper/meta.json" <<EOF
{"job_id":"${HPC_JOB_ID}","kind":"nested"}
EOF
echo "report-${HPC_JOB_ID}" > "${HPC_OUTPUT_DIR}/reports/2026/03/report.txt"

echo "e2e_job.sh completed successfully"
