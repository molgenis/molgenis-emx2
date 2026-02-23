#!/bin/bash
# apptainer_run.sh — Wrapper for running compute jobs in Apptainer containers.
#
# This script handles:
# 1. Validating inputs and creating temporary working directory
# 2. Building bind mount arguments
# 3. Running the container via Apptainer with --cleanenv
# 4. Capturing stdout/stderr to log files
# 5. Propagating the container exit code
# 6. Cleaning up temporary files
#
# Usage: apptainer_run.sh <SIF_IMAGE> <INPUT_DIR> <OUTPUT_DIR> [COMMAND...] [-- EXTRA_BIND_PATHS...]
#
# Environment variables:
#   EMX2_JOB_ID      — EMX2 job identifier (for logging)
#   SLURM_JOB_ID     — Slurm job ID (set automatically by Slurm)
#   APPTAINER_TMPDIR  — Override for temporary directory

set -euo pipefail

SIF_IMAGE="${1:?Usage: apptainer_run.sh <SIF_IMAGE> <INPUT_DIR> <OUTPUT_DIR> [COMMAND...] [-- EXTRA_BINDS...]}"
INPUT_DIR="${2:?Input directory required}"
OUTPUT_DIR="${3:?Output directory required}"
shift 3

# Parse remaining args: everything before "--" is the command, after is extra binds
CONTAINER_CMD=()
EXTRA_BINDS=""
PARSING_CMD=true

for arg in "$@"; do
    if [ "$arg" = "--" ]; then
        PARSING_CMD=false
        continue
    fi
    if $PARSING_CMD; then
        CONTAINER_CMD+=("$arg")
    else
        if [ -n "$EXTRA_BINDS" ]; then
            EXTRA_BINDS="${EXTRA_BINDS},${arg}"
        else
            EXTRA_BINDS="${arg}"
        fi
    fi
done

# Default command if none specified
if [ ${#CONTAINER_CMD[@]} -eq 0 ]; then
    CONTAINER_CMD=("/bin/bash" "-c" "echo 'No command specified'; exit 1")
fi

# Set up temporary working directory
TMPDIR="${APPTAINER_TMPDIR:-/tmp}"
WORKDIR=$(mktemp -d "${TMPDIR}/emx2-hpc-XXXXXX")

echo "=== EMX2 HPC Job ==="
echo "Job ID: ${EMX2_JOB_ID:-unknown}"
echo "Slurm Job ID: ${SLURM_JOB_ID:-unknown}"
echo "SIF Image: ${SIF_IMAGE}"
echo "Input Dir: ${INPUT_DIR}"
echo "Output Dir: ${OUTPUT_DIR}"
echo "Work Dir: ${WORKDIR}"
echo "Command: ${CONTAINER_CMD[*]}"
echo "Start: $(date -Iseconds)"
echo "===================="

# Build bind mount arguments
BIND_ARGS="${INPUT_DIR}:/input:ro,${OUTPUT_DIR}:/output,${WORKDIR}:/work"
if [ -n "$EXTRA_BINDS" ]; then
    BIND_ARGS="${BIND_ARGS},${EXTRA_BINDS}"
fi

# Create log files in output directory
STDOUT_LOG="${OUTPUT_DIR}/container-stdout.log"
STDERR_LOG="${OUTPUT_DIR}/container-stderr.log"

# Run the container with cleanenv for isolation
EXIT_CODE=0
apptainer exec \
    --cleanenv \
    --bind "${BIND_ARGS}" \
    --pwd /work \
    --env "EMX2_JOB_ID=${EMX2_JOB_ID:-}" \
    "${SIF_IMAGE}" \
    "${CONTAINER_CMD[@]}" \
    > "${STDOUT_LOG}" 2> "${STDERR_LOG}" \
    || EXIT_CODE=$?

echo "=== Job Complete ==="
echo "Exit Code: ${EXIT_CODE}"
echo "End: $(date -Iseconds)"
echo "Stdout log: ${STDOUT_LOG}"
echo "Stderr log: ${STDERR_LOG}"
echo "===================="

# Show last lines of stderr if job failed
if [ "$EXIT_CODE" -ne 0 ] && [ -s "${STDERR_LOG}" ]; then
    echo "--- Last 20 lines of stderr ---"
    tail -20 "${STDERR_LOG}"
    echo "--- End stderr ---"
fi

# Cleanup temporary working directory
rm -rf "${WORKDIR}"

exit "${EXIT_CODE}"
