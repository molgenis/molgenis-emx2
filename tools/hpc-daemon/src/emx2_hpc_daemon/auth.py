"""HMAC-SHA256 request signing for the HPC bridge protocol.

Canonical request string: method + path + body_hash + timestamp + nonce
Keyed with shared secret. Pluggable for JWT/mTLS in future.
"""

from __future__ import annotations

import hashlib
import hmac
import time
import uuid


def generate_nonce() -> str:
    """Generate a unique nonce for request signing."""
    return uuid.uuid4().hex


def generate_timestamp() -> str:
    """Generate an ISO-like timestamp for request signing."""
    return str(int(time.time()))


def sign_request(
    method: str,
    path: str,
    body: bytes | str,
    timestamp: str,
    nonce: str,
    secret: str,
) -> str:
    """
    Compute HMAC-SHA256 signature for a request.

    The canonical request string is:
        METHOD\nPATH\nBODY_SHA256\nTIMESTAMP\nNONCE

    Returns the hex-encoded signature.
    """
    if isinstance(body, str):
        body = body.encode("utf-8")

    body_hash = hashlib.sha256(body).hexdigest()
    canonical = f"{method.upper()}\n{path}\n{body_hash}\n{timestamp}\n{nonce}"

    signature = hmac.new(
        secret.encode("utf-8"),
        canonical.encode("utf-8"),
        hashlib.sha256,
    ).hexdigest()

    return signature


def build_authorization_header(
    method: str,
    path: str,
    body: bytes | str,
    secret: str,
) -> tuple[dict[str, str], str, str]:
    """
    Build all auth-related headers for a request.

    Returns (headers_dict, timestamp, nonce) where headers_dict contains:
    - Authorization: HMAC-SHA256 <signature>
    - X-Timestamp: <timestamp>
    - X-Nonce: <nonce>
    """
    timestamp = generate_timestamp()
    nonce = generate_nonce()
    signature = sign_request(method, path, body, timestamp, nonce, secret)

    headers = {
        "Authorization": f"HMAC-SHA256 {signature}",
        "X-Timestamp": timestamp,
        "X-Nonce": nonce,
    }
    return headers, timestamp, nonce
