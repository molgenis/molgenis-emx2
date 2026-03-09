#!/bin/bash
# E2E transform job: reads input CSV, performs a real transformation, writes outputs.
# Verifies the full artifact round-trip: upload → stage → transform → collect.
#
# Input:  $HPC_INPUT_DIR/<artifact-id>/measurements.csv
#         CSV with columns: sample_id,temperature,pressure,label
#
# Output: $HPC_OUTPUT_DIR/summary.json     — per-label statistics
#         $HPC_OUTPUT_DIR/sorted.csv        — rows sorted by temperature desc
#         $HPC_OUTPUT_DIR/manifest.txt      — sha256 of each output file
set -euo pipefail

echo "e2e_job_transform.sh running for job ${HPC_JOB_ID}"
echo "Parameters: ${HPC_PARAMETERS}"

# Find the input CSV — it's in the first (only) artifact subdirectory
INPUT_CSV=$(find "${HPC_INPUT_DIR}" -name "measurements.csv" -type f | head -1)
if [ -z "$INPUT_CSV" ]; then
    echo "FATAL: measurements.csv not found in ${HPC_INPUT_DIR}" >&2
    ls -laR "${HPC_INPUT_DIR}" >&2
    exit 1
fi
echo "Found input: ${INPUT_CSV}"
echo '{"phase":"loading","message":"reading input CSV","progress":0.0}' >> "${HPC_OUTPUT_DIR}/.hpc_progress.jsonl"
echo "Input contents (first 5 lines):"
head -5 "$INPUT_CSV"

LINE_COUNT=$(tail -n +2 "$INPUT_CSV" | wc -l)
echo "Data rows: ${LINE_COUNT}"

echo '{"phase":"sorting","message":"sorting by temperature","progress":0.3}' >> "${HPC_OUTPUT_DIR}/.hpc_progress.jsonl"

# --- Transform 1: sort by temperature descending ---
head -1 "$INPUT_CSV" > "${HPC_OUTPUT_DIR}/sorted.csv"
tail -n +2 "$INPUT_CSV" | sort -t',' -k2 -rn >> "${HPC_OUTPUT_DIR}/sorted.csv"
echo "Wrote sorted.csv"

echo '{"phase":"statistics","message":"computing per-label stats","progress":0.6}' >> "${HPC_OUTPUT_DIR}/.hpc_progress.jsonl"

# --- Transform 2: compute per-label statistics using awk ---
# Produces JSON with count, mean temperature, min/max pressure per label.
awk -F',' '
BEGIN { OFS="," }
NR == 1 { next }  # skip header
{
    label = $4
    temp = $2 + 0
    pres = $3 + 0
    count[label]++
    sum_temp[label] += temp
    sum_pres[label] += pres
    if (!(label in min_pres) || pres < min_pres[label]) min_pres[label] = pres
    if (!(label in max_pres) || pres > max_pres[label]) max_pres[label] = pres
    if (!(label in min_temp) || temp < min_temp[label]) min_temp[label] = temp
    if (!(label in max_temp) || temp > max_temp[label]) max_temp[label] = temp
}
END {
    printf "{\n  \"job_id\": \"'"${HPC_JOB_ID}"'\",\n  \"total_rows\": %d,\n  \"labels\": {\n", NR-1
    n = 0
    for (l in count) n++
    i = 0
    for (l in count) {
        i++
        mean_t = sum_temp[l] / count[l]
        mean_p = sum_pres[l] / count[l]
        printf "    \"%s\": {\n", l
        printf "      \"count\": %d,\n", count[l]
        printf "      \"mean_temperature\": %.2f,\n", mean_t
        printf "      \"min_temperature\": %.2f,\n", min_temp[l]
        printf "      \"max_temperature\": %.2f,\n", max_temp[l]
        printf "      \"mean_pressure\": %.2f,\n", mean_p
        printf "      \"min_pressure\": %.2f,\n", min_pres[l]
        printf "      \"max_pressure\": %.2f\n", max_pres[l]
        if (i < n) printf "    },\n"
        else printf "    }\n"
    }
    printf "  }\n}\n"
}
' "$INPUT_CSV" > "${HPC_OUTPUT_DIR}/summary.json"
echo "Wrote summary.json"

echo '{"phase":"finalizing","message":"computing checksums","progress":0.9}' >> "${HPC_OUTPUT_DIR}/.hpc_progress.jsonl"

# --- Transform 3: manifest with SHA-256 of each output ---
cd "${HPC_OUTPUT_DIR}"
sha256sum sorted.csv summary.json > manifest.txt
echo "Wrote manifest.txt"

echo "e2e_job_transform.sh completed successfully"
