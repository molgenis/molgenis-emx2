"""Focused tests for artifact upload/commit acknowledgement validation."""

from __future__ import annotations

import hashlib
from pathlib import Path
from unittest.mock import MagicMock

import pytest

from emx2_hpc_daemon.daemon_artifacts import upload_managed_artifact


def _write_file(path: Path, content: bytes) -> Path:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(content)
    return path


def test_upload_managed_artifact_rejects_mismatched_file_ack(tmp_path: Path):
    root = tmp_path / "output"
    payload = b"hello transfer\n"
    file_path = _write_file(root / "nested" / "result.txt", payload)

    expected_sha = hashlib.sha256(payload).hexdigest()
    expected_size = len(payload)

    client = MagicMock()
    client.create_artifact.return_value = {"id": "art-1", "_links": {}}
    client.upload_artifact_file.return_value = {
        "sha256": "0" * 64,
        "size_bytes": expected_size,
    }

    with pytest.raises(ValueError, match="Upload acknowledgement hash mismatch"):
        upload_managed_artifact(
            client,
            job_id="job-1",
            output_files=[file_path],
            output_dir=str(root),
        )

    client.commit_artifact.assert_not_called()
    client.upload_artifact_file.assert_called_once()
    uploaded_kwargs = client.upload_artifact_file.call_args.kwargs
    assert uploaded_kwargs["path"] == "nested/result.txt"
    assert expected_sha != "0" * 64


def test_upload_managed_artifact_rejects_mismatched_commit_ack(tmp_path: Path):
    root = tmp_path / "output"
    payload = b"commit check\n"
    file_path = _write_file(root / "result.txt", payload)

    expected_sha = hashlib.sha256(payload).hexdigest()
    expected_size = len(payload)

    client = MagicMock()
    client.create_artifact.return_value = {"id": "art-2", "_links": {}}
    client.upload_artifact_file.return_value = {
        "sha256": expected_sha,
        "size_bytes": expected_size,
    }
    client.commit_artifact.return_value = {
        "status": "COMMITTED",
        "sha256": "f" * 64,
        "size_bytes": expected_size,
    }

    with pytest.raises(ValueError, match="Commit acknowledgement hash mismatch"):
        upload_managed_artifact(
            client,
            job_id="job-2",
            output_files=[file_path],
            output_dir=str(root),
        )
