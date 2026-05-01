# EMX2 HPC Daemon

Outbound execution bridge that connects MOLGENIS EMX2 to HPC clusters managed by Slurm. The daemon runs on the HPC head node, polls EMX2 for pending jobs, submits them to Slurm inside Apptainer containers, monitors execution, and reports results (including output artifacts) back to EMX2.

All communication is outbound from HPC to EMX2 — no inbound connections into the cluster are needed.

## Read First

For the full setup flow (EMX2 settings -> worker credential -> daemon -> first
simulated job -> real Slurm/Apptainer run),
use the canonical quick start:

- [apps/hpc/README.md](../../apps/hpc/README.md)

This daemon README is daemon-specific reference documentation.

## Install

Requires Python 3.11+. Install with [uv](https://docs.astral.sh/uv/):

```bash
uv pip install -e .
```

## Prerequisites

Before starting the daemon:

1. EMX2 HPC must be enabled (`MOLGENIS_HPC_ENABLED=true`).
2. EMX2 worker credential encryption key must be configured (`MOLGENIS_HPC_CREDENTIALS_KEY`).
3. A worker credential secret must be issued for your `worker_id` and stored on the daemon host (for example in `.secret`).

If you need exact bootstrap steps, use [apps/hpc/README.md](../../apps/hpc/README.md).

## Configuration

The daemon reads a YAML config file. Environment variables can be referenced with `${VAR}` syntax.
Secrets should use `worker_secret_file` to read from a file with restricted permissions.

```yaml
emx2:
  base_url: "https://emx2.example.org"
  worker_id: "hpc-headnode-01"
  worker_secret_file: /etc/emx2-hpc/secret  # chmod 600, owned by service user
  auth_mode: "hmac"  # or "token"

worker:
  poll_interval_seconds: 30
  max_concurrent_jobs: 10

profiles:
  text-embedding:v3/gpu-medium:
    sif_image: /nfs/images/text-embedding_v3.sif
    partition: gpu
    cpus: 8
    memory: 64G
    time: "04:00:00"
    output_residence: posix   # or "managed" (default)
    log_residence: managed    # or "posix"

apptainer:
  bind_paths:
    - /nfs/data:/data
  tmp_dir: /tmp/emx2-hpc
```

Each entry under `profiles` maps a `processor/profile` key to Slurm resource parameters and an Apptainer SIF image.

## Commands

```bash
# Validate config, check Slurm/Apptainer availability, test EMX2 connectivity
emx2-hpc-daemon -c config.yaml check

# Start the daemon loop (register, poll, claim, submit, monitor)
emx2-hpc-daemon -c config.yaml run

# Run a single poll-claim-monitor cycle, then exit (useful for cron)
emx2-hpc-daemon -c config.yaml once

# Register the worker with EMX2 and exit
emx2-hpc-daemon -c config.yaml register
```

Use `--backend=simulate` with `run` or `once` to walk jobs through all
lifecycle states without invoking Slurm. Add `-v` for debug logging or
`--json-logs` for structured output.

For an end-to-end copy/paste example, including the first simulated job
submission and the real Slurm/Apptainer VM harness, use
[apps/hpc/README.md](../../apps/hpc/README.md).

## Tests

```bash
uv pip install -e ".[dev]"
python -m pytest -v
```

## Design

See [docs/hpc/design.md](../../docs/hpc/design.md) for the full protocol specification.
