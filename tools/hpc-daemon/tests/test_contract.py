"""Conformance test: asserts generated Python constants match the protocol spec.

If this test fails, run: uv run python protocol/generate.py
"""

from __future__ import annotations

import json
from pathlib import Path

import pytest

from emx2_hpc_daemon._generated import (
    API_VERSION,
    ARTIFACT_RESIDENCES,
    ARTIFACT_STATUSES,
    JOB_STATUSES,
    TERMINAL_STATUSES,
    TRANSITIONS,
    is_terminal,
)

SPEC_PATH = Path(__file__).resolve().parent.parent.parent.parent / "protocol" / "hpc-protocol.json"


@pytest.fixture(scope="module")
def spec():
    with open(SPEC_PATH) as f:
        return json.load(f)["definitions"]


def test_api_version(spec):
    assert API_VERSION == spec["apiVersion"]["const"]


def test_job_statuses(spec):
    assert list(JOB_STATUSES) == spec["HpcJobStatus"]["enum"]


def test_artifact_statuses(spec):
    assert list(ARTIFACT_STATUSES) == spec["ArtifactStatus"]["enum"]


def test_artifact_residences(spec):
    assert list(ARTIFACT_RESIDENCES) == spec["ArtifactResidence"]["enum"]


def test_terminal_statuses(spec):
    assert TERMINAL_STATUSES == set(spec["terminalStatuses"]["const"])


def test_transitions(spec):
    spec_transitions = spec["transitions"]["properties"]
    for status, targets_spec in spec_transitions.items():
        expected = set(targets_spec["const"])
        actual = TRANSITIONS.get(status, frozenset())
        assert actual == expected, f"Transitions for {status}: expected {expected}, got {actual}"


def test_is_terminal():
    assert is_terminal("COMPLETED") is True
    assert is_terminal("FAILED") is True
    assert is_terminal("CANCELLED") is True
    assert is_terminal("PENDING") is False
    assert is_terminal("STARTED") is False
