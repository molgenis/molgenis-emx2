#!/bin/bash
set -euo pipefail

MODE="${1:-full}"
EMX2_URL="${EMX2_BASE_URL:-http://10.0.2.2:8080}"

sync_clock() {
  systemctl restart chrony
  chronyc makestep >/dev/null 2>&1 || true
  for i in $(seq 1 10); do
    if chronyc waitsync 1 0.1 0 0 &>/dev/null; then
      break
    fi
    sleep 1
  done
  hwclock -w >/dev/null 2>&1 || true
}

check_services() {
  local services=(chrony munge slurmdbd slurmctld slurmd cron)
  for service in "${services[@]}"; do
    if ! systemctl is-active --quiet "$service"; then
      echo "Service not active: $service" >&2
      return 1
    fi
  done
  sinfo -h >/dev/null
}

check_emx2_clock_skew() {
  local local_epoch remote_date remote_epoch skew abs_skew
  local_epoch=$(date -u +%s)
  remote_date=$(curl -fsSI --max-time 5 "${EMX2_URL}/api/hpc/health" | sed -n 's/^[Dd]ate: //p' | tr -d '\r' | tail -1 || true)
  if [ -z "$remote_date" ]; then
    return 0
  fi
  remote_epoch=$(date -u -d "$remote_date" +%s 2>/dev/null || true)
  if [ -z "$remote_epoch" ]; then
    return 0
  fi
  skew=$((local_epoch - remote_epoch))
  abs_skew=${skew#-}
  if [ "$abs_skew" -gt 30 ]; then
    echo "Clock skew too large: local=$local_epoch remote=$remote_epoch skew=$skew" >&2
    return 1
  fi
}

sync_clock
if [ "$MODE" != "--quick" ]; then
  check_services
  check_emx2_clock_skew
fi
echo "preflight ok (mode=$MODE, time=$(date -u +%FT%TZ))"
