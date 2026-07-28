#!/usr/bin/env bash
# Asserts that the task-outcome artifact written by gradle/task-outcomes.gradle covers the gradle
# invocation whose console log is given. Deleting the recorder therefore fails the build.
# usage: check-task-outcomes.sh <gradle log> <task outcome tsv glob>
set -euo pipefail

log="$1"
outcomes_glob="$2"

shopt -s nullglob
matches=($outcomes_glob)
shopt -u nullglob

if [ ${#matches[@]} -eq 0 ]; then
  echo "no task outcome artifact matched $outcomes_glob"
  exit 1
fi

outcomes="${matches[0]}"
for candidate in "${matches[@]}"; do
  if [ "$candidate" -nt "$outcomes" ]; then
    outcomes="$candidate"
  fi
done
echo "checking $outcomes"

if [ ! -s "$outcomes" ]; then
  echo "task outcome artifact $outcomes is missing or empty"
  exit 1
fi

summary=$(grep -E '^[0-9]+ actionable tasks?:' "$log" | tail -1 || true)
if [ -z "$summary" ]; then
  echo "no 'N actionable tasks' summary line in $log, so the artifact cannot be checked"
  exit 1
fi

count_in_summary() {
  echo "$summary" | grep -oE "[0-9]+ $1" | grep -oE '^[0-9]+' || echo 0
}
actionable=$(echo "$summary" | grep -oE '^[0-9]+')
summary_executed=$(count_in_summary 'executed')
summary_from_cache=$(count_in_summary 'from cache')

recorded=$(wc -l < "$outcomes" | tr -d ' ')
recorded_executed=$(awk -F'\t' '$2 == "executed" && $4 != "" { n++ } END { print n + 0 }' "$outcomes")
recorded_from_cache=$(awk -F'\t' '$2 == "fromCache" { n++ } END { print n + 0 }' "$outcomes")

echo "summary: $summary"
echo "recorded: $recorded tasks, $recorded_executed executed, $recorded_from_cache from cache"

status=0
# Every actionable task is recorded, plus the lifecycle tasks that gradle leaves out of its own count.
if [ "$recorded" -lt "$actionable" ]; then
  echo "artifact records $recorded tasks but gradle counted $actionable actionable tasks"
  status=1
fi
if [ "$recorded_executed" -ne "$summary_executed" ]; then
  echo "artifact records $recorded_executed executed tasks, gradle reported $summary_executed"
  status=1
fi
if [ "$recorded_from_cache" -ne "$summary_from_cache" ]; then
  echo "artifact records $recorded_from_cache from-cache tasks, gradle reported $summary_from_cache"
  status=1
fi
exit $status
