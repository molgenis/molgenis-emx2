"""Tests for the HPC client auth header generation."""

from emx2_hpc_daemon.auth import sign_request
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
