"""Configuration loading from YAML with environment variable substitution.

Config file supports ${ENV_VAR} syntax and ``shared_secret_file`` for secrets.
Example:
    emx2:
      base_url: "https://emx2.example.org"
      shared_secret_file: /etc/emx2-hpc/secret
"""

from __future__ import annotations

import os
import re
from dataclasses import dataclass, field
from pathlib import Path

import yaml


def _substitute_env_vars(value: str) -> str:
    """Replace ${VAR} patterns with environment variable values."""
    return re.sub(
        r"\$\{([^}]+)\}",
        lambda m: os.environ.get(m.group(1), m.group(0)),
        value,
    )


def _walk_and_substitute(obj):
    """Recursively substitute env vars in all string values."""
    if isinstance(obj, str):
        return _substitute_env_vars(obj)
    if isinstance(obj, dict):
        return {k: _walk_and_substitute(v) for k, v in obj.items()}
    if isinstance(obj, list):
        return [_walk_and_substitute(item) for item in obj]
    return obj


@dataclass
class EmxConfig:
    base_url: str = "http://localhost:8080"
    worker_id: str = "hpc-daemon-01"
    shared_secret: str = ""
    shared_secret_file: str = ""
    auth_mode: str = "hmac"


@dataclass
class WorkerConfig:
    poll_interval_seconds: int = 30
    max_concurrent_jobs: int = 10
    queue_report_interval_seconds: int = 300  # report Slurm PENDING status every 5 min
    state_db: str = ""  # path to TinyDB state file; empty = ~/.local/share/hpc-daemon/state.json


@dataclass
class SlurmConfig:
    default_partition: str = "normal"
    default_account: str = ""


@dataclass
class ProfileEntry:
    """Maps a processor/profile key to Slurm + execution parameters.

    Execution mode is determined by which field is set:
    - ``sif_image``: run inside an Apptainer container
    - ``entrypoint``: exec a wrapper script with well-defined env vars

    At least one of ``sif_image`` or ``entrypoint`` must be set.
    """

    sif_image: str = ""
    entrypoint: str = ""
    partition: str = "normal"
    cpus: int = 4
    memory: str = "16G"
    time: str = "01:00:00"
    extra_args: list[str] = field(default_factory=list)
    output_residence: str = "managed"
    log_residence: str = "managed"
    claim_timeout_seconds: int = 300
    execution_timeout_seconds: int = 0  # 0 = use Slurm wall time only


@dataclass
class ApptainerConfig:
    bind_paths: list[str] = field(default_factory=list)
    tmp_dir: str = "/tmp/emx2-hpc"


@dataclass
class DaemonConfig:
    emx2: EmxConfig = field(default_factory=EmxConfig)
    worker: WorkerConfig = field(default_factory=WorkerConfig)
    slurm: SlurmConfig = field(default_factory=SlurmConfig)
    profiles: dict[str, ProfileEntry] = field(default_factory=dict)
    apptainer: ApptainerConfig = field(default_factory=ApptainerConfig)


def load_config(path: str | Path) -> DaemonConfig:
    """Load configuration from a YAML file with env var substitution."""
    with open(path) as f:
        raw = yaml.safe_load(f)

    if raw is None:
        return DaemonConfig()

    raw = _walk_and_substitute(raw)

    config = DaemonConfig()

    if "emx2" in raw:
        config.emx2 = EmxConfig(**{k: v for k, v in raw["emx2"].items()})

    # shared_secret_file takes priority over shared_secret
    # Resolve relative paths against the config file's directory
    if config.emx2.shared_secret_file:
        secret_path = Path(config.emx2.shared_secret_file)
        if not secret_path.is_absolute():
            secret_path = Path(path).parent / secret_path
        if not secret_path.is_file():
            raise FileNotFoundError(
                f"shared_secret_file not found: {secret_path}"
            )
        config.emx2.shared_secret = secret_path.read_text().strip()

    if "worker" in raw:
        config.worker = WorkerConfig(**raw["worker"])

    if "slurm" in raw:
        config.slurm = SlurmConfig(**raw["slurm"])

    if "profiles" in raw:
        for key, val in raw["profiles"].items():
            config.profiles[key] = ProfileEntry(**val)

    if "apptainer" in raw:
        config.apptainer = ApptainerConfig(**raw["apptainer"])

    return config
