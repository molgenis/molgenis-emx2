"""AUTO-GENERATED from protocol/hpc-protocol.json — do not edit."""

from __future__ import annotations

API_VERSION: str = "2025-01"

JOB_STATUSES: tuple[str, ...] = ('PENDING', 'CLAIMED', 'SUBMITTED', 'STARTED', 'COMPLETED', 'FAILED', 'CANCELLED')

TERMINAL_STATUSES: frozenset[str] = frozenset(['CANCELLED', 'COMPLETED', 'FAILED'])

ARTIFACT_STATUSES: tuple[str, ...] = ('CREATED', 'UPLOADING', 'REGISTERED', 'COMMITTED', 'FAILED')

ARTIFACT_RESIDENCES: tuple[str, ...] = ('managed', 'posix', 's3', 'http', 'reference')

TRANSITIONS: dict[str, frozenset[str]] = {
    "PENDING": frozenset(['CANCELLED', 'CLAIMED']),
    "CLAIMED": frozenset(['CANCELLED', 'FAILED', 'SUBMITTED']),
    "SUBMITTED": frozenset(['CANCELLED', 'FAILED', 'STARTED']),
    "STARTED": frozenset(['CANCELLED', 'COMPLETED', 'FAILED']),
    "COMPLETED": frozenset(),
    "FAILED": frozenset(),
    "CANCELLED": frozenset(),
}


def is_terminal(status: str) -> bool:
    """Return True if the given status is a terminal state."""
    return status in TERMINAL_STATUSES
