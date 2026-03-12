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
    entrypoint: str
    partition: str
    cpus: int
    memory: str
    time: str
    sbatch_args: list[str]
    output_residence: str
    log_residence: str
    submission_timeout_seconds: int = 300


def resolve_profile(
    config: DaemonConfig, processor: str, profile: str
) -> ResolvedProfile | None:
    """
    Resolve a (processor, profile) pair to Slurm parameters.

    Looks up "{processor}:{profile}" in config.profiles.

    Returns None if no matching profile is found.
    """
    profile_key = profile or "default"
    key = f"{processor}:{profile_key}"
    entry = config.profiles.get(key)

    if entry is None:
        return None

    return ResolvedProfile(
        processor=processor,
        profile=profile_key,
        sif_image=entry.sif_image,
        entrypoint=entry.entrypoint,
        partition=entry.partition or config.slurm.default_partition,
        cpus=entry.cpus,
        memory=entry.memory,
        time=entry.time,
        sbatch_args=entry.sbatch_args or [],
        output_residence=entry.output_residence,
        log_residence=entry.log_residence,
        submission_timeout_seconds=entry.submission_timeout_seconds,
    )


def derive_capabilities(config: DaemonConfig) -> list[dict]:
    """
    Derive worker capabilities from configured profiles.

    Returns a list of capability dicts suitable for the worker registration API:
    [{"processor": "...", "profile": "...", "max_concurrent_jobs": N}, ...]
    """
    capabilities = []
    for key in config.profiles:
        processor, profile = key.split(":", 1)
        capabilities.append(
            {
                "processor": processor,
                "profile": profile,
                "max_concurrent_jobs": config.worker.max_concurrent_jobs,
            }
        )
    return capabilities
