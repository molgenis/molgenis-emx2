"""Tests for the HPC client auth header generation."""

from emx2_hpc_daemon.auth import build_authorization_header, sign_request


def test_sign_request_deterministic():
    sig1 = sign_request("POST", "/api/hpc/jobs", "body", "12345", "nonce1", "secret")
    sig2 = sign_request("POST", "/api/hpc/jobs", "body", "12345", "nonce1", "secret")
    assert sig1 == sig2


def test_sign_request_different_with_different_nonce():
    sig1 = sign_request("POST", "/api/hpc/jobs", "body", "12345", "nonce1", "secret")
    sig2 = sign_request("POST", "/api/hpc/jobs", "body", "12345", "nonce2", "secret")
    assert sig1 != sig2


def test_build_authorization_header():
    headers, timestamp, nonce = build_authorization_header(
        "POST", "/api/hpc/jobs", "body", "secret"
    )
    assert "Authorization" in headers
    assert headers["Authorization"].startswith("HMAC-SHA256 ")
    assert "X-Timestamp" in headers
    assert "X-Nonce" in headers
    assert len(nonce) == 32  # uuid4 hex
