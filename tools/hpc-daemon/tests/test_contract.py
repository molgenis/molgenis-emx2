"""Protocol conformance and compatibility checks for the Python daemon.

Source of truth: protocol/hpc-protocol.json
"""

from __future__ import annotations

import hashlib
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
from emx2_hpc_daemon.auth import sign_request
from emx2_hpc_daemon.client import HpcClient

SPEC_PATH = (
    Path(__file__).resolve().parent.parent.parent.parent
    / "protocol"
    / "hpc-protocol.json"
)


@pytest.fixture(scope="module")
def defs():
    with open(SPEC_PATH) as f:
        return json.load(f)["definitions"]


def _required(defs: dict, section: str) -> set[str]:
    return set(defs["responseRequiredFields"]["properties"][section]["const"])


def test_api_version(defs):
    assert API_VERSION == defs["apiVersion"]["const"]


def test_job_statuses(defs):
    assert list(JOB_STATUSES) == defs["HpcJobStatus"]["enum"]


def test_artifact_statuses(defs):
    assert list(ARTIFACT_STATUSES) == defs["ArtifactStatus"]["enum"]


def test_artifact_residences(defs):
    assert list(ARTIFACT_RESIDENCES) == defs["ArtifactResidence"]["enum"]


def test_terminal_statuses(defs):
    assert TERMINAL_STATUSES == set(defs["terminalStatuses"]["const"])


def test_transitions(defs):
    spec_transitions = defs["transitions"]["properties"]
    for status, targets_spec in spec_transitions.items():
        expected = set(targets_spec["const"])
        actual = TRANSITIONS.get(status, frozenset())
        assert actual == expected, (
            f"Transitions for {status}: expected {expected}, got {actual}"
        )


def test_is_terminal():
    assert is_terminal("COMPLETED") is True
    assert is_terminal("FAILED") is True
    assert is_terminal("CANCELLED") is True
    assert is_terminal("PENDING") is False
    assert is_terminal("STARTED") is False


def test_required_headers_present_on_signed_requests(defs):
    client = HpcClient(
        base_url="http://example.invalid",
        worker_id="worker-1",
        worker_secret="contract-secret-0123456789abcdef0123456789",
        auth_mode="hmac",
    )
    try:
        headers = client._headers("POST", "/api/hpc/jobs", "{}")
        required_headers = set(defs["requiredHeaders"]["const"])
        assert required_headers.issubset(headers.keys())
        assert headers["X-EMX2-API-Version"] == API_VERSION
        assert "Authorization" in headers
        assert "X-Nonce" in headers
    finally:
        client.close()


def test_binary_upload_sends_content_sha256(defs):
    expected_hash = hashlib.sha256(b"hello world").hexdigest()

    class DummyResponse:
        status_code = 201
        reason_phrase = "Created"
        content = b'{"ok": true}'

        def raise_for_status(self):
            return None

        def json(self):
            return {"ok": True}

    captured: dict[str, object] = {}
    client = HpcClient(
        base_url="http://example.invalid",
        worker_id="worker-1",
        worker_secret="contract-secret-0123456789abcdef0123456789",
        auth_mode="hmac",
    )
    try:
        original = client._http.request

        def fake_request(method, path, content=None, headers=None, timeout=None):
            captured["method"] = method
            captured["path"] = path
            captured["headers"] = dict(headers or {})
            return DummyResponse()

        client._http.request = fake_request  # type: ignore[assignment]
        client.upload_artifact_file(
            artifact_id="550e8400-e29b-41d4-a716-446655440000",
            path="results/output.bin",
            file_content=b"hello world",
        )
    finally:
        client._http.request = original  # type: ignore[assignment]
        client.close()

    headers = captured["headers"]
    assert isinstance(headers, dict)
    assert headers.get("Content-SHA256") == expected_hash
    assert set(defs["binaryBodyHeaders"]["const"]).issubset(headers.keys())
    assert "Authorization" in headers


def test_hmac_vectors_match_spec(defs):
    secret = defs["hmacFixtureSecret"]["const"]
    for vector in defs["hmacVectors"]["const"]:
        actual = sign_request(
            vector["method"],
            vector["path"],
            vector["body"],
            vector["timestamp"],
            vector["nonce"],
            secret,
            body_hash=vector.get("content_sha256"),
        )
        assert actual == vector["expected_signature"], vector["name"]


def test_protocol_response_field_contracts_are_nonempty(defs):
    # Guard rail: ensures response shape contracts exist and don't regress to empty lists.
    for section in (
        "health",
        "workerRegister",
        "job",
        "jobList",
        "jobTransitions",
        "artifact",
        "artifactFileUpload",
        "artifactFileList",
    ):
        assert _required(defs, section), (
            f"Missing required response field contract: {section}"
        )
