"""Progress file monitoring logic extracted from HpcDaemon.

All functions are free functions receiving explicit dependencies.
"""

from __future__ import annotations

import hashlib
import json
import logging
from pathlib import Path

from .client import HpcClient
from .tracker import TrackedJob

logger = logging.getLogger(__name__)


def parse_last_progress_line(raw: str) -> dict | None:
    """Parse the last valid JSON line from an NDJSON progress file.

    Iterates lines in reverse, skipping blank lines and lines that
    fail to parse (e.g. a partial write in progress).  Returns None
    if no valid line is found.
    """
    for line in reversed(raw.splitlines()):
        line = line.strip()
        if not line:
            continue
        try:
            obj = json.loads(line)
            if isinstance(obj, dict):
                return obj
        except (json.JSONDecodeError, ValueError):
            continue
    return None


def validate_progress(raw_progress: dict) -> dict:
    """Validate and sanitize a parsed progress object.

    - ``phase``: optional str, truncated to 100 chars
    - ``message``: optional str, truncated to 500 chars
    - ``progress``: optional float, clamped to [0.0, 1.0]
    - Unknown keys are silently dropped.
    """
    result: dict = {}
    if "phase" in raw_progress:
        v = raw_progress["phase"]
        if isinstance(v, str):
            result["phase"] = v[:100]
    if "message" in raw_progress:
        v = raw_progress["message"]
        if isinstance(v, str):
            result["message"] = v[:500]
    if "progress" in raw_progress:
        v = raw_progress["progress"]
        try:
            result["progress"] = max(0.0, min(1.0, float(v)))
        except (TypeError, ValueError):
            pass
    return result


def check_progress_file(client: HpcClient, tracked: TrackedJob) -> None:
    """Read .hpc_progress.jsonl from output_dir and relay updates to EMX2.

    The workload appends one JSON object per line (NDJSON format).
    The daemon reads the last complete line -- partial writes from
    in-progress appends are safely skipped.
    """
    if tracked.status != "STARTED" or not tracked.output_dir:
        return

    progress_path = Path(tracked.output_dir) / ".hpc_progress.jsonl"
    if not progress_path.is_file():
        return

    try:
        raw = progress_path.read_text(encoding="utf-8", errors="replace")
        content_hash = hashlib.md5(raw.encode()).hexdigest()
        if content_hash == tracked.last_progress_hash:
            return

        tracked.last_progress_hash = content_hash

        progress = parse_last_progress_line(raw)
        if progress is None:
            return
        progress = validate_progress(progress)
        if not progress:
            return

        parts = []
        if "phase" in progress:
            parts.append(progress["phase"])
        if "message" in progress:
            parts.append(progress["message"])
        if "progress" in progress:
            parts.append(f"{progress['progress']:.0%}")
        detail = "; ".join(parts) if parts else "progress update"

        client.transition_job(
            tracked.emx2_job_id,
            "STARTED",
            detail=f"progress: {detail}",
            phase=progress.get("phase"),
            message=progress.get("message"),
            progress=progress.get("progress"),
        )
        logger.debug(
            "Relayed progress for job %s: %s",
            tracked.emx2_job_id,
            detail,
        )
    except OSError:
        logger.debug(
            "Could not read progress file for job %s",
            tracked.emx2_job_id,
        )
