#!/bin/sh
# Apptainer e2e job: reads a staged message input and writes output files.
set -eu

set -- /input/*/message.txt
if [ ! -f "$1" ]; then
    echo "FATAL: message.txt not found under /input" >&2
    exit 1
fi

IFS= read -r message < "$1"
printf "%s\n" "$message" > /output/copied-message.txt
printf "Hello from apptainer job %s\n" "$EMX2_JOB_ID" > /output/result.txt
printf '{"job_id":"%s","mode":"apptainer","input":"%s"}\n' \
    "$EMX2_JOB_ID" \
    "$message" \
    > /output/result.json
printf "container-stdout\n"
