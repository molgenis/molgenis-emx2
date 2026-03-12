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
3. Click **Issue** to create the first credential.
4. Copy the returned secret (shown once).

Write it to a local file next to daemon config:

```bash
printf '%s' '<paste-secret>' > .secret && chmod 600 .secret
```

Behavior summary:
- **Issue**: creates the first active credential; returns `409` if one is already active.
- **Rotate**: use **Manage** on an existing worker; revokes the active credential immediately and creates a new one.
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

## 5) First Simulated Job (Copy/Paste Path)

This is the fastest full proof that the bridge is wired correctly.

### 5.1 Submit a job

The demo config advertises one capability:
- `processor=test`
- `profile=test`

You can submit it from the UI, or directly through the API using the same
admin cookie from step 1:

```bash
curl -sS -b /tmp/emx2.cookies \
  -H "Content-Type: application/json" \
  -H "X-EMX2-API-Version: 2025-01" \
  -d '{"processor":"test","profile":"test"}' \
  "${EMX2_BASE_URL}/api/hpc/jobs"
```

Expected response shape:

```json
{
  "id": "<job-id>",
  "status": "PENDING",
  "processor": "test",
  "profile": "test"
}
```

### 5.2 Watch the lifecycle

With the simulated daemon loop running, the job should move through:

`PENDING -> CLAIMED -> SUBMITTED -> STARTED -> COMPLETED`

From the shell:

```bash
JOB_ID="<paste-job-id>"

watch -n 2 "curl -sS -b /tmp/emx2.cookies \
  \"${EMX2_BASE_URL}/api/hpc/jobs/${JOB_ID}\""
```

### 5.3 Verify outputs

Once the job is `COMPLETED`, verify:
1. `slurm_job_id` is present
2. output artifact is linked
3. log artifact is linked
4. transition history shows the full lifecycle

This simulated path proves:
- EMX2 settings are correct
- worker auth works
- daemon polling/claim/report works
- the HPC app renders jobs, workers, and artifacts correctly

## 6) First Real Slurm / Apptainer Run

The repo includes a real-Slurm VM harness that provisions:
- Slurm controller + node
- accounting database
- daemon
- real Apptainer execution

Run it with:

```bash
cd tools/hpc-daemon/e2e
make e2e
```

That suite is the system-truth layer for:
- entrypoint-based Slurm execution
- Apptainer-backed execution
- managed and posix artifacts
- transform roundtrips
- nested outputs
- cancellation and failure handling

## 7) Submit and Verify a Job in the UI

In **Jobs**:
1. Submit a job with a processor/profile your worker advertises.
2. Observe transitions (for example `PENDING -> CLAIMED -> SUBMITTED -> STARTED -> COMPLETED`).
3. Open job details and verify output/log artifact links.

In **Workers**:
1. Confirm heartbeat updates.
2. Confirm capabilities shown are the worker-advertised set.

## Protocol Contract and Generated Types

The HPC bridge has a protocol contract with one source of truth:

- `protocol/hpc-protocol.json`

This JSON Schema file defines shared protocol constants and contracts across Java, Python, and Vue:
- API version
- job/artifact status enums
- allowed job transitions
- required/optional headers
- problem detail shape
- HATEOAS link expectations
- cross-language HMAC vectors

Generated artifacts from that schema:
- `tools/hpc-daemon/src/emx2_hpc_daemon/_generated.py`
- `apps/hpc/app/utils/protocol.ts`

Regenerate both from repo root:

```bash
uv run python protocol/generate.py
```

Rule: edit `protocol/hpc-protocol.json` first, regenerate, then run contract tests. Do not hand-edit generated files.

Recommended drift checks:

```bash
./gradlew :backend:molgenis-emx2-hpc:test --tests org.molgenis.emx2.hpc.protocol.HpcApiContractTest
uv run pytest tools/hpc-daemon/tests/test_contract.py -q
./gradlew :backend:molgenis-emx2-webapi:test --tests org.molgenis.emx2.web.hpc.HpcApiProtocolContractE2ETest
```

## Troubleshooting

- `503` on HPC endpoints:
  - HPC not enabled or credentials key missing.
  - check `/api/hpc/health`.
- `401` from worker calls:
  - secret mismatch, revoked, expired, wrong worker id, or timestamp skew.
- `409` on credential issue:
  - worker already has active credential; use **Rotate**.
- worker disappears:
  - the worker was explicitly removed (UI/API `DELETE /api/hpc/workers/{id}`).
  - daemon stopped, blocked, or clock drift broke auth and the row is stale in UI (not deleted).

## Doc Map

- Protocol/API spec: [doc/design.md](./doc/design.md)
- Protocol contract schema: [../../protocol/hpc-protocol.json](../../protocol/hpc-protocol.json)
- Daemon install/config reference: [../../tools/hpc-daemon/README.md](../../tools/hpc-daemon/README.md)
- Real Slurm VM e2e harness: [../../tools/hpc-daemon/e2e/README.md](../../tools/hpc-daemon/e2e/README.md)
