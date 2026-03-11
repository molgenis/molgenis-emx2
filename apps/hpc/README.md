# EMX2 HPC Bridge Quick Start

This is the canonical onboarding guide for the EMX2 HPC bridge.

It covers the operational flow end-to-end:
1. Configure EMX2 HPC settings.
2. Issue a worker credential.
3. Start the daemon.
4. Submit and observe a job.

Use this README first. Other docs are reference material.

## What You Get

The HPC app provides three operational views:
- **Jobs**: submit jobs, inspect lifecycle transitions, and review outputs.
- **Workers**: monitor worker heartbeat/capabilities and manage credentials.
- **Artifacts**: upload/manage input/output data files.

## Prerequisites

1. EMX2 is running (default local URL: `http://localhost:8080`).
2. You can sign in as an EMX2 admin user.
3. Python + `uv` are available for running the daemon.
4. This app can reach EMX2:
   - local app dev typically runs on `http://localhost:3000`.
   - API calls proxy to EMX2 (`http://localhost:8080` by default).

## 1) Enable HPC in EMX2

HPC requires two `_SYSTEM_` settings:
- `MOLGENIS_HPC_ENABLED=true`
- `MOLGENIS_HPC_CREDENTIALS_KEY=<strong-random-key>`

You can set these in your admin settings UI, or with GraphQL:

```bash
EMX2_BASE_URL="http://localhost:8080"
EMX2_ADMIN_EMAIL="admin"
EMX2_ADMIN_PASSWORD="admin"
HPC_KEY="$(openssl rand -hex 32)"

# Sign in and store cookie
curl -sS -c /tmp/emx2.cookies \
  -H "Content-Type: application/json" \
  -d "{\"query\":\"mutation{signin(email:\\\"${EMX2_ADMIN_EMAIL}\\\",password:\\\"${EMX2_ADMIN_PASSWORD}\\\"){status message}}\"}" \
  "${EMX2_BASE_URL}/api/graphql"

# Set HPC settings
curl -sS -b /tmp/emx2.cookies \
  -H "Content-Type: application/json" \
  -d "{\"query\":\"mutation change(\$settings:[MolgenisSettingsInput]){change(settings:\$settings){status message}}\",\"variables\":{\"settings\":[{\"key\":\"MOLGENIS_HPC_ENABLED\",\"value\":\"true\"},{\"key\":\"MOLGENIS_HPC_CREDENTIALS_KEY\",\"value\":\"${HPC_KEY}\"}]}}" \
  "${EMX2_BASE_URL}/api/graphql"
```

Verify:

```bash
curl -sS "${EMX2_BASE_URL}/api/hpc/health"
```

Expected: `hpc_enabled=true` and `credentials_key_configured=true`.

## 2) Start the HPC App (UI)

From this directory:

```bash
pnpm install
pnpm dev
```

Open the app and sign in (usually `http://localhost:3000`).

## 3) Add Worker Credential (UI)

In **Workers**:
1. Click **+ Add Worker**.
2. Enter your daemon `worker_id`.
3. Click **Issue** (first credential) or **Rotate** (replace existing active credential).
4. Copy the returned secret (shown once).

Write it to a local file next to daemon config:

```bash
printf '%s' '<paste-secret>' > .secret && chmod 600 .secret
```

Behavior summary:
- **Issue**: creates the first active credential; returns `409` if one is already active.
- **Rotate**: revokes existing active credential immediately and creates a new one.
- **Revoke**: invalidates that credential immediately.
- **Delete worker**: removes worker row, capabilities, and credentials.

## 4) Start the Daemon

From repo root:

```bash
cd tools/hpc-daemon
uv pip install -e .
```

Use `demo-config.yaml` as a starting point:

```yaml
emx2:
  base_url: "http://localhost:8080"
  worker_id: "demo-worker-01"
  worker_secret_file: ".secret"
```

Run checks:

```bash
uv run emx2-hpc-daemon -c demo-config.yaml check
```

Run daemon loop with simulated backend:

```bash
uv run emx2-hpc-daemon -c demo-config.yaml run --backend simulate -v
```

For real Slurm execution, use `--backend slurm` and valid `profiles` mappings.

## 5) Submit and Verify a Job

In **Jobs**:
1. Submit a job with a processor/profile your worker advertises.
2. Observe transitions (for example `PENDING -> CLAIMED -> SUBMITTED -> STARTED -> COMPLETED`).
3. Open job details and verify output/log artifact links.

In **Workers**:
1. Confirm heartbeat updates.
2. Confirm capabilities shown are the worker-advertised set.

## Troubleshooting

- `503` on HPC endpoints:
  - HPC not enabled or credentials key missing.
  - check `/api/hpc/health`.
- `401` from worker calls:
  - secret mismatch, revoked, expired, wrong worker id, or timestamp skew.
- `409` on credential issue:
  - worker already has active credential; use **Rotate**.
- worker disappears:
  - stale-worker expiry after heartbeat timeout.
  - daemon stopped, blocked, or clock drift broke auth.

## Doc Map

- Protocol/API spec: [doc/design.md](./doc/design.md)
- Daemon install/config reference: [../../tools/hpc-daemon/README.md](../../tools/hpc-daemon/README.md)
- Real Slurm VM e2e harness: [../../tools/hpc-daemon/e2e/README.md](../../tools/hpc-daemon/e2e/README.md)
