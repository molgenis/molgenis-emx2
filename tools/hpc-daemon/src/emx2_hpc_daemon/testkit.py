"""Shared HPC testkit helpers used across unit tests and e2e tests."""

from __future__ import annotations

import hashlib
import shutil
import uuid
from collections.abc import Callable, Iterator, Mapping
from contextlib import contextmanager
from dataclasses import dataclass
from pathlib import Path

from .auth import sign_request
from .hashing import compute_tree_hash

_DEFAULT_API_VERSION = "2025-01"
_DEFAULT_TMP_ROOT = Path("/tmp/emx2-hpc-testkit")


@dataclass
class FakeClock:
    """Simple deterministic clock for request signing tests."""

    unix_seconds: int = 1_700_000_000

    def now(self) -> int:
        return self.unix_seconds

    def tick(self, seconds: int = 1) -> int:
        self.unix_seconds += seconds
        return self.unix_seconds


class DeterministicIds:
    """Deterministic UUID/nonce generator for tests."""

    def __init__(self, namespace: str = "emx2-hpc-testkit") -> None:
        self._namespace = namespace
        self._counter = 0

    def next_uuid(self, prefix: str = "id") -> str:
        self._counter += 1
        token = f"{self._namespace}:{prefix}:{self._counter}"
        return str(uuid.uuid5(uuid.NAMESPACE_URL, token))

    def next_nonce(self) -> str:
        return self.next_uuid("nonce").replace("-", "")


def build_signed_test_headers(
    method: str,
    path: str,
    body: bytes | str,
    *,
    secret: str,
    ids: DeterministicIds | None = None,
    clock: FakeClock | None = None,
    body_hash: str | None = None,
    api_version: str = _DEFAULT_API_VERSION,
) -> dict[str, str]:
    """Build full signed headers with deterministic request-id/timestamp/nonce."""
    ids = ids or DeterministicIds()
    clock = clock or FakeClock()
    timestamp = str(clock.now())
    nonce = ids.next_nonce()
    request_id = ids.next_uuid("request")
    signature = sign_request(
        method,
        path,
        body,
        timestamp,
        nonce,
        secret,
        body_hash=body_hash,
    )
    return {
        "Authorization": f"HMAC-SHA256 {signature}",
        "X-EMX2-API-Version": api_version,
        "X-Request-Id": request_id,
        "X-Timestamp": timestamp,
        "X-Nonce": nonce,
    }


def artifact_record(
    artifact_id: str,
    *,
    artifact_type: str = "blob",
    residence: str = "managed",
    status: str = "COMMITTED",
    sha256: str | None = None,
    size_bytes: int | None = None,
    content_url: str | None = None,
    links: Mapping[str, object] | None = None,
) -> dict[str, object]:
    """Create a canonical artifact dictionary for tests."""
    rec: dict[str, object] = {
        "id": artifact_id,
        "type": artifact_type,
        "residence": residence,
        "status": status,
        "_links": dict(links or {}),
    }
    if sha256 is not None:
        rec["sha256"] = sha256
    if size_bytes is not None:
        rec["size_bytes"] = size_bytes
    if content_url is not None:
        rec["content_url"] = content_url
    return rec


def make_artifact_factory(prefix: str = "art") -> Callable[..., dict[str, object]]:
    """Create a deterministic artifact factory for mock clients."""
    counter = {"value": 0}

    def _create(
        *,
        artifact_type: str = "blob",
        residence: str = "managed",
        status: str = "CREATED",
        name: str | None = None,
        **_: object,
    ) -> dict[str, object]:
        counter["value"] += 1
        artifact_id = f"{prefix}-{artifact_type}-{counter['value']:04d}"
        rec = artifact_record(
            artifact_id,
            artifact_type=artifact_type,
            residence=residence,
            status=status,
            links={},
        )
        if name is not None:
            rec["name"] = name
        return rec

    return _create


@contextmanager
def deterministic_temp_dir(
    name: str,
    *,
    root: Path | None = None,
    keep: bool = False,
) -> Iterator[Path]:
    """Provide a deterministic temp directory path for tests."""
    root = root or _DEFAULT_TMP_ROOT
    safe_name = "".join(ch if ch.isalnum() or ch in "-._" else "_" for ch in name)
    target = root / safe_name
    if target.exists():
        shutil.rmtree(target)
    target.mkdir(parents=True, exist_ok=True)
    try:
        yield target
    finally:
        if not keep:
            shutil.rmtree(target, ignore_errors=True)


def create_and_commit_managed_artifact(
    client,
    *,
    files: Mapping[str, bytes],
    artifact_type: str = "blob",
    name: str | None = None,
) -> dict[str, object]:
    """Create a managed artifact, upload files, and commit with a canonical tree hash."""
    created = client.create_artifact(
        artifact_type=artifact_type,
        residence="managed",
        name=name,
    )
    artifact_id = created["id"]

    file_items: list[tuple[str, bytes]] = []
    total_size = 0
    for rel_path, content in files.items():
        client.upload_artifact_file(
            artifact_id=artifact_id,
            path=rel_path,
            file_content=content,
        )
        file_items.append((rel_path, content))
        total_size += len(content)

    tree_hash = compute_tree_hash(file_items)
    client.commit_artifact(
        artifact_id=artifact_id,
        sha256=tree_hash,
        size_bytes=total_size,
    )
    return {
        "id": artifact_id,
        "sha256": tree_hash,
        "size_bytes": total_size,
    }


def sha256_hex(data: bytes | str) -> str:
    """Hex SHA-256 helper for test assertions."""
    if isinstance(data, str):
        data = data.encode("utf-8")
    return hashlib.sha256(data).hexdigest()
