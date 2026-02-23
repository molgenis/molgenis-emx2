"""Profile management: maps (processor, profile) keys to Slurm + Apptainer parameters.

Profiles are loaded from the daemon config and determine how jobs are submitted
to Slurm. They also derive the worker capabilities advertised during registration.
"""

from __future__ import annotations

from dataclasses import dataclass

from .config import DaemonConfig


@dataclass
class ResolvedProfile:
    """A fully resolved profile ready for batch script generation."""

    processor: str
    profile: str
    sif_image: str
    partition: str
    cpus: int
    memory: str
    time: str
    extra_args: list[str]
    artifact_residence: str


def resolve_profile(
    config: DaemonConfig, processor: str, profile: str
) -> ResolvedProfile | None:
    """
    Resolve a (processor, profile) pair to Slurm parameters.

    Looks up "{processor}:{profile}" in config.profiles, falls back to
    "{processor}" without profile qualifier.

    Returns None if no matching profile is found.
    """
    # Try exact match first
    key = f"{processor}:{profile}" if profile else processor
    entry = config.profiles.get(key)

    # Try processor-only fallback
    if entry is None and profile:
        entry = config.profiles.get(processor)

    if entry is None:
        return None

    return ResolvedProfile(
        processor=processor,
        profile=profile or "default",
        sif_image=entry.sif_image,
        partition=entry.partition or config.slurm.default_partition,
        cpus=entry.cpus,
        memory=entry.memory,
        time=entry.time,
        extra_args=entry.extra_args or [],
        artifact_residence=entry.artifact_residence,
    )


def derive_capabilities(config: DaemonConfig) -> list[dict]:
    """
    Derive worker capabilities from configured profiles.

    Returns a list of capability dicts suitable for the worker registration API:
    [{"processor": "...", "profile": "...", "max_concurrent_jobs": N}, ...]
    """
    capabilities = []
    for key in config.profiles:
        parts = key.split(":", 1)
        processor = parts[0]
        profile = parts[1] if len(parts) > 1 else "default"
        capabilities.append(
            {
                "processor": processor,
                "profile": profile,
                "max_concurrent_jobs": config.worker.max_concurrent_jobs,
            }
        )
    return capabilities
