"""Artifact upload logic extracted from HpcDaemon.

All functions are free functions receiving explicit dependencies
(client, config, tracked, etc.) rather than ``self``.
"""

from __future__ import annotations

import json
import logging
import mimetypes
from pathlib import Path

from .client import HpcClient, format_links
from .config import DaemonConfig
from .hashing import _sha256_file, compute_tree_hash_from_hashes
from .profiles import resolve_profile
from .tracker import TrackedJob

logger = logging.getLogger(__name__)


def _validate_upload_ack(
    response: dict,
    *,
    artifact_id: str,
    path: str,
    expected_sha256: str,
    expected_size: int,
) -> None:
    """Validate optional server acknowledgement fields for uploaded file metadata."""
    if not isinstance(response, dict):
        return
    actual_sha = response.get("sha256")
    if actual_sha is not None and str(actual_sha) != expected_sha256:
        raise ValueError(
            "Upload acknowledgement hash mismatch for "
            f"{artifact_id}/{path}: expected={expected_sha256} actual={actual_sha}"
        )
    actual_size = response.get("size_bytes")
    if actual_size is not None and int(actual_size) != expected_size:
        raise ValueError(
            "Upload acknowledgement size mismatch for "
            f"{artifact_id}/{path}: expected={expected_size} actual={actual_size}"
        )


def _validate_commit_ack(
    response: dict,
    *,
    artifact_id: str,
    expected_sha256: str,
    expected_size: int,
) -> None:
    """Validate optional server acknowledgement fields for artifact commit."""
    if not isinstance(response, dict):
        return
    status = response.get("status")
    if status is not None and status != "COMMITTED":
        raise ValueError(
            f"Artifact {artifact_id} commit acknowledgement returned unexpected status: {status}"
        )
    actual_sha = response.get("sha256")
    if actual_sha is not None and str(actual_sha) != expected_sha256:
        raise ValueError(
            "Commit acknowledgement hash mismatch for "
            f"{artifact_id}: expected={expected_sha256} actual={actual_sha}"
        )
    actual_size = response.get("size_bytes")
    if actual_size is not None and int(actual_size) != expected_size:
        raise ValueError(
            "Commit acknowledgement size mismatch for "
            f"{artifact_id}: expected={expected_size} actual={actual_size}"
        )


def classify_output_files(
    output_dir: str,
) -> tuple[list[Path], list[Path]]:
    """Classify files in output_dir into (output_files, log_files).

    Log files: slurm-*.out, slurm-*.err, container-stdout.log,
               container-stderr.log, and any other *.log files.
    Excluded from both: .hpc_progress.jsonl
    Output files: everything else.
    """
    output_path = Path(output_dir)
    output_files: list[Path] = []
    log_files: list[Path] = []

    for f in output_path.rglob("*"):
        if not f.is_file():
            continue
        if f.name == ".hpc_progress.jsonl":
            continue
        if f.name.startswith("slurm-") or f.name.endswith(".log"):
            log_files.append(f)
        else:
            output_files.append(f)

    return output_files, log_files


def resolve_residence(
    config: DaemonConfig,
    processor: str | None,
    profile: str | None,
    kind: str = "output",
) -> str:
    """Resolve artifact residence from profile config, defaulting to 'managed'.

    Args:
        kind: ``"output"`` or ``"log"`` -- selects which residence field to use.
    """
    residence = "managed"
    if processor:
        resolved = resolve_profile(config, processor, profile or "")
        if resolved:
            residence = (
                resolved.log_residence if kind == "log" else resolved.output_residence
            )
    return residence


def build_provenance_metadata(
    config: DaemonConfig, tracked: TrackedJob, artifact_type: str
) -> dict:
    """Build provenance metadata dict from tracked job state."""
    meta: dict = {
        "job_id": tracked.emx2_job_id,
        "processor": tracked.processor,
        "profile": tracked.profile,
        "worker_id": config.emx2.worker_id,
        "artifact_role": artifact_type,
    }
    if tracked.submit_user:
        meta["created_by"] = tracked.submit_user
    if tracked.input_artifact_ids:
        meta["input_artifact_ids"] = json.loads(tracked.input_artifact_ids)
    if tracked.parameters_hash:
        meta["parameters_hash"] = tracked.parameters_hash
    return meta


def upload_artifact(
    client: HpcClient,
    config: DaemonConfig,
    tracked: TrackedJob,
    files: list[Path],
    output_dir: str,
    residence: str,
    artifact_type: str = "blob",
    name_prefix: str = "output",
) -> str | None:
    """Upload files as an artifact. Returns artifact ID or None."""
    if not files:
        logger.debug(
            "No %s files to upload for job %s", name_prefix, tracked.emx2_job_id
        )
        return None

    logger.debug(
        "Artifact files for job %s (%s): %s",
        tracked.emx2_job_id,
        name_prefix,
        [
            f"{f.relative_to(Path(output_dir)).as_posix()} ({f.stat().st_size}b)"
            for f in files
        ],
    )

    metadata = build_provenance_metadata(config, tracked, name_prefix)

    try:
        if residence == "posix":
            return register_posix_artifact(
                client,
                tracked.emx2_job_id,
                output_dir,
                files,
                artifact_type,
                name_prefix,
                metadata=metadata,
            )
        else:
            return upload_managed_artifact(
                client,
                tracked.emx2_job_id,
                files,
                output_dir,
                artifact_type,
                name_prefix,
                metadata=metadata,
            )
    except Exception:
        logger.exception(
            "Failed to upload %s artifacts for job %s",
            name_prefix,
            tracked.emx2_job_id,
        )
        return None


def upload_output_artifacts(
    client: HpcClient,
    config: DaemonConfig,
    tracked: TrackedJob,
    output_dir: str,
    processor: str | None = None,
    profile: str | None = None,
) -> str | None:
    """Register output files as a new artifact. Returns artifact ID or None."""
    output_files, _ = classify_output_files(output_dir)
    residence = resolve_residence(config, processor, profile, kind="output")
    return upload_artifact(
        client, config, tracked, output_files, output_dir, residence, "blob", "output"
    )


def upload_log_artifact(
    client: HpcClient,
    config: DaemonConfig,
    tracked: TrackedJob,
    output_dir: str,
    processor: str | None = None,
    profile: str | None = None,
) -> str | None:
    """Upload log files as a separate log artifact. Returns artifact ID or None."""
    _, log_files = classify_output_files(output_dir)
    residence = resolve_residence(config, processor, profile, kind="log")
    return upload_artifact(
        client, config, tracked, log_files, output_dir, residence, "log", "log"
    )


def register_posix_artifact(
    client: HpcClient,
    job_id: str,
    output_dir: str,
    output_files: list[Path],
    artifact_type: str = "blob",
    name_prefix: str = "output",
    metadata: dict | None = None,
) -> str:
    """Register a posix artifact with file metadata only (no binary upload)."""
    content_url = f"file://{output_dir}"
    logger.debug(
        "Creating posix artifact for job %s: content_url=%s, files=%d",
        job_id,
        content_url,
        len(output_files),
    )
    artifact = client.create_artifact(
        artifact_type=artifact_type,
        residence="posix",
        metadata=metadata or {"job_id": job_id},
        content_url=content_url,
        name=f"{name_prefix}-{job_id[:8]}",
    )
    artifact_id = artifact["id"]
    logger.debug(
        "Created posix artifact %s, _links: %s",
        artifact_id,
        format_links(artifact.get("_links", {})),
    )

    # Register individual file metadata and collect hashes
    file_hashes: list[tuple[str, str]] = []
    total_size = 0
    output_root = Path(output_dir)
    for f in sorted(output_files, key=lambda p: p.relative_to(output_root).as_posix()):
        relative_path = f.relative_to(output_root).as_posix()
        fhash = _sha256_file(f)
        fsize = f.stat().st_size
        file_hashes.append((relative_path, fhash))
        total_size += fsize
        content_type = (
            mimetypes.guess_type(relative_path)[0] or "application/octet-stream"
        )
        register_result = client.register_artifact_file(
            artifact_id,
            path=relative_path,
            sha256=fhash,
            size_bytes=fsize,
            content_type=content_type,
        )
        _validate_upload_ack(
            register_result,
            artifact_id=artifact_id,
            path=relative_path,
            expected_sha256=fhash,
            expected_size=fsize,
        )

    tree_hash = compute_tree_hash_from_hashes(file_hashes)
    commit_result = client.commit_artifact(
        artifact_id,
        sha256=tree_hash,
        size_bytes=total_size,
    )
    _validate_commit_ack(
        commit_result,
        artifact_id=artifact_id,
        expected_sha256=tree_hash,
        expected_size=total_size,
    )
    logger.debug(
        "Committed posix artifact %s: sha256=%s, size=%d, _links: %s",
        artifact_id,
        tree_hash,
        total_size,
        format_links(commit_result.get("_links", {})),
    )
    logger.info(
        "Registered posix artifact %s (%d files) for job %s at %s",
        artifact_id,
        len(output_files),
        job_id,
        content_url,
    )
    return artifact_id


def upload_managed_artifact(
    client: HpcClient,
    job_id: str,
    output_files: list[Path],
    output_dir: str,
    artifact_type: str = "blob",
    name_prefix: str = "output",
    metadata: dict | None = None,
) -> str:
    """Upload output files as a managed artifact with binary content."""
    logger.debug(
        "Creating managed artifact for job %s (%d files)",
        job_id,
        len(output_files),
    )
    artifact = client.create_artifact(
        artifact_type=artifact_type,
        residence="managed",
        metadata=metadata or {"job_id": job_id},
        name=f"{name_prefix}-{job_id[:8]}",
    )
    artifact_id = artifact["id"]
    logger.debug(
        "Created managed artifact %s, _links: %s",
        artifact_id,
        format_links(artifact.get("_links", {})),
    )

    file_hashes: list[tuple[str, str]] = []
    total_size = 0
    output_root = Path(output_dir)
    for f in sorted(output_files, key=lambda p: p.relative_to(output_root).as_posix()):
        relative_path = f.relative_to(output_root).as_posix()
        file_hash = _sha256_file(f)
        file_size = f.stat().st_size
        file_hashes.append((relative_path, file_hash))
        total_size += file_size
        logger.debug(
            "Uploading file %s (%d bytes) to artifact %s",
            relative_path,
            file_size,
            artifact_id,
        )
        upload_result = client.upload_artifact_file(
            artifact_id,
            path=relative_path,
            file_path=str(f),
            size_bytes=file_size,
        )
        _validate_upload_ack(
            upload_result,
            artifact_id=artifact_id,
            path=relative_path,
            expected_sha256=file_hash,
            expected_size=file_size,
        )

    tree_hash = compute_tree_hash_from_hashes(file_hashes)
    commit_result = client.commit_artifact(
        artifact_id,
        sha256=tree_hash,
        size_bytes=total_size,
    )
    _validate_commit_ack(
        commit_result,
        artifact_id=artifact_id,
        expected_sha256=tree_hash,
        expected_size=total_size,
    )
    logger.debug(
        "Committed managed artifact %s: sha256=%s, size=%d, _links: %s",
        artifact_id,
        tree_hash,
        total_size,
        format_links(commit_result.get("_links", {})),
    )
    logger.info(
        "Uploaded %d output files as artifact %s for job %s",
        len(output_files),
        artifact_id,
        job_id,
    )
    return artifact_id
