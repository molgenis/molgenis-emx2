"""Tests for the HPC client auth header generation."""

from pathlib import Path

import pytest

from emx2_hpc_daemon.auth import sign_request
from emx2_hpc_daemon.client import HpcClient
from emx2_hpc_daemon.testkit import (
    DeterministicIds,
    FakeClock,
    build_signed_test_headers,
)


def test_sign_request_deterministic():
    sig1 = sign_request("POST", "/api/hpc/jobs", "body", "12345", "nonce1", "secret")
    sig2 = sign_request("POST", "/api/hpc/jobs", "body", "12345", "nonce1", "secret")
    assert sig1 == sig2


def test_sign_request_different_with_different_nonce():
    sig1 = sign_request("POST", "/api/hpc/jobs", "body", "12345", "nonce1", "secret")
    sig2 = sign_request("POST", "/api/hpc/jobs", "body", "12345", "nonce2", "secret")
    assert sig1 != sig2


def test_build_authorization_header():
    ids = DeterministicIds("test-client")
    clock = FakeClock(unix_seconds=1_700_001_234)
    headers = build_signed_test_headers(
        "POST",
        "/api/hpc/jobs",
        "body",
        secret="secret",
        ids=ids,
        clock=clock,
    )
    assert "Authorization" in headers
    assert headers["Authorization"].startswith("HMAC-SHA256 ")
    assert headers["X-EMX2-API-Version"] == "2025-01"
    assert "X-Request-Id" in headers
    assert "X-Timestamp" in headers
    assert "X-Nonce" in headers
    assert headers["X-Timestamp"] == "1700001234"
    assert len(headers["X-Nonce"]) == 32


def test_download_artifact_files_raises_when_any_file_fails(tmp_path: Path):
    client = HpcClient(
        base_url="http://example.invalid",
        worker_id="worker-1",
        worker_secret="secret",
    )
    client.list_artifact_files = lambda artifact_id: [  # type: ignore[method-assign]
        {"path": "ok.txt"},
        {"path": "broken.txt"},
    ]

    def _download(_artifact_id: str, path: str, local_path: str) -> None:
        if path == "broken.txt":
            raise RuntimeError("network error")
        out = Path(local_path)
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text("ok")

    client.download_artifact_file = _download  # type: ignore[method-assign]

    with pytest.raises(RuntimeError, match="broken.txt"):
        client.download_artifact_files("art-1", str(tmp_path))

    client.close()
