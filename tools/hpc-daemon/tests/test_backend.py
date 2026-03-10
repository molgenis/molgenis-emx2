"""Tests for backend input artifact normalization and staging."""

from __future__ import annotations

import hashlib
from pathlib import Path
from unittest.mock import MagicMock, call

import pytest

from emx2_hpc_daemon.backend import (
    _normalize_input_artifact_ids,
    _stage_input_artifacts,
)
from emx2_hpc_daemon.testkit import artifact_record


def _managed_artifact(artifact_id: str) -> dict:
    return artifact_record(
        artifact_id,
        residence="managed",
        status="COMMITTED",
    )


def _single_file_hash(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def test_normalize_input_artifact_ids_supports_named_reference_forms():
    inputs = [
        "art-1",
        {"artifact_id": "art-2"},
        {"dataset": "art-3"},
        {"dataset": "art-3"},  # duplicate should be deduplicated
        {"dataset": " ", "reference": "art-4"},
    ]
    result = _normalize_input_artifact_ids(inputs)
    assert result == ["art-1", "art-2", "art-3", "art-4"]


def test_normalize_input_artifact_ids_supports_top_level_mapping():
    inputs = {"dataset": "art-1", "reference": "art-2"}
    result = _normalize_input_artifact_ids(inputs)
    assert result == ["art-1", "art-2"]


def test_stage_input_artifacts_accepts_named_reference_object(tmp_path: Path):
    input_dir = tmp_path / "input"
    input_dir.mkdir()

    client = MagicMock()
    client.get_artifact.return_value = _managed_artifact("art-1")
    client.list_artifact_files.return_value = []
    client.download_artifact_files.return_value = []

    job = {"id": "job-1", "inputs": [{"dataset": "art-1"}]}
    _stage_input_artifacts(job, str(input_dir), client)

    client.get_artifact.assert_called_once_with("art-1")
    client.list_artifact_files.assert_called_once_with("art-1")
    client.download_artifact_files.assert_called_once_with(
        "art-1", str(input_dir / "art-1")
    )
    assert (input_dir / "art-1").is_dir()


def test_stage_input_artifacts_accepts_json_named_reference_mapping(tmp_path: Path):
    input_dir = tmp_path / "input"
    input_dir.mkdir()

    client = MagicMock()
    client.get_artifact.side_effect = lambda artifact_id: _managed_artifact(artifact_id)
    client.list_artifact_files.return_value = []
    client.download_artifact_files.return_value = []

    job = {"id": "job-2", "inputs": '{"dataset":"art-1","reference":"art-2"}'}
    _stage_input_artifacts(job, str(input_dir), client)

    assert client.get_artifact.call_args_list == [call("art-1"), call("art-2")]
    assert client.list_artifact_files.call_args_list == [call("art-1"), call("art-2")]
    assert client.download_artifact_files.call_args_list == [
        call("art-1", str(input_dir / "art-1")),
        call("art-2", str(input_dir / "art-2")),
    ]


def test_stage_input_artifacts_verifies_managed_hash(tmp_path: Path):
    input_dir = tmp_path / "input"
    input_dir.mkdir()

    content = b"hello managed hash"
    expected = _single_file_hash(content)

    client = MagicMock()
    client.get_artifact.return_value = {
        "id": "art-1",
        "residence": "managed",
        "status": "COMMITTED",
        "sha256": expected,
        "_links": {},
    }
    client.list_artifact_files.return_value = [{"path": "data.txt"}]

    def _download(_artifact_id: str, dest_dir: str) -> list[str]:
        path = Path(dest_dir) / "data.txt"
        path.write_bytes(content)
        return [str(path)]

    client.download_artifact_files.side_effect = _download

    _stage_input_artifacts({"id": "job-1", "inputs": ["art-1"]}, str(input_dir), client)
    assert (input_dir / "art-1" / "data.txt").read_bytes() == content


def test_stage_input_artifacts_rejects_managed_hash_mismatch(tmp_path: Path):
    input_dir = tmp_path / "input"
    input_dir.mkdir()

    client = MagicMock()
    client.get_artifact.return_value = {
        "id": "art-1",
        "residence": "managed",
        "status": "COMMITTED",
        "sha256": "0" * 64,
        "_links": {},
    }
    client.list_artifact_files.return_value = [{"path": "data.txt"}]

    def _download(_artifact_id: str, dest_dir: str) -> list[str]:
        path = Path(dest_dir) / "data.txt"
        path.write_text("actual content")
        return [str(path)]

    client.download_artifact_files.side_effect = _download

    with pytest.raises(ValueError, match="input_hash_mismatch"):
        _stage_input_artifacts({"id": "job-2", "inputs": ["art-1"]}, str(input_dir), client)


def test_stage_input_artifacts_rejects_posix_hash_mismatch(tmp_path: Path):
    input_dir = tmp_path / "input"
    input_dir.mkdir()
    source = tmp_path / "posix-src"
    source.mkdir()
    (source / "result.txt").write_text("posix content")

    client = MagicMock()
    client.get_artifact.return_value = {
        "id": "art-posix",
        "residence": "posix",
        "status": "COMMITTED",
        "content_url": f"file://{source}",
        "sha256": "f" * 64,
        "_links": {},
    }

    with pytest.raises(ValueError, match="input_hash_mismatch"):
        _stage_input_artifacts(
            {"id": "job-3", "inputs": [{"artifact_id": "art-posix"}]},
            str(input_dir),
            client,
        )
