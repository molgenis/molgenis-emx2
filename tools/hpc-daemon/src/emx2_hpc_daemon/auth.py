"""HMAC-SHA256 request signing for the HPC bridge protocol.

Canonical request string: method + path + body_hash + timestamp + nonce
Keyed with per-worker secret material. Pluggable for JWT/mTLS in future.

For binary uploads, pass the pre-computed SHA-256 of the body via the
``body_hash`` parameter to avoid double-hashing (the Content-SHA256 header
value is used directly in the canonical string).
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
    *,
    body_hash: str | None = None,
) -> str:
    """
    Compute HMAC-SHA256 signature for a request.

    The canonical request string is:
        METHOD\\nPATH\\nBODY_SHA256\\nTIMESTAMP\\nNONCE

    When ``body_hash`` is provided (hex-encoded SHA-256), it is used directly
    as the body hash component of the canonical string. This is used for binary
    uploads where the Content-SHA256 header carries the pre-computed hash.

    Returns the hex-encoded signature.
    """
    if body_hash is not None:
        computed_body_hash = body_hash
    else:
        if isinstance(body, str):
            body = body.encode("utf-8")
        computed_body_hash = hashlib.sha256(body).hexdigest()

    canonical = f"{method.upper()}\n{path}\n{computed_body_hash}\n{timestamp}\n{nonce}"

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
    *,
    body_hash: str | None = None,
) -> tuple[dict[str, str], str, str]:
    """
    Build all auth-related headers for a request.

    Returns (headers_dict, timestamp, nonce) where headers_dict contains:
    - Authorization: HMAC-SHA256 <signature>
    - X-Timestamp: <timestamp>
    - X-Nonce: <nonce>

    When ``body_hash`` is provided, it is used directly as the body hash
    in the canonical string (for binary uploads with Content-SHA256).
    """
    timestamp = generate_timestamp()
    nonce = generate_nonce()
    signature = sign_request(
        method, path, body, timestamp, nonce, secret, body_hash=body_hash
    )

    headers = {
        "Authorization": f"HMAC-SHA256 {signature}",
        "X-Timestamp": timestamp,
        "X-Nonce": nonce,
    }
    return headers, timestamp, nonce
