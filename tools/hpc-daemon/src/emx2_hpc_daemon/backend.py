"""Execution backend strategy: abstracts Slurm, shell, and simulated execution.

The daemon delegates all execution operations to an ExecutionBackend,
keeping a single code path regardless of execution mode.
"""

from __future__ import annotations

import json
import logging
import shutil
from abc import ABC, abstractmethod
from dataclasses import dataclass
from pathlib import Path

from .client import HpcClient, format_links
from .hashing import compute_tree_hash_from_paths
from .slurm import SlurmJobInfo

logger = logging.getLogger(__name__)


# Map Slurm states to EMX2 HPC job statuses
SLURM_TO_HPC_STATUS = {
    "PENDING": None,  # Still waiting in Slurm queue, no transition needed
    "RUNNING": "STARTED",
    "COMPLETED": "COMPLETED",
    "FAILED": "FAILED",
    "CANCELLED": "CANCELLED",
    "TIMEOUT": "FAILED",
    "OUT_OF_MEMORY": "FAILED",
    "NODE_FAIL": "FAILED",
    "PREEMPTED": "FAILED",
    "BOOT_FAIL": "FAILED",
    "DEADLINE": "FAILED",
    "REVOKED": "FAILED",
    "SPECIAL_EXIT": "FAILED",
}


def _verify_artifact_hash(artifact: dict, artifact_dir: Path, artifact_id: str) -> None:
    """Verify SHA-256 of staged input artifact files against expected hash.

    Raises ValueError("input_hash_mismatch") on mismatch.
    """
    expected_hash = artifact.get("sha256")
    if not expected_hash:
        logger.warning(
            "Artifact %s has no sha256 — skipping hash verification",
            artifact_id,
        )
        return

    # Collect all files in the artifact directory
    if not artifact_dir.is_dir():
        # Single file — check if it's a file directly
        if artifact_dir.is_file():
            actual_hash, _ = compute_tree_hash_from_paths(
                [(artifact_dir.name, artifact_dir)]
            )
        else:
            raise ValueError(
                f"input_hash_mismatch: artifact {artifact_id} "
                f"staged path missing at {artifact_dir}"
            )
    else:
        file_list = sorted(f for f in artifact_dir.rglob("*") if f.is_file())
        if not file_list:
            raise ValueError(
                f"input_hash_mismatch: artifact {artifact_id} "
                f"staged path {artifact_dir} contains no files"
            )

        # Build (relative_path, file_path) pairs
        pairs: list[tuple[str, Path]] = []
        for f in file_list:
            rel_path = str(f.relative_to(artifact_dir))
            pairs.append((rel_path, f))
        actual_hash, _ = compute_tree_hash_from_paths(pairs)

    if actual_hash != expected_hash:
        raise ValueError(
            f"input_hash_mismatch: artifact {artifact_id} "
            f"expected={expected_hash} actual={actual_hash}"
        )
    logger.debug("Hash verified for artifact %s: %s", artifact_id, actual_hash)


@dataclass
class SubmitResult:
    """Result of submitting a job to an execution backend."""

    slurm_job_id: str
    work_dir: str | None = None
    input_dir: str | None = None
    output_dir: str | None = None


@dataclass
class StatusResult:
    """Result of a status query: the new HPC status plus Slurm metadata."""

    hpc_status: str  # e.g. "STARTED", "COMPLETED", "FAILED"
    slurm_info: SlurmJobInfo  # full Slurm metadata for detail strings


class ExecutionBackend(ABC):
    """Strategy interface for job execution."""

    @abstractmethod
    def submit(self, job: dict, client: HpcClient) -> SubmitResult:
        """Submit a job for execution. Returns tracking info.

        Raises ValueError if the job cannot be submitted (e.g. no matching profile).
        """

    @abstractmethod
    def query_status(
        self, slurm_job_id: str, current_status: str
    ) -> StatusResult | None:
        """Query the current status of a submitted job.

        Returns a StatusResult with the new HPC status and Slurm metadata,
        or None if the status hasn't changed.
        """

    def query_slurm_info(self, slurm_job_id: str) -> SlurmJobInfo | None:
        """Query raw Slurm info without status mapping. Override for real Slurm."""
        return None

    @abstractmethod
    def cancel(self, slurm_job_id: str) -> None:
        """Cancel a running job."""


def _stage_input_artifacts(job: dict, input_dir: str, client: HpcClient) -> None:
    """Download or symlink input artifacts to the job's input directory.

    For posix artifacts with file:// content_url, creates symlinks.
    For managed artifacts, downloads via GET.
    """
    inputs = job.get("inputs")
    if not inputs:
        logger.debug("Job %s has no input artifacts", job.get("id", "?"))
        return

    if isinstance(inputs, str):
        try:
            inputs = json.loads(inputs)
        except (json.JSONDecodeError, TypeError):
            logger.warning("Could not parse job inputs: %s", inputs)
            return

    logger.debug(
        "Staging %d input artifact(s) for job %s: %s",
        len(inputs) if isinstance(inputs, list) else 0,
        job.get("id", "?"),
        inputs,
    )

    artifact_ids = _normalize_input_artifact_ids(inputs)
    logger.debug(
        "Normalized input artifacts for job %s: %s",
        job.get("id", "?"),
        artifact_ids,
    )
    for artifact_id in artifact_ids:
        try:
            artifact = client.get_artifact(artifact_id)
            residence = artifact.get("residence", "managed")
            content_url = artifact.get("content_url")
            logger.debug(
                "Input artifact %s: type=%s, format=%s, "
                "residence=%s, status=%s, sha256=%s, "
                "size_bytes=%s, content_url=%s",
                artifact_id,
                artifact.get("type"),
                artifact.get("format"),
                residence,
                artifact.get("status"),
                artifact.get("sha256"),
                artifact.get("size_bytes"),
                content_url,
            )

            # Log artifact's HATEOAS links
            links = artifact.get("_links", {})
            if links:
                logger.debug(
                    "Input artifact %s _links: %s",
                    artifact_id,
                    format_links(links),
                )

            if residence == "posix" and content_url and content_url.startswith("file://"):
                # Symlink posix artifact directory
                posix_path = content_url[len("file://") :]
                link_path = Path(input_dir) / artifact_id
                link_path.symlink_to(posix_path)
                logger.info(
                    "Symlinked posix artifact %s: %s -> %s",
                    artifact_id,
                    link_path,
                    posix_path,
                )
                # Verify hash for posix artifacts
                _verify_artifact_hash(
                    artifact, Path(posix_path), artifact_id
                )
            else:
                # Download managed artifact files
                artifact_dir = Path(input_dir) / artifact_id
                if artifact_dir.exists():
                    if artifact_dir.is_dir() and not artifact_dir.is_symlink():
                        shutil.rmtree(artifact_dir)
                    else:
                        artifact_dir.unlink()
                artifact_dir.mkdir(parents=True, exist_ok=True)
                files = client.list_artifact_files(artifact_id)
                logger.debug(
                    "Artifact %s has %d file(s): %s",
                    artifact_id,
                    len(files),
                    [f"{f.get('path')} ({f.get('size_bytes', '?')}b)" for f in files],
                )
                downloaded = client.download_artifact_files(
                    artifact_id, str(artifact_dir)
                )
                logger.info(
                    "Staged %d files from artifact %s",
                    len(downloaded),
                    artifact_id,
                )
                # Verify hash for managed artifacts
                _verify_artifact_hash(artifact, artifact_dir, artifact_id)
        except Exception:
            logger.exception("Failed to stage artifact %s", artifact_id)
            raise


def _normalize_input_artifact_ids(inputs: object) -> list[str]:
    """Normalize supported job input formats into unique artifact IDs.

    Supported forms:
    - ["id-1", "id-2"]
    - [{"artifact_id": "id-1"}]
    - [{"dataset": "id-1"}]
    - {"dataset": "id-1", "reference": "id-2"}
    """
    items: list[object]
    if isinstance(inputs, list):
        items = inputs
    elif isinstance(inputs, dict):
        items = [inputs]
    else:
        return []

    ids: list[str] = []
    seen: set[str] = set()

    def add_if_new(candidate: object) -> None:
        if isinstance(candidate, str):
            artifact_id = candidate.strip()
            if artifact_id and artifact_id not in seen:
                seen.add(artifact_id)
                ids.append(artifact_id)

    for item in items:
        if isinstance(item, str):
            add_if_new(item)
            continue
        if not isinstance(item, dict):
            continue

        artifact_id = item.get("artifact_id")
        if isinstance(artifact_id, str) and artifact_id.strip():
            add_if_new(artifact_id)
            continue

        # Convenience named-reference forms, e.g. {"dataset": "id-1"}.
        for value in item.values():
            add_if_new(value)

    return ids


# Re-export backend implementations so existing imports still work:
#   from emx2_hpc_daemon.backend import SlurmBackend, ShellBackend, SimulatedBackend
from .backend_slurm import SlurmBackend as SlurmBackend  # noqa: E402, F401
from .backend_shell import ShellBackend as ShellBackend  # noqa: E402, F401
from .backend_simulated import SimulatedBackend as SimulatedBackend  # noqa: E402, F401

__all__ = [
    "ExecutionBackend",
    "SubmitResult",
    "StatusResult",
    "SlurmBackend",
    "ShellBackend",
    "SimulatedBackend",
    "SLURM_TO_HPC_STATUS",
    "_stage_input_artifacts",
    "_normalize_input_artifact_ids",
    "_verify_artifact_hash",
]
