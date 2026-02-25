"""Tree hash computation for artifact integrity verification.

Matches the server-side tree hash logic in ArtifactService.java exactly:
- Single file: SHA-256 of the file bytes
- Multiple files: SHA-256 of concatenated "path:sha256_hex" strings, sorted by path
"""

from __future__ import annotations

import hashlib
from pathlib import Path

_HASH_CHUNK_SIZE = 1024 * 1024


def _sha256_file(path: Path) -> str:
    """Compute the SHA-256 hex digest of a file, reading in chunks."""
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        while True:
            chunk = handle.read(_HASH_CHUNK_SIZE)
            if not chunk:
                break
            digest.update(chunk)
    return digest.hexdigest()


def compute_tree_hash(files: list[tuple[str, bytes]]) -> str:
    """Compute the tree hash for a set of files.

    Args:
        files: list of (path, content_bytes) pairs.

    Returns:
        Hex-encoded SHA-256 tree hash.
    """
    if not files:
        raise ValueError("Cannot compute tree hash of empty file list")

    sorted_files = sorted(files, key=lambda f: f[0])

    if len(sorted_files) == 1:
        return hashlib.sha256(sorted_files[0][1]).hexdigest()

    # Multi-file: tree hash over sorted path:sha256 pairs
    canonical = "".join(
        f"{path}:{hashlib.sha256(content).hexdigest()}"
        for path, content in sorted_files
    )
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()


def compute_tree_hash_from_hashes(file_hashes: list[tuple[str, str]]) -> str:
    """Compute the tree hash from pre-computed per-file SHA-256 hex digests.

    Args:
        file_hashes: list of (relative_path, sha256_hex) pairs.

    Returns:
        Hex-encoded SHA-256 tree hash.
    """
    if not file_hashes:
        raise ValueError("Cannot compute tree hash of empty file list")

    sorted_hashes = sorted(file_hashes, key=lambda item: item[0])

    if len(sorted_hashes) == 1:
        return sorted_hashes[0][1]

    canonical = "".join(f"{p}:{h}" for p, h in sorted_hashes)
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()


def compute_tree_hash_from_paths(files: list[tuple[str, Path]]) -> tuple[str, int]:
    """Compute the tree hash for files on disk.

    Args:
        files: list of (relative_path, file_path) pairs.

    Returns:
        Tuple of (hex-encoded SHA-256 tree hash, total size in bytes).
    """
    if not files:
        raise ValueError("Cannot compute tree hash of empty file list")

    sorted_files = sorted(files, key=lambda item: item[0])
    total_size = 0
    file_hashes: list[tuple[str, str]] = []

    for rel_path, file_path in sorted_files:
        fhash = _sha256_file(file_path)
        total_size += file_path.stat().st_size
        file_hashes.append((rel_path, fhash))

    return compute_tree_hash_from_hashes(file_hashes), total_size
