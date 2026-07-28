#!/usr/bin/env bash
# Measurement probe for ticket 36 step 1: read the Configuration vs Task Execution split out of
# gradle's --profile HTML report and echo it to the CI step log, so the number is readable without
# downloading an artifact. Deliberately does NOT use `set -e` and always exits 0 — a broken parser
# must never fail the job that carries it.
set -uo pipefail

LABEL="${1:-unlabelled}"
REPORT_DIR="${2:-build/reports/profile}"

REPORT=$(find "$REPORT_DIR" -maxdepth 1 -name 'profile-*.html' 2>/dev/null | sort | tail -1)

if [ -z "$REPORT" ]; then
  echo "GRADLE_PROFILE_MISSING[$LABEL]: no profile-*.html found under $REPORT_DIR"
  exit 0
fi

extract() {
  local field="$1"
  # Newline-separated commands inside the {} block, not semicolons: BSD sed (used to develop and
  # verify this locally) rejects `n;s/.../p` on one line, GNU sed (CI) accepts either form.
  sed -n "
/<td>${field}<\/td>/{
n
s/.*<td class=\"numeric\">\([^<]*\)<\/td>.*/\1/p
}" "$REPORT" 2>/dev/null | head -1
}

TOTAL=$(extract "Total Build Time")
STARTUP=$(extract "Startup")
SETTINGS=$(extract "Settings and buildSrc")
LOADING=$(extract "Loading Projects")
CONFIGURING=$(extract "Configuring Projects")
TASK_EXEC=$(extract "Task Execution")

if [ -z "$CONFIGURING" ] || [ -z "$TASK_EXEC" ]; then
  echo "GRADLE_PROFILE_PARSE_FAILED[$LABEL]: could not extract Configuring Projects / Task Execution from $REPORT"
  exit 0
fi

echo "GRADLE_PROFILE[$LABEL]: report=$REPORT total=${TOTAL:-?} startup=${STARTUP:-?} settingsAndBuildSrc=${SETTINGS:-?} loadingProjects=${LOADING:-?} configuringProjects=${CONFIGURING} taskExecution=${TASK_EXEC}"
exit 0
