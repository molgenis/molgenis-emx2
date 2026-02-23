# EMX2 HPC Daemon

Outbound execution bridge that connects MOLGENIS EMX2 to HPC clusters managed by Slurm. The daemon runs on the HPC head node, polls EMX2 for pending jobs, submits them to Slurm inside Apptainer containers, monitors execution, and reports results (including output artifacts) back to EMX2.

All communication is outbound from HPC to EMX2 — no inbound connections into the cluster are needed.

## Install

Requires Python 3.11+. Install with [uv](https://docs.astral.sh/uv/):

```bash
uv pip install -e .
```

## Configuration

The daemon reads a YAML config file. Environment variables can be referenced with `${VAR}` syntax.

```yaml
emx2:
  base_url: "https://emx2.example.org"
  worker_id: "hpc-headnode-01"
  shared_secret: "${EMX2_HPC_SECRET}"
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
    artifact_residence: posix  # or "managed"

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

Add `--simulate` to `run` or `once` to walk jobs through all lifecycle states without invoking Slurm. Add `-v` for debug logging or `--json-logs` for structured output.

## Tests

```bash
uv pip install -e ".[dev]"
python -m pytest -v
```

## Design

See [apps/hpc/doc/design.md](../../apps/hpc/doc/design.md) for the full protocol specification.
