#!/usr/bin/env python3
"""Code generator: reads hpc-protocol.json and emits Python + TS constants.

Usage:
    uv run python protocol/generate.py

Generates:
    tools/hpc-daemon/src/emx2_hpc_daemon/_generated.py
    apps/hpc/app/utils/protocol.ts
"""

from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SCHEMA_PATH = Path(__file__).resolve().parent / "hpc-protocol.json"

PY_OUT = ROOT / "tools" / "hpc-daemon" / "src" / "emx2_hpc_daemon" / "_generated.py"
TS_OUT = ROOT / "apps" / "hpc" / "app" / "utils" / "protocol.ts"

HEADER = "AUTO-GENERATED from protocol/hpc-protocol.json — do not edit"


def load_schema() -> dict:
    with open(SCHEMA_PATH) as f:
        return json.load(f)


def generate_python(defs: dict) -> str:
    lines: list[str] = []
    lines.append(f'"""{HEADER}."""')
    lines.append("")
    lines.append("from __future__ import annotations")
    lines.append("")

    # API version
    api_version = defs["apiVersion"]["const"]
    lines.append(f'API_VERSION: str = "{api_version}"')
    lines.append("")

    # Job statuses
    job_statuses = defs["HpcJobStatus"]["enum"]
    lines.append(f"JOB_STATUSES: tuple[str, ...] = {tuple(job_statuses)!r}")
    lines.append("")

    # Terminal statuses
    terminal = defs["terminalStatuses"]["const"]
    lines.append(f"TERMINAL_STATUSES: frozenset[str] = frozenset({sorted(terminal)!r})")
    lines.append("")

    # Artifact statuses
    artifact_statuses = defs["ArtifactStatus"]["enum"]
    lines.append(f"ARTIFACT_STATUSES: tuple[str, ...] = {tuple(artifact_statuses)!r}")
    lines.append("")

    # Artifact residences
    residences = defs["ArtifactResidence"]["enum"]
    lines.append(f"ARTIFACT_RESIDENCES: tuple[str, ...] = {tuple(residences)!r}")
    lines.append("")

    # Transitions
    transitions = defs["transitions"]["properties"]
    lines.append("TRANSITIONS: dict[str, frozenset[str]] = {")
    for status, spec in transitions.items():
        targets = spec["const"]
        if targets:
            lines.append(f'    "{status}": frozenset({sorted(targets)!r}),')
        else:
            lines.append(f'    "{status}": frozenset(),')
    lines.append("}")
    lines.append("")

    # Helper function
    lines.append("")
    lines.append("def is_terminal(status: str) -> bool:")
    lines.append('    """Return True if the given status is a terminal state."""')
    lines.append("    return status in TERMINAL_STATUSES")
    lines.append("")

    return "\n".join(lines)


def generate_ts(defs: dict) -> str:
    lines: list[str] = []
    lines.append(f"// {HEADER}")
    lines.append("")

    # API version
    api_version = defs["apiVersion"]["const"]
    lines.append(f'export const API_VERSION = "{api_version}";')
    lines.append("")

    # Job statuses
    job_statuses = defs["HpcJobStatus"]["enum"]
    items = ", ".join(f'"{s}"' for s in job_statuses)
    lines.append(f"export const JOB_STATUSES = [{items}] as const;")
    lines.append("")
    lines.append("export type JobStatus = (typeof JOB_STATUSES)[number];")
    lines.append("")

    # Terminal statuses
    terminal = defs["terminalStatuses"]["const"]
    t_items = ", ".join(f'"{s}"' for s in terminal)
    lines.append(f"export const TERMINAL_STATUSES = new Set<JobStatus>([{t_items}]);")
    lines.append("")

    # Artifact statuses
    artifact_statuses = defs["ArtifactStatus"]["enum"]
    a_items = ", ".join(f'"{s}"' for s in artifact_statuses)
    lines.append(f"export const ARTIFACT_STATUSES = [{a_items}] as const;")
    lines.append("")
    lines.append("export type ArtifactStatus = (typeof ARTIFACT_STATUSES)[number];")
    lines.append("")

    # Artifact residences
    residences = defs["ArtifactResidence"]["enum"]
    r_items = ", ".join(f'"{s}"' for s in residences)
    lines.append(f"export const ARTIFACT_RESIDENCES = [{r_items}] as const;")
    lines.append("")
    lines.append(
        "export type ArtifactResidence = (typeof ARTIFACT_RESIDENCES)[number];"
    )
    lines.append("")

    # Helper
    lines.append("export function isTerminal(status: string): boolean {")
    lines.append("  return TERMINAL_STATUSES.has(status as JobStatus);")
    lines.append("}")
    lines.append("")

    return "\n".join(lines)


def main() -> None:
    schema = load_schema()
    defs = schema["definitions"]

    py_code = generate_python(defs)
    PY_OUT.parent.mkdir(parents=True, exist_ok=True)
    PY_OUT.write_text(py_code)
    print(f"Generated {PY_OUT.relative_to(ROOT)}")

    ts_code = generate_ts(defs)
    TS_OUT.parent.mkdir(parents=True, exist_ok=True)
    TS_OUT.write_text(ts_code)
    print(f"Generated {TS_OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
