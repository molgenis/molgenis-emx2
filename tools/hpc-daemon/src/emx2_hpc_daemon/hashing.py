"""Tree hash computation for artifact integrity verification.

Matches the server-side tree hash logic in ArtifactService.java exactly:
- Single file: SHA-256 of the file bytes
- Multiple files: SHA-256 of concatenated "path:sha256_hex" strings, sorted by path
"""

from __future__ import annotations

import hashlib


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
