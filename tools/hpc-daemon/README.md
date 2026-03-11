# EMX2 HPC Daemon

Outbound execution bridge that connects MOLGENIS EMX2 to HPC clusters managed by Slurm. The daemon runs on the HPC head node, polls EMX2 for pending jobs, submits them to Slurm inside Apptainer containers, monitors execution, and reports results (including output artifacts) back to EMX2.

All communication is outbound from HPC to EMX2 — no inbound connections into the cluster are needed.

## Install

Requires Python 3.11+. Install with [uv](https://docs.astral.sh/uv/):

```bash
uv pip install -e .
```

## Prerequisites

Before starting the daemon, configure these `_SYSTEM_` settings:

- `MOLGENIS_HPC_ENABLED=true`
- `MOLGENIS_HPC_CREDENTIALS_KEY=<strong-random-key>`

Worker authentication uses per-worker credentials. Issue or rotate a credential for the worker id,
then place the returned secret on the daemon host.

### Bootstrap worker credential (UI)

From zero-state (no worker rows yet):

1. Open `http://localhost:3000/workers` (or your HPC app URL).
2. In **Bootstrap Worker Credential**, enter the daemon `worker_id`.
3. Click **Issue** (or **Rotate** if re-keying an existing worker id).
4. Copy the shown secret into a local `.secret` file next to your daemon config:

```bash
printf '%s' '<paste-secret>' > .secret && chmod 600 .secret
```

5. Configure daemon with:

```yaml
emx2:
  worker_id: "hpc-headnode-01"
  worker_secret_file: ".secret"
```

Issuing the credential creates the worker identity row. Capability data and
heartbeat fields appear after the daemon successfully calls
`POST /api/hpc/workers/register`.

You can verify the setting is active by checking the health endpoint:

```bash
curl https://emx2.example.org/api/hpc/health
# Should return: {"status":"ok","hpc_enabled":true,"credentials_key_configured":true,...}
```

If `MOLGENIS_HPC_CREDENTIALS_KEY` is missing, credential issue/rotate endpoints return `503 Service Unavailable`.

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

Use `--backend=simulate` with `run` or `once` to walk jobs through all lifecycle states without invoking Slurm. Add `-v` for debug logging or `--json-logs` for structured output.

## Tests

```bash
uv pip install -e ".[dev]"
python -m pytest -v
```

## Design

See [apps/hpc/doc/design.md](../../apps/hpc/doc/design.md) for the full protocol specification.
