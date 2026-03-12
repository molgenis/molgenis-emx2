---
title: "MOLGENIS EMX2 & HPC Job Orchestration — Design Specification"
lang: en-US

mainfont: "IBM Plex Sans"
monofont: "PragmataPro Liga"
mathfont: "Latin Modern Math"

linestretch: 1.15
geometry: margin=1in

hyphenpenalty: 10000
exhyphenpenalty: 10000
emergencystretch: 2em

toc: true
toc-depth: 3
numbersections: true

header-includes: |
  \usepackage{float}
  \floatplacement{figure}{!ht}
  \renewcommand{\topfraction}{0.9}
  \renewcommand{\bottomfraction}{0.9}
  \renewcommand{\textfraction}{0.1}
  \renewcommand{\floatpagefraction}{0.8}
  \usepackage{tikz}
  \usetikzlibrary{arrows.meta, positioning}
---

# Abstract

EMX2 is evolving to support heavy compute workloads (including AI) that cannot run inside the application stack and cannot be triggered via inbound connections into available HPC clusters. At the same time, EMX2 must remain the authoritative system of record for job state and artifact metadata, while HPC governance must retain control over scheduling and resource allocation. This creates a coordination problem across a strict trust boundary.

This document specifies an outbound-only execution bridge: the HPC head node polls EMX2 for work, claims jobs atomically, submits them to Slurm, and reports lifecycle transitions and artifacts back to EMX2. EMX2 owns state and integrity; HPC owns execution and scheduling. The result is deterministic, auditable job orchestration without breaking institutional network constraints or governance boundaries.

# Introduction

## Motivation

Molgenis EMX2 is a metadata-driven platform for scientific data built around FAIR principles (findability, accessibility, interoperability and reusability). As the platform evolves to incorporate AI-backed enhancements — automated annotation, similarity search, inference pipelines — it needs the ability to offload compute-intensive workloads to GPU-enabled infrastructure that typically lives outside the application's own network.

This document specifies a protocol for bridging EMX2 with one or more HPC clusters managed by Slurm. The design addresses a specific institutional constraint: the HPC environment cannot accept inbound connections. All communication MUST be initiated from the HPC side.

The architecture is an outbound-only job execution bridge. HPC workers poll EMX2 for work, claim jobs, execute them (inside Apptainer containers or via wrapper scripts), and report results — all without EMX2 needing to reach into the cluster.

## Scope

This specification covers worker registration and capability advertisement, job lifecycle management, artifact management (typed, content-addressed data objects for job inputs and outputs), and authentication plus authorization across the trust boundary.

It does not cover job creation by end users (that is an EMX2 application concern), Slurm cluster administration, or artifact retention policy.

## Terminology

| Term           | Meaning                                                                                     |
| -------------- | ------------------------------------------------------------------------------------------- |
| **Worker**     | A head node controller that registers with EMX2, polls for jobs, and submits them to Slurm. |
| **Processor**  | A logical identifier for a type of workload (e.g. `text-embedding:v3`).                     |
| **Profile**    | An abstract resource tier (e.g. `gpu-medium`) mapped to Slurm parameters on the HPC side.   |
| **Artifact**   | A typed, content-addressed data object tracked by the artifact registry.                    |
| **Transition** | A recorded state change on a job, created as a sub-resource of that job.                    |

# System Architecture

The system is divided into two trust domains connected by outbound HTTPS from the HPC environment. This section describes the components in each domain and how they interact.

![HPC Topology](./flow.pdf)

## EMX2 Application Domain

The system introduces three API surfaces within EMX2:

- **Workers API** — registration of head nodes and their capabilities.
- **Jobs API** — job listing, filtering by capability, atomic claiming, state transitions, and output artifact linking.
- **Artifact API** — lifecycle management (create/upload/commit), path-based file operations (PUT/GET/HEAD/DELETE), paginated file listing, and integrity verification. Exposes an S3-minimal surface for managed artifacts.

These are backed by tables in the EMX2 `_SYSTEM_` schema (prefixed with `Hpc` to avoid collisions). The system tables hold job state (including `output_artifact_id` foreign key to artifacts), worker registrations, capability advertisements, transition audit logs, artifact metadata, and artifact file content (stored in EMX2 FILE columns for managed residence). A Vue-based HPC dashboard provides browser access to jobs, workers, and artifacts including direct file upload.

The dashboard renders structured progress fields directly from HPC data: the jobs overview and job detail pages show the latest job-level progress snapshot (`phase`, `message`, `progress`), while job transition history shows per-transition progress values. The workers page shows active jobs per worker with the same structured progress fields. UI polling is periodic (no server push): data is refreshed in place at a fixed interval and rendered with stable row identity to avoid full-table flicker.

All endpoints live under `/api/hpc/*` with a shared before-handler that validates protocol headers and applies authentication and authorization. The health endpoint (`/api/hpc/health`) SHOULD be exempt from authentication.

## HPC Environment

The HPC side consists of:

- **Head Node Controller** — a daemon that registers capabilities, polls for pending jobs, maps processor + profile to Slurm parameters, submits `sbatch`, and reports the result back to EMX2.
- **Slurm Controller** — the cluster's workload manager, unchanged from its standard role.
- **Apptainer Runtime / Wrapper Script** — executes the workload on a compute node, either inside an Apptainer (formerly Singularity) container or via a wrapper script executed directly on the host. The daemon handles all communication with EMX2: staging input artifacts (symlink for posix, download for managed), monitoring Slurm job state, reading filesystem-based progress updates, uploading or registering output artifacts, and posting status transitions. The workload MUST NOT have direct EMX2 access — its only output channel is the filesystem (exit code, output files, and an optional `.hpc_progress.jsonl` progress file).
- **NFS Shared Storage** — an NFS export mounted on the head node and all compute nodes. Stores Apptainer SIF images, POSIX-resident artifacts, and shared scratch data.
- **Local Scratch** — per-node temporary storage, discarded after job completion.

## Separation of Responsibilities

| Concern                                                                   | Owner                               |
| ------------------------------------------------------------------------- | ----------------------------------- |
| Job registry, lifecycle state, artifact metadata, managed file storage    | EMX2                                |
| Capability registration, job claiming, Slurm submission, artifact staging | Head Node Controller                |
| Workload execution                                                        | Apptainer Runtime or Wrapper Script |
| Scheduling, resource allocation, node dispatch                            | Slurm Controller                    |
| Managed artifact binary content (FILE columns)                            | EMX2 Database                       |
| Shared data between jobs, POSIX-resident artifacts                        | NFS                                 |

## Head Node Daemon

The head node controller is a Python CLI (`emx2-hpc-daemon`), built on Click for argument parsing, httpx for HTTP communication, and subprocess for Slurm command invocation. It exposes four commands:

| Command    | Purpose                                                                               |
| ---------- | ------------------------------------------------------------------------------------- |
| `run`      | Start the daemon main loop (register → poll → claim → submit → monitor, repeating).   |
| `once`     | Run a single poll-claim-monitor cycle, then exit. Suitable for cron-based invocation. |
| `register` | Register the worker with EMX2 and exit.                                               |
| `check`    | Validate config, connectivity, and Slurm command availability.                        |

Both `run` and `once` SHOULD accept a `--simulate` flag that walks jobs through all lifecycle states without invoking Slurm or creating working directories. In simulate mode, each poll cycle advances tracked jobs one step (CLAIMED → SUBMITTED → STARTED → COMPLETED), completing a full lifecycle in approximately three cycles.

The daemon SHOULD send periodic heartbeats (default: every 120 seconds) to keep the worker registration alive. On startup, it SHOULD recover tracking state for non-terminal jobs from a previous run. On SIGTERM/SIGINT, it SHOULD stop accepting new work and exit gracefully; Slurm jobs continue running independently and are recovered on next startup.

During the monitor loop, the daemon SHOULD also check for a `.hpc_progress.jsonl` file (NDJSON format — one JSON object per line) in each STARTED job's output directory. The workload appends progress lines; the daemon reads the **last complete line** and relays it to EMX2 as a same-state STARTED transition with structured fields (`phase`, `message`, `progress`) plus an optional human-readable `detail`. This append-only format is inherently safe against partial reads: incomplete trailing lines (from in-progress writes) are silently skipped. The daemon validates and sanitizes each progress object: `phase` (string, max 100 chars), `message` (string, max 500 chars), `progress` (float in [0.0, 1.0]). Unknown keys are dropped; invalid types are skipped per-field. EMX2 stores these fields both in transition audit rows and as the latest progress snapshot on the job row.

Workloads report progress by appending lines (works from any language):

```bash
echo '{"phase":"processing","message":"step 3 of 10","progress":0.3}' >> "$HPC_OUTPUT_DIR/.hpc_progress.jsonl"
```

Configuration SHOULD be via a YAML file specifying EMX2 connection details, Slurm parameters, Apptainer settings, and profile-to-resource mappings.

# End-to-End Protocol

The happy-path sequence proceeds in four phases (see Appendix B for the sequence diagram).

**Phase 1 — Registration and job acquisition.** The head node registers its capabilities with the Workers API, then polls the Jobs API for pending jobs that match its declared processors and profiles. When it finds one, it claims it. The claim MUST be atomic: if two workers try to claim the same job, only one succeeds.

**Phase 2 — Input staging and Slurm submission.** The head node stages input artifacts: for posix artifacts it symlinks the `file://` path into the job's input directory (zero-copy); for managed artifacts it downloads files via `GET /api/hpc/artifacts/{id}/files/{path}`. SHA-256 hashes MUST be verified after staging. The head node then maps the job's processor and profile to execution parameters — either an Apptainer SIF image or a wrapper script entrypoint — and a set of Slurm parameters, then submits via `sbatch`. It reports the Slurm job ID back to EMX2 as a SUBMITTED transition.

**Phase 3 — Execution and monitoring.** Slurm dispatches the job to a compute node, where it runs either inside an Apptainer container or as a wrapper script (see §5). The daemon monitors the job via `squeue`/`sacct` and posts a STARTED transition to EMX2 when execution begins. During execution, the daemon checks for a `.hpc_progress.jsonl` file (NDJSON) in the job's output directory and relays the last valid progress line to EMX2 using structured progress fields (`phase`, `message`, `progress`) on same-state STARTED transitions. All EMX2 communication MUST be driven by the daemon — the workload itself has no direct access to EMX2.

**Phase 4 — Output and completion.** When the daemon detects job completion via Slurm, it creates an output artifact, uploads files (for managed residence) or registers the output directory path (for posix residence), and commits. It posts a COMPLETED transition with the `output_artifact_id` field linking the job to its output artifact. The artifact ID is stored as a foreign key on the job record, making outputs discoverable via GraphQL.

At every step, the client discovers what it can do next from hypermedia links in the response. If a transition is not legal in the current state, the corresponding link MUST be absent. Failure at any point results in a FAILED transition with a reason code (see Job Lifecycle, below).

## Design Principles

The architecture is deliberately minimal:

- **EMX2 is the system of record** for jobs, lifecycle state, and artifact metadata.
- **HPC is responsible for execution** via Slurm and Apptainer. EMX2 MUST NOT tell the cluster how to schedule.
- **Inputs and outputs are tracked as artifacts.** Jobs reference artifacts by ID. Content is accessed via the artifact file API (managed) or directly via `file://`, `s3://`, or `https://` URIs (external). Managed artifacts store binary content in EMX2; external artifacts store only metadata.
- **Workers declare capabilities; EMX2 enforces compatibility.** Workers advertise `(processor, profile)` capabilities at registration. When a worker claims a job, EMX2 verifies the worker has a matching capability; if not, the claim is rolled back. There is no negotiation.
- **The API is resource-oriented.** State transitions are sub-resources of jobs. Responses include hypermedia links advertising legal next actions.
- **Everything is recoverable.** Transitions MUST be idempotent, timeouts detect stuck jobs, and the system converges to a consistent state after any single failure.

# Processor and Execution Model

Jobs reference a logical processor identifier (e.g. `text-embedding:v3`) and an optional execution profile (e.g. `gpu-medium`). EMX2 MUST NOT encode cluster-specific scheduling parameters. Instead, the protocol uses a hybrid model that separates **application intent** from **cluster policy**.

EMX2 specifies _what_ to run — a processor identifier and a profile. The head node determines _how_ — which SIF image, which Slurm partition, how many GPUs, how much memory, and what wall time.

For example, given a job requesting `text-embedding:v3` with profile `gpu-medium`, the head node resolves this to:

```
text-embedding:v3 + gpu-medium
    → image: /nfs/images/text-embedding_v3.sif
    → partition: gpu
    → gpus: 1
    → cpus_per_task: 8
    → mem: 64G
    → time: 04:00:00
    → command: apptainer exec --nv /nfs/images/text-embedding_v3.sif ...
```

This mapping is maintained locally on the HPC system and MAY evolve independently of the protocol.

## Wrapper Scripts (Entrypoint Mode)

Not all workloads fit the "run one container command" model. Multi-process orchestration, module loads, venv activation, and structured teardown require direct host access. For these cases, profiles MAY specify an `entrypoint` instead of (or alongside) `sif_image`.

When `entrypoint` is set, the batch script exports well-defined environment variables and `exec`s the wrapper script directly on the host:

```yaml
profiles:
  vtm-pipeline:gpu-large:
    entrypoint: /nfs/scripts/vtm-pipeline.sh
    # Slurm scheduling — these map directly to #SBATCH directives:
    partition: gpu # --partition
    cpus: 16 # --cpus-per-task
    memory: 128G # --mem
    time: "08:00:00" # --time
    sbatch_args: # additional raw sbatch flags
      - "--gres=gpu:a40:2"
      - "--exclusive"
```

The wrapper script contract defines these environment variables:

- **`HPC_JOB_ID`** — the EMX2 job identifier.
- **`HPC_INPUT_DIR`** — directory containing staged input artifacts (read from here).
- **`HPC_OUTPUT_DIR`** — directory for output files (write results here).
- **`HPC_WORK_DIR`** — scratch working directory.
- **`HPC_PARAMETERS`** — JSON string with the full job parameters object.
- Any extra environment variables from the profile or job parameters.

The wrapper's responsibilities: read inputs from `HPC_INPUT_DIR`, write results to `HPC_OUTPUT_DIR`, and exit 0 on success. Optionally, append NDJSON lines to `$HPC_OUTPUT_DIR/.hpc_progress.jsonl` for in-flight progress reporting (see §3 for format). The wrapper MUST NOT have direct EMX2 access — the daemon handles all API communication.

Use wrapper scripts when multi-process orchestration, host-level module systems, or setup that doesn't fit inside a single container exec is needed. Use Apptainer containers when reproducible, isolated execution with a single SIF image is preferred.

## Rationale: Why Hybrid Profiles

Three models were considered:

1. **Full embedding** — Slurm parameters in EMX2 payloads. Rejected: couples EMX2 to cluster configuration and violates institutional scheduling governance.
2. **Full delegation** — no hint from EMX2 at all. Rejected: EMX2 cannot express workload intent, making it impossible to distinguish a lightweight job from a GPU-heavy one.
3. **Hybrid profiles** (adopted) — EMX2 expresses intent through a logical profile; the HPC side maps it to concrete resources.

The hybrid model keeps scheduling policy within HPC governance while letting EMX2 express meaningful workload requirements. Cluster configuration can change without protocol changes.

# Job Lifecycle

A job MUST pass through a strict state machine. Every transition is recorded as a sub-resource and the full history is queryable as an audit log.

```{=latex}
\begin{center}
\begin{tikzpicture}[
  node distance=1.6cm,
  state/.style={rectangle, draw, rounded corners, minimum width=1.8cm, minimum height=0.7cm, font=\small\sffamily},
  terminal/.style={state, double},
  arr/.style={-{Stealth[length=5pt]}, thick}
]
  \node[state]    (pending)   {PENDING};
  \node[state]    (claimed)   [right=of pending]   {CLAIMED};
  \node[state]    (submitted) [right=of claimed]   {SUBMITTED};
  \node[state]    (started)   [right=of submitted] {STARTED};
  \node[terminal] (completed) [right=of started]   {COMPLETED};
  \node[terminal] (failed)    [below=1.2cm of submitted] {FAILED};
  \node[terminal] (cancelled) [below=1.2cm of claimed]   {CANCELLED};

  \draw[arr] (pending)   -- (claimed);
  \draw[arr] (claimed)   -- (submitted);
  \draw[arr] (submitted) -- (started);
  \draw[arr] (started)   -- (completed);
  \draw[arr] (claimed)   -- (failed);
  \draw[arr] (submitted) -- (failed);
  \draw[arr] (started)   -- (failed);
  \draw[arr] (pending)   -- (cancelled);
  \draw[arr] (claimed)   -- (cancelled);
  \draw[arr] (submitted) -- (cancelled);
  \draw[arr] (started)   -- (cancelled);
\end{tikzpicture}
\end{center}
```

| From      | To        | Initiated by                    | Trigger                                     |
| --------- | --------- | ------------------------------- | ------------------------------------------- |
| PENDING   | CLAIMED   | Head node                       | Atomic claim                                |
| PENDING   | CANCELLED | EMX2 or user                    | Cancel before claim                         |
| CLAIMED   | SUBMITTED | Head node                       | After `sbatch`                              |
| CLAIMED   | FAILED    | EMX2 or Head node               | Timeout or submission error                 |
| CLAIMED   | CANCELLED | Head node or EMX2               | Cancel before submission                    |
| SUBMITTED | STARTED   | Daemon (monitoring Slurm state) | Execution begins                            |
| SUBMITTED | FAILED    | Head node or EMX2               | Slurm rejection or timeout                  |
| SUBMITTED | CANCELLED | Head node or EMX2               | Cancel; head node issues `scancel`          |
| STARTED   | COMPLETED | Daemon (monitoring Slurm state) | Outputs committed; `output_artifact_id` set |
| STARTED   | FAILED    | Daemon (monitoring Slurm state) | Runtime error, hash mismatch, or timeout    |
| STARTED   | CANCELLED | EMX2                            | Cancel; daemon issues `scancel`             |

All other transitions MUST be rejected with `409 Conflict`. Jobs in terminal states (COMPLETED, FAILED, CANCELLED) MUST NOT transition further.

Jobs MAY be deleted via `DELETE /api/hpc/jobs/{id}`. If the job is in a non-terminal state, the caller MUST cancel it first via `POST /api/hpc/jobs/{id}/cancel`; attempting to delete a non-terminal job MUST return `409 Conflict`. The transition history is deleted with the job.

## Failure Recovery

The protocol is designed to converge to a consistent state after any single failure.

**Idempotent transitions.** A transition request is considered identical when `job_id`, `status`, the effective authenticated worker identity, and all payload fields match a previously accepted transition. Duplicates SHOULD return `200 OK`. Non-identical submissions to the same state MUST return `409 Conflict`. This allows safe retries on network failure.

**Timeout-driven state progression.** Two enforcement tiers prevent jobs from stalling indefinitely:

- **Per-job timeout (daemon-enforced).** Jobs MAY carry an optional `timeout_seconds` field set at submission. The daemon measures total elapsed time from `claimed_at` and applies to any tracked (non-terminal) status. If exceeded, the daemon transitions the job to FAILED and issues `scancel` if a Slurm job ID is known. This provides fine-grained, per-job wall-clock control when callers know the expected duration.

- **Per-profile submission timeout (daemon-enforced).** Each profile in the daemon config carries `submission_timeout_seconds` (default 300). The daemon checks tracked jobs in CLAIMED status (i.e. claimed but not yet submitted to Slurm) against this limit on each monitor cycle. On timeout, it transitions the job to FAILED. This acts as a safety net for jobs where `sbatch` never ran.

- **Slurm wall-time limit.** Execution timeouts for running jobs are delegated to Slurm's native `--time` directive (set via the profile `time` field). When Slurm kills a job for exceeding wall time, the daemon detects the TIMEOUT state via `sacct` and posts a FAILED transition. This avoids duplicating Slurm's scheduling logic in the daemon.

**Infrastructure termination.** If Slurm kills a job unexpectedly (node failure, preemption, wall-time exceeded), the daemon detects this via `squeue`/`sacct` on the next monitor cycle and posts a FAILED transition. The workload (whether container or wrapper script) has no direct EMX2 access — its only output channel is the filesystem: exit code, output files in `HPC_OUTPUT_DIR`, and an optional `.hpc_progress.jsonl` progress file. The daemon is responsible for interpreting these signals and posting appropriate transitions.

**Concurrency control.** Workers declare `max_concurrent_jobs` during registration and are responsible for not over-claiming. EMX2 MAY optionally enforce an upper bound.

# Artifact Store

Artifacts are the primary data objects in the system: job inputs, job outputs, model weights, execution logs, container images. This section describes how they are classified, where they live, how their integrity is ensured, and how their lifecycle is managed.

## Classification

Every artifact is described along two dimensions.

**Type** is a free-text label describing what the artifact is. No fixed ontology is prescribed — users write whatever is meaningful for their context: `csv`, `parquet`, `onnx-model`, `vcf`, `log`, `report`, `sif`, etc. Per-file `content_type` covers media type details, so a single field suffices for artifact-level classification.

**Name** is a human-readable identifier for the artifact. Names are optional but strongly encouraged — without one, artifacts are identified only by truncated UUIDs in the UI. The daemon SHOULD auto-generate names like `output-<job-id-prefix>` for job outputs.

**Residence** specifies where the content physically lives.

| Residence   | Content URI           | Access pattern                                           |
| ----------- | --------------------- | -------------------------------------------------------- |
| `managed`   | Artifact API endpoint | Upload/download via API; also S3-compatible endpoint.    |
| `posix`     | `file:///nfs/...`     | Direct filesystem read from any node with the NFS mount. |
| `s3`        | `s3://bucket/key`     | Direct access with presigned URLs or credentials.        |
| `http`      | `https://...`         | Direct download; supports range requests.                |
| `reference` | N/A                   | Metadata-only; EMX2 tracks but does not store or proxy.  |

## Residence: NFS

The cluster's NFS export is mounted on both the head node and all compute nodes. This makes it the natural location for large, frequently-reused artifacts: model weights, pre-built indices, and Apptainer SIF images. Data produced by one job is immediately available to the next.

The `posix` residence registers artifacts that live on NFS. The content URI is a `file://` path referencing the absolute mount location. The Apptainer runtime reads directly from NFS with no transfer overhead — the fastest access pattern for data already co-located with compute.

Since all nodes share the same NFS export, mount availability is not a per-node concern. Immutability is enforced by convention: operators MUST ensure that committed paths are not modified or deleted outside the protocol. Hash verification still applies — the runtime checks SHA-256 hashes before use, just as for any other residence.

## Residence: Managed Repository

Managed artifacts are stored in EMX2's database using the FILE column type. The artifact file API exposes an S3-minimal surface — path-based `PUT` (upload), `GET` (download), `HEAD` (metadata), and `DELETE` operations on individual files within an artifact, plus paginated listing. This maps cleanly to WebDAV semantics and makes future S3-compatible gateway implementation straightforward.

The file API uses path-addressed URLs: `/api/hpc/artifacts/{id}/files/{path}`. Paths are logical names within the artifact (e.g. `data.parquet`, `model/weights.bin`) and support any depth. The server computes SHA-256 on upload and returns it in the response; clients do not need to pre-compute hashes for individual file uploads (though the overall artifact hash is provided at commit time). For browser-based uploads, the client SHOULD send a raw binary PUT body and compute the commit-time SHA-256 via the SubtleCrypto API; no multipart encoding is required.

For analytical tools (DuckDB, pandas), committed artifacts can be accessed via the GET endpoint with standard HTTP range requests. A future S3-compatible gateway can proxy these paths to provide native S3 connector support.

## Multi-File Artifacts and Integrity

Some artifacts consist of multiple files: a model with a tokenizer sidecar, a VCF with a tabix index. Multi-file artifacts MUST include a file manifest listing each file's path, size, and individual SHA-256 hash.

For single-file artifacts, the content hash is the SHA-256 of the file bytes. For multi-file artifacts, the top-level hash is a tree hash: `SHA256(concat(for each file in sorted(paths): path + ":" + sha256_hex(file_bytes)))`. Any modification to any constituent file is detectable.

Input artifacts MUST be COMMITTED before a job can reference them. The daemon verifies hashes before execution — for managed artifacts it downloads and hashes locally; for NFS artifacts it reads from the mount. A mismatch MUST result in a FAILED transition with reason `input_hash_mismatch`. Output artifacts MUST be immutable after commit.

## Schema Metadata

Tabular artifacts (e.g. `type: "parquet"` or `type: "csv"`) SHOULD include a `schema` field describing column names, types, nullability, row count, and (for Parquet) row group count. This is extracted automatically from the Parquet file footer at commit time, so consumers can discover data shape without downloading the file.

This metadata makes artifacts directly queryable. Tools like DuckDB can read a Parquet file from its NFS path or HTTP URL using range requests — fetching the footer first, then only the needed columns and rows. For NFS-resident artifacts this happens with zero network overhead. In practice, EMX2 application code or analytical scripts can use this to inspect job outputs without pulling entire datasets: a SQL query against the artifact's `content_url` (or NFS path) returns results in place.

## Execution Logs

The daemon SHOULD upload execution logs as artifacts of type `log`, governed by the same retention policy and integrity model as other artifacts. Log artifacts are referenced alongside outputs in the COMPLETED (or FAILED) transition. Structured JSONL is recommended over plain text for machine queryability.

## Artifact Lifecycle

Managed artifacts pass through a state machine:

```{=latex}
\begin{center}
\begin{tikzpicture}[
  node distance=1.6cm,
  state/.style={rectangle, draw, rounded corners, minimum width=1.8cm, minimum height=0.7cm, font=\small\sffamily},
  terminal/.style={state, double},
  arr/.style={-{Stealth[length=5pt]}, thick}
]
  \node[state]    (created)   {CREATED};
  \node[state]    (uploading) [right=of created]   {UPLOADING};
  \node[terminal] (committed) [right=of uploading] {COMMITTED};
  \node[terminal] (failed)    [below=1.2cm of uploading] {FAILED};

  \draw[arr] (created)   -- (uploading);
  \draw[arr] (uploading) -- (committed);
  \draw[arr] (created)   -- (failed);
  \draw[arr] (uploading) -- (failed);
\end{tikzpicture}
\end{center}
```

External artifacts (POSIX, S3, HTTP, reference) skip the upload phase: REGISTERED → COMMITTED or REGISTERED → FAILED.

Artifacts MUST be immutable after COMMITTED. If an artifact stalls in CREATED or UPLOADING with no activity within a configured timeout, it SHOULD transition to FAILED and become eligible for garbage collection.

## Job→Artifact Link

Jobs reference output artifacts via the `output_artifact_id` field, which is a foreign key to the `HpcArtifacts` table. When the daemon completes a job and uploads (or registers) output artifacts, it passes the `output_artifact_id` in the COMPLETED transition request. EMX2 stores this link on the job record, making it queryable via GraphQL (`output_artifact_id { id name type status { name } }`).

Input artifacts are referenced in the job's `inputs` field. The canonical format is an array of objects: `[{"artifact_id": "..."}]`. Plain arrays of artifact ID strings (`["id-1", "id-2"]`) and named-reference objects (`{"dataset": "id-1"}`) are also accepted for convenience. The daemon stages input artifacts before execution: for managed artifacts it downloads files via GET; for posix artifacts it symlinks the `file://` path into the job's input directory. The output residence for each profile is configured in the daemon config via an `artifact_residence` field on each profile entry. This two-residence model means that large datasets on NFS incur zero transfer overhead, while smaller browser-uploaded artifacts are served from the managed store.

# API Design

This section describes the principles governing the API. Full endpoint specifications with request and response payloads are in Appendix A.

## Protocol Contract Source of Truth

The canonical protocol contract is:

- `protocol/hpc-protocol.json`

This JSON Schema is normative for cross-stack protocol constants and shape contracts, including:
- API version
- job/artifact enum values
- transition matrix
- required/optional headers
- RFC 9457 problem detail shape
- expected HATEOAS link relations
- cross-language HMAC fixture vectors

Generated files derived from this schema:
- `tools/hpc-daemon/src/emx2_hpc_daemon/_generated.py`
- `apps/hpc/app/utils/protocol.ts`

Regeneration command (run from repo root):

```bash
uv run python protocol/generate.py
```

Required change workflow for protocol edits:
1. Edit `protocol/hpc-protocol.json`.
2. Regenerate derived files with `protocol/generate.py`.
3. Run contract/conformance tests:
   - `backend/molgenis-emx2-hpc/.../HpcApiContractTest`
   - `tools/hpc-daemon/tests/test_contract.py`
   - `backend/molgenis-emx2-webapi/.../HpcApiProtocolContractE2ETest`
4. Ship schema + generated files + contract tests in the same change.

## Versioning

The API SHOULD be versioned via the `X-EMX2-API-Version` request header rather than a URL path prefix. Versions are date-based strings (e.g. `2025-01`). This keeps URLs stable across versions and avoids cascading changes to hypermedia links. Missing header → `400 Bad Request`; unsupported version → `400 Bad Request`.

## Request Headers and Traceability

Every request from a worker or runtime MUST include a standard set of headers:

| Header               | Required          | Purpose                                                                                                                                        |
| -------------------- | ----------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| `X-EMX2-API-Version` | Yes               | Protocol version (date-based, e.g. `2025-01`).                                                                                                 |
| `X-Request-Id`       | Yes               | Unique per-request identifier (UUID v4).                                                                                                       |
| `X-Timestamp`        | Yes               | Request creation time; used for HMAC verification and replay prevention.                                                                       |
| `X-Nonce`            | When HMAC enabled | Cryptographically random single-use value; replay prevention.                                                                                  |
| `X-Trace-Id`         | No                | Identifier spanning a logical operation, e.g. an entire job lifecycle.                                                                         |
| `X-Worker-Id`        | Worker write endpoints | Worker identifier; REQUIRED for worker-origin lifecycle endpoints (register, heartbeat, claim, transition, complete). Must match path/body where applicable. |
| `Authorization`      | When HMAC enabled | `HMAC-SHA256 <hex-signature>` (see Authentication and Trust).                                                                                  |
| `Content-SHA256`     | Non-JSON bodies   | Hex-encoded SHA-256 of the request body. Required for all binary uploads; when HMAC is enabled it is also used as the body hash in the canonical string. |

EMX2 MUST echo `X-Request-Id` in error responses for traceability.

## Resource Model and Hypermedia

The API is organized around two resources: **jobs** and **artifacts**. State transitions on jobs are a **transitions** sub-resource: each transition is a created resource (`POST` returns `201 Created`), the history is queryable, and the job representation includes `_links` advertising legal next actions. Clients follow links rather than hardcoding URL patterns (HATEOAS). All URLs in `_links` fields MUST be treated as opaque — clients dereference them as-is.

## Error Responses

Client errors MUST return structured JSON with `type`, `title`, `status`, and `detail` fields, following the RFC 9457 (Problem Details for HTTP APIs) structure. Whether to formally adopt the `application/problem+json` media type is an open deployment decision.

## Endpoint Summary

All endpoints live under `/api/hpc`. Detailed specifications are in Appendix A.

```{=latex}
\subsubsection*{Workers API}\addcontentsline{toc}{subsubsection}{Workers API}

\begin{tabular}{@{} l l p{8cm} @{}}
\hline
\textbf{Method} & \textbf{Path} & \textbf{Purpose} \\
\hline
\texttt{POST}   & \texttt{/workers/register}       & Register or update a worker and its capabilities \\
\texttt{POST}   & \texttt{/workers/\{id\}/heartbeat} & Lightweight keepalive \\
\texttt{DELETE}  & \texttt{/workers/\{id\}}           & Remove a stale worker \\
\hline
\end{tabular}

\bigskip
\subsubsection*{Jobs API}\addcontentsline{toc}{subsubsection}{Jobs API}

\begin{tabular}{@{} l l p{8cm} @{}}
\hline
\textbf{Method} & \textbf{Path} & \textbf{Purpose} \\
\hline
\texttt{POST}   & \texttt{/jobs}                     & Create a new job (\textsc{pending}) \\
\texttt{GET}    & \texttt{/jobs}                     & List/filter jobs (paginated) \\
\texttt{GET}    & \texttt{/jobs/\{id\}}               & Retrieve a single job \\
\texttt{POST}   & \texttt{/jobs/\{id\}/claim}         & Atomically claim a pending job \\
\texttt{POST}   & \texttt{/jobs/\{id\}/transition}    & Report a state transition \\
\texttt{POST}   & \texttt{/jobs/\{id\}/cancel}        & Cancel from any non-terminal state \\
\texttt{DELETE}  & \texttt{/jobs/\{id\}}               & Delete a job and its history \\
\texttt{GET}    & \texttt{/jobs/\{id\}/transitions}   & Ordered transition audit log \\
\hline
\end{tabular}

\bigskip
\subsubsection*{Artifact API}\addcontentsline{toc}{subsubsection}{Artifact API}

\begin{tabular}{@{} l l p{8cm} @{}}
\hline
\textbf{Method} & \textbf{Path} & \textbf{Purpose} \\
\hline
\texttt{POST}   & \texttt{/artifacts}                          & Create an artifact (managed or external) \\
\texttt{GET}    & \texttt{/artifacts/\{id\}}                    & Retrieve artifact metadata \\
\texttt{PUT}    & \texttt{/artifacts/\{id\}/files/\{path\}}     & Upload a file by path \\
\texttt{GET}    & \texttt{/artifacts/\{id\}/files/\{path\}}     & Download a file \\
\texttt{HEAD}   & \texttt{/artifacts/\{id\}/files/\{path\}}     & File metadata (existence, hash, size) \\
\texttt{DELETE}  & \texttt{/artifacts/\{id\}/files/\{path\}}     & Delete a file (before commit only) \\
\texttt{GET}    & \texttt{/artifacts/\{id\}/files}              & List files (paginated, prefix-filterable) \\
\texttt{POST}   & \texttt{/artifacts/\{id\}/commit}             & Commit with top-level SHA-256 \\
\hline
\end{tabular}

\bigskip
\subsubsection*{Health}\addcontentsline{toc}{subsubsection}{Health}

\begin{tabular}{@{} l l p{6.2cm} @{}}
\hline
\textbf{Method} & \textbf{Path} & \textbf{Purpose} \\
\hline
\texttt{GET}    & \texttt{/health} & Liveness check (exempt from authentication) \\
\hline
\end{tabular}
```

# Authentication, Authorization, and Trust

The protocol operates across a trust boundary between EMX2 and the HPC environment. Every request must be validated for identity, integrity, freshness, and permissions.

## Authentication Envelope

Worker-origin requests use protocol headers plus HMAC:

```
X-EMX2-API-Version: 2025-01
X-Request-Id:       <UUID v4>
X-Timestamp:        <unix epoch seconds>
X-Nonce:            <random value, used exactly once>
Authorization:      HMAC-SHA256 <hex-encoded signature>
```

The HMAC canonical form is `METHOD\nPATH\nBODY_HASH\nTIMESTAMP\nNONCE`.

- **JSON requests:** `BODY_HASH = SHA256(utf8-body)`.
- **Binary requests:** `BODY_HASH = Content-SHA256` header value.

This provides:

- **Channel authenticity:** request came from a caller that knows the worker credential.
- **Integrity:** tampered path/body/headers fail signature verification.
- **Freshness:** timestamp-window and nonce replay checks.

## Provisioning and Activation

HPC functionality is controlled by an explicit feature flag. All endpoints except `/api/hpc/health` return `503` until HPC is enabled.

Set `_SYSTEM_` setting `MOLGENIS_HPC_ENABLED=true`. When enabled, the server initializes the HPC schema and activates `/api/hpc/*` routes. When disabled, the server MUST reject all non-health HPC requests with `503 Service Unavailable`.

Worker credentials also require `_SYSTEM_` setting `MOLGENIS_HPC_CREDENTIALS_KEY` (server-side encryption key for stored worker secrets). If this setting is missing, credential issue/rotate MUST return `503 Service Unavailable`.

Each head node must have a stable `worker_id` and include `X-Worker-Id` on worker write endpoints.

Worker authentication is credential-based. Credentials are issued per worker identity and are independent of HPC activation. A worker credential MAY exist before the worker has ever registered.

## Authentication Resolution (Rewritten Cascade)

The `/api/hpc/*` before-handler resolves principal type as follows:

1. **Worker principal (HMAC):** valid `Authorization: HMAC-SHA256 ...` verified against the active credential for `X-Worker-Id`.
2. **User principal (token):** valid `x-molgenis-token`.
3. **User principal (session):** signed-in browser session.
4. Otherwise: `401 Unauthorized`.

This cascade determines **who** the caller is. It does not grant permissions by itself.

### Worker Credential Validation

Worker HMAC authentication is resolved by `X-Worker-Id`.

- The server looks up the active credential for that worker identity.
- The HMAC signature is verified against that credential.
- If the credential is missing, revoked, expired, malformed, or does not verify the request, the server returns `401 Unauthorized`.
- Revocation takes effect immediately. There is no grace period, overlap window, or fallback credential unless a new credential has been explicitly issued and configured on the worker.

### `401` vs `403`

- `401 Unauthorized` means the caller failed authentication: missing/invalid HMAC, revoked worker credential, expired worker credential, invalid token, or no signed-in session.
- `403 Forbidden` means the caller is authenticated but lacks authorization: for example, a signed-in EMX2 user without the required `_SYSTEM_` role, or an `Editor` trying to cancel/delete another user's job.

## Authorization Model

Authorization is explicit and role-based on `_SYSTEM_`. No role means no HPC access.

### Role Semantics

| Role on `_SYSTEM_` | Capability |
| ------------------ | ---------- |
| `Viewer`           | Read-only HPC data (jobs, transitions, workers, artifacts, files metadata/download). |
| `Editor`           | Submitter role: create jobs, create/upload/commit artifacts, cancel/delete own jobs only. |
| `Manager`          | Operator role: full lifecycle control, including worker credential management and cancelling/deleting any job. |
| `Owner` / admin    | Same or higher than `Manager`. |

### Endpoint Policy Matrix

| Endpoint class | Viewer | Editor | Manager | HMAC daemon |
| -------------- | ------ | ------ | ------- | ----------- |
| GET `/api/hpc/jobs*`, `/artifacts*`, `/workers*` | Yes | Yes | Yes | Yes |
| POST `/api/hpc/jobs` | No | Yes | Yes | Yes |
| POST `/api/hpc/jobs/{id}/cancel` | No | Own jobs only | Any job | Yes |
| DELETE `/api/hpc/jobs/{id}` | No | Own jobs only (terminal only) | Any job (terminal only) | Yes |
| Worker lifecycle (`/workers/register`, `/workers/{id}/heartbeat`, `/jobs/{id}/claim`, `/jobs/{id}/transition`, `/jobs/{id}/complete`) | No | No | Yes | Yes |
| DELETE `/api/hpc/workers/{id}` | No | No | Yes (user principal only) | No |
| Worker credential management (`/workers/{id}/credentials*`) | No | No | Yes | No |
| Artifact destructive ops (`DELETE /artifacts/{id}`, `DELETE /artifacts/{id}/files/{path}`) | No | No | Yes | Yes |

### Ownership Rules

- `submit_user` is server-derived from authenticated user/token identity on `POST /jobs`; client-supplied `submit_user` must be ignored.
- `Editor` can cancel/delete only jobs with `submit_user == authenticated identity`.
- `Manager` can cancel/delete any job.
- Non-terminal delete remains forbidden (`409`, must cancel first).

### Worker Identity and Capabilities

- `X-Worker-Id` is required for worker write endpoints.
- Where body/path also carry worker identity, they must match `X-Worker-Id`.
- Effective worker identity for claim/transition/complete is derived from `X-Worker-Id`, not trusted from body fields.
- Workers self-advertise capabilities on registration. EMX2 treats those advertised capabilities as observed worker state.
- The UI displays capabilities as reported by the worker; users do not configure them in advance.

### Worker Credentials

- Worker credentials are issued and revoked by `Manager` or `Owner` users.
- Credentials are returned exactly once at issue/rotate time.
- The stored secret is encrypted at rest.
- Credential issuance reserves the `worker_id`, even if no worker row exists yet.
- The first successful `POST /api/hpc/workers/register` for that `worker_id` creates or updates the worker row and replaces the observed capability set.
- Rotation has no implicit overlap. If an operator wants the daemon to keep working, the daemon configuration must be updated before or immediately after rotation and then restarted or reloaded.

### GraphQL Boundary for HPC Tables

- Mutations targeting `Hpc*` tables in `/_SYSTEM_/graphql` are forbidden.
- `Hpc*` GraphQL remains query-only for UI/reporting.
- All HPC writes must go through `/api/hpc/*` REST handlers.

### Internal Java API

Internal producers (for example, Catalogue-triggered workflows) should call a dedicated Java service facade (job/artifact orchestration service) that reuses the same validation and authorization rules as REST handlers, instead of mutating `Hpc*` tables through GraphQL.

## What EMX2 Enforces

EMX2 is the sole authority for job state, lifecycle transitions, and artifact metadata. Workers and submitters request actions; EMX2 accepts only legal state transitions and authorized operations.

# Summary, Trade-offs, and Open Questions

## What This Proposal Provides

A minimal, deterministic bridge between EMX2 and HPC infrastructure with these invariants:

- **Outbound-only communication.** EMX2 never initiates connections to the cluster.
- **Flexible execution.** Either Apptainer containers (SIF images on NFS) or wrapper scripts, invoked by Slurm on compute nodes.
- **NFS as the primary shared data path.** Artifacts co-located with compute require no transfer.
- **Resource-oriented API with HATEOAS.** Clients discover actions from server responses.
- **Typed, content-addressed artifacts.** A single metadata model governs everything from queryable Parquet tables to multi-gigabyte model weights, with SHA-256 integrity verification.
- **Idempotent transitions and timeout-based recovery.** The system converges after any single failure.

## Key Trade-offs

**Polling vs. push.** The outbound-only constraint requires the head node to poll EMX2 for new jobs, introducing latency proportional to the poll interval. A shorter interval reduces latency but increases API load. For the expected workload (long-running GPU jobs), 10–30 seconds is likely acceptable, but this should be validated under realistic load.

**Hybrid profiles.** EMX2 expresses workload intent; the HPC side interprets it. This preserves scheduling governance but means EMX2 cannot guarantee exact resource allocation. The head node's profile-to-Slurm mapping is an out-of-band dependency that must be kept in sync manually.

**NFS immutability by convention.** The protocol registers NFS paths as artifacts but cannot enforce immutability on them. If an operator modifies a file after it has been committed, the hash check will catch it at runtime — but the job will fail rather than being prevented. In environments with strict data governance this may need filesystem-level write protection (e.g. read-only snapshots or chattr).

**S3-minimal file surface.** The artifact file API exposes path-based GET/PUT/HEAD/DELETE operations that map to S3 semantics (`GetObject`, `PutObject`, `HeadObject`, `DeleteObject`). This is sufficient for the initial use case and makes a future S3-compatible gateway straightforward to implement. Until then, analytical tools access managed artifacts via HTTP GET with range request support.

**Authentication and authorization.** Per-worker HMAC and user-token/session authentication are both supported, but access is role-gated and HPC write operations are REST-only. See §8 for details.

## Open Design Decisions

| Decision              | Options                              | Considerations                                                                           |
| --------------------- | ------------------------------------ | ---------------------------------------------------------------------------------------- |
| Artifact retention    | TTL, reference-counted, or manual    | Out of protocol scope, but the store must accommodate the chosen strategy.               |
| S3-compatible gateway | MinIO proxy, custom gateway, or none | The path-based API maps to S3 semantics; a gateway adds DuckDB/pandas native S3 support. |

\newpage

# Appendix A: API Reference {.unnumbered}

Full endpoint specifications. All endpoints require authentication and authorization as described in §8. URLs shown here are illustrative; in practice, clients MUST follow `_links` from server responses.

## A.1 Workers API {.unnumbered}

### POST /api/hpc/workers/register {.unnumbered}

Registers a worker or updates its registration. Idempotent — subsequent calls update the heartbeat timestamp and replace the capability set.

`X-Worker-Id` MUST be present and MUST match `worker_id` in the request body.

Authentication is by per-worker HMAC credential. Unknown, revoked, expired, or invalid credentials MUST return `401 Unauthorized`.

```json
{
  "worker_id": "hpc-headnode-01",
  "hostname": "login-node.cluster.local",
  "capabilities": [
    {
      "processor": "text-embedding:v3",
      "profile": "gpu-medium",
      "max_concurrent_jobs": 4
    }
  ]
}
```

**Response:** `200 OK` with worker metadata and HATEOAS links.

```json
{
  "worker_id": "hpc-headnode-01",
  "hostname": "login-node.cluster.local",
  "registered_at": "2026-02-21T10:00:00",
  "last_heartbeat_at": "2026-02-21T10:00:00",
  "_links": {
    "self": { "href": "/api/hpc/workers/hpc-headnode-01", "method": "GET" },
    "heartbeat": {
      "href": "/api/hpc/workers/hpc-headnode-01/heartbeat",
      "method": "POST"
    },
    "jobs": { "href": "/api/hpc/jobs?status=PENDING", "method": "GET" }
  }
}
```

### POST /api/hpc/workers/{id}/heartbeat {.unnumbered}

Lightweight heartbeat. Updates `last_heartbeat_at` without re-submitting capabilities. The daemon SHOULD send this periodically (default: every 120 seconds) between poll cycles.
Missed heartbeats MUST NOT auto-delete or auto-deregister the worker identity.

`X-Worker-Id` MUST be present and MUST match `{id}`.

**Response:** `200 OK` with `{"worker_id": "...", "status": "ok"}`.

### POST /api/hpc/workers/{id}/credentials/issue {.unnumbered}

Issues a new credential for a worker identity. This endpoint is for `Manager` or `Owner` users only. It returns the new secret exactly once.

If the `worker_id` does not yet exist in `HpcWorkers`, it is still considered reserved for future worker registration.

```json
{
  "label": "slurm-headnode-01",
  "expires_at": "2026-12-31T23:59:59"
}
```

**Response:** `201 Created`

```json
{
  "id": "f7632056-1a2b-4727-b0c7-3bdf18aa75dd",
  "worker_id": "hpc-headnode-01",
  "status": "ACTIVE",
  "label": "slurm-headnode-01",
  "secret": "emx2hpc_XXXXXXXXXXXXXXXX",
  "created_at": "2026-02-21T10:00:00",
  "expires_at": "2026-12-31T23:59:59",
  "_links": {
    "self": {
      "href": "/api/hpc/workers/hpc-headnode-01/credentials/f7632056-1a2b-4727-b0c7-3bdf18aa75dd",
      "method": "GET"
    },
    "list": { "href": "/api/hpc/workers/hpc-headnode-01/credentials", "method": "GET" },
    "revoke": {
      "href": "/api/hpc/workers/hpc-headnode-01/credentials/f7632056-1a2b-4727-b0c7-3bdf18aa75dd/revoke",
      "method": "POST"
    }
  }
}
```

**Error responses:** `409 Conflict` when an active credential already exists, `503 Service Unavailable` when `_SYSTEM_.MOLGENIS_HPC_CREDENTIALS_KEY` is not configured.

### POST /api/hpc/workers/{id}/credentials/rotate {.unnumbered}

Issues a replacement credential for a worker identity. The old credential is revoked immediately. There is no grace period.

**Response:** `200 OK` with the same response shape as `credentials/issue` and the new credential secret shown once.

**Error responses:** `503 Service Unavailable` when `_SYSTEM_.MOLGENIS_HPC_CREDENTIALS_KEY` is not configured.

### POST /api/hpc/workers/{id}/credentials/{credentialId}/revoke {.unnumbered}

Revokes a worker credential immediately.

**Response:** `200 OK` with credential metadata (no secret returned).

```json
{
  "id": "f7632056-1a2b-4727-b0c7-3bdf18aa75dd",
  "worker_id": "hpc-headnode-01",
  "label": "slurm-headnode-01",
  "status": "REVOKED",
  "created_at": "2026-02-21T10:00:00",
  "created_by": "admin",
  "last_used_at": "2026-02-21T10:30:00",
  "revoked_at": "2026-02-21T12:00:00",
  "expires_at": "2026-12-31T23:59:59"
}
```

Subsequent worker requests signed with that credential MUST fail with `401 Unauthorized`.

### GET /api/hpc/workers/{id}/credentials {.unnumbered}

Lists credential metadata for a worker. Secrets are never returned.

**Response:** `200 OK`

```json
{
  "items": [
    {
      "id": "f7632056-1a2b-4727-b0c7-3bdf18aa75dd",
      "worker_id": "hpc-headnode-01",
      "label": "slurm-headnode-01",
      "status": "ACTIVE",
      "created_at": "2026-02-21T10:00:00",
      "created_by": "admin",
      "last_used_at": "2026-02-21T10:30:00",
      "revoked_at": null,
      "expires_at": "2026-12-31T23:59:59"
    }
  ],
  "count": 1,
  "_links": {
    "self": { "href": "/api/hpc/workers/hpc-headnode-01/credentials", "method": "GET" }
  }
}
```

### Worker Credential Bootstrap Flow {.unnumbered}

Credential issue/rotate for a `worker_id` MUST ensure a worker identity row
exists in `HpcWorkers`, even before the first successful
`POST /api/hpc/workers/register`.

Observed capabilities and heartbeat metadata are populated by
`POST /api/hpc/workers/register`.

Operational setup steps are intentionally documented in the canonical quick
start: [apps/hpc/README.md](../README.md).

### DELETE /api/hpc/workers/{id} {.unnumbered}

Removes a worker, its capabilities, and all worker credentials. Jobs previously assigned to this worker retain their history but have their `worker_id` nullified.
Worker removal is explicit-only via this endpoint; heartbeat timeout does not remove workers.
This endpoint is user-only (`Manager`/`Owner`); worker HMAC principals cannot call it.

**Response:** `204 No Content`, `404 Not Found`.

## A.2 Jobs API {.unnumbered}

### POST /api/hpc/jobs {.unnumbered}

Creates a new job in PENDING status.

```json
{
  "processor": "text-embedding:v3",
  "profile": "gpu-medium",
  "parameters": { "model": "multilingual-e5-large", "batch_size": 256 },
  "inputs": [{ "artifact_id": "corpus-01" }]
}
```

`submit_user` is server-derived from the authenticated principal and is not accepted from the client payload.

**Response:** `201 Created`

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING",
  "_links": {
    "self": { "href": "/api/hpc/jobs/550e8400-...", "method": "GET" },
    "claim": { "href": "/api/hpc/jobs/550e8400-.../claim", "method": "POST" },
    "cancel": { "href": "/api/hpc/jobs/550e8400-.../cancel", "method": "POST" }
  }
}
```

### GET /api/hpc/jobs {.unnumbered}

Lists jobs with optional filtering and pagination. Query parameters: `status`, `processor`, `profile`, `limit` (default 100), `offset` (default 0). When `status` is omitted, the default SHOULD be `PENDING` (optimized for worker polling).

**Response:** `200 OK` with paginated result.

```json
{
  "items": [ { "id": "...", "status": "PENDING", "processor": "text-embedding:v3", ... } ],
  "count": 2,
  "total_count": 42,
  "limit": 100,
  "offset": 0,
  "_links": { "self": { "href": "/api/hpc/jobs", "method": "GET" } }
}
```

### POST /api/hpc/jobs/{id}/claim {.unnumbered}

Atomically claims a job. After the atomic claim succeeds, EMX2 verifies that the claiming worker has a registered capability matching the job's `(processor, profile)`. If the worker lacks a matching capability, the claim is rolled back (job returns to PENDING) and the server returns `409 Conflict` with a capability mismatch message. This prevents misconfigured workers from claiming jobs they cannot execute. Returns `409 Conflict` if already claimed or capability mismatch, `404` if not found.

No request body fields are required. Worker identity is derived from `X-Worker-Id`.

Authentication failure on the worker credential returns `401 Unauthorized`.

**Response:** `200 OK` with the job in CLAIMED state, including `_links` for `submit` and `cancel`.

```json
{
  "id": "550e8400-...",
  "status": "CLAIMED",
  "worker_id": "hpc-headnode-01",
  "processor": "text-embedding:v3",
  "profile": "gpu-medium",
  "_links": {
    "self": { "href": "/api/hpc/jobs/550e8400-...", "method": "GET" },
    "transitions": {
      "href": "/api/hpc/jobs/550e8400-.../transitions",
      "method": "GET"
    },
    "submit": {
      "href": "/api/hpc/jobs/550e8400-.../transition",
      "method": "POST"
    },
    "cancel": { "href": "/api/hpc/jobs/550e8400-.../cancel", "method": "POST" }
  }
}
```

### POST /api/hpc/jobs/{id}/transition {.unnumbered}

Reports a state transition. MUST reject invalid transitions with `409 Conflict`. Idempotent: re-posting an identical transition returns `200 OK`. Response includes the updated job.

Authentication failure on the worker credential returns `401 Unauthorized`.

Optional structured progress fields are accepted on transition and completion payloads:

- `phase` (string, max 100 chars)
- `message` (string, max 500 chars)
- `progress` (number in `[0.0, 1.0]`)

**SUBMITTED** (head node, after `sbatch`):

```json
{
  "status": "SUBMITTED",
  "detail": "sbatch id 45678",
  "slurm_job_id": "45678"
}
```

**STARTED** (daemon monitor):

```json
{
  "status": "STARTED",
  "detail": "running on node-05"
}
```

**STARTED progress update** (same-state transition):

```json
{
  "status": "STARTED",
  "detail": "progress: sorting; step 3 of 10; 30%",
  "phase": "sorting",
  "message": "step 3 of 10",
  "progress": 0.3
}
```

**COMPLETED** (after outputs committed):

```json
{
  "status": "COMPLETED",
  "detail": "exit code 0",
  "output_artifact_id": "art_abc123-..."
}
```

**FAILED** (head node or daemon):

```json
{
  "status": "FAILED",
  "detail": "input_hash_mismatch"
}
```

### POST /api/hpc/jobs/{id}/cancel {.unnumbered}

Convenience endpoint for cancellation. Transitions the job to CANCELLED from any non-terminal state. `Editor` callers may cancel only their own jobs (`submit_user` match). `Manager` callers may cancel any job. The head node SHOULD issue `scancel` if a Slurm job ID is known.

**Response:** `200 OK` with updated job, `409 Conflict` if already terminal.

### DELETE /api/hpc/jobs/{id} {.unnumbered}

Deletes a job and its transition history. The job MUST be in a terminal state (COMPLETED, FAILED, or CANCELLED); otherwise the server MUST return `409 Conflict`. `Editor` callers may delete only their own jobs (`submit_user` match). `Manager` callers may delete any terminal job.

**Response:** `204 No Content`, `404 Not Found`.

### GET /api/hpc/jobs/{id}/transitions {.unnumbered}

Ordered transition history (audit log).

```json
{
  "items": [
    {
      "id": "tr_001",
      "from_status": null,
      "to_status": "PENDING",
      "timestamp": "2026-02-21T10:28:00",
      "worker_id": null,
      "detail": "Job created",
      "phase": null,
      "message": null,
      "progress": null
    },
    {
      "id": "tr_002",
      "from_status": "PENDING",
      "to_status": "CLAIMED",
      "timestamp": "2026-02-21T10:29:00",
      "worker_id": "hpc-headnode-01",
      "detail": "Claimed by worker hpc-headnode-01",
      "phase": null,
      "message": null,
      "progress": null
    },
    {
      "id": "tr_003",
      "from_status": "STARTED",
      "to_status": "STARTED",
      "timestamp": "2026-02-21T10:29:30",
      "worker_id": "hpc-headnode-01",
      "detail": "progress: sorting; step 3 of 10; 30%",
      "phase": "sorting",
      "message": "step 3 of 10",
      "progress": 0.3
    }
  ],
  "count": 3
}
```

### Error response example {.unnumbered}

Follows RFC 9457 (Problem Details for HTTP APIs) structure.

```json
{
  "title": "Conflict",
  "status": 409,
  "detail": "Cannot transition job 550e8400-... from PENDING to STARTED"
}
```

## A.3 Artifact API {.unnumbered}

### POST /api/hpc/artifacts {.unnumbered}

Creates an artifact. Managed artifacts start in CREATED; external artifacts (posix, s3, http, reference) start in REGISTERED.

**Managed:** `{ "name": "my-dataset", "type": "parquet", "residence": "managed" }`

**NFS:** `{ "name": "output-abc123", "type": "blob", "residence": "posix", "content_url": "file:///nfs/outputs/job-123" }`

**S3:** `{ "name": "analysis-results", "type": "parquet", "residence": "s3", "content_url": "s3://..." }`

**Response:** `201 Created`

```json
{
  "id": "art_abc123-...",
  "name": "my-dataset",
  "type": "parquet",
  "status": "CREATED",
  "_links": {
    "self": { "href": "/api/hpc/artifacts/art_abc123-...", "method": "GET" },
    "upload": {
      "href": "/api/hpc/artifacts/art_abc123-.../files/{path}",
      "method": "PUT"
    },
    "files": {
      "href": "/api/hpc/artifacts/art_abc123-.../files",
      "method": "GET"
    }
  }
}
```

### GET /api/hpc/artifacts/{id} {.unnumbered}

Returns full metadata with HATEOAS links. Links vary by status: CREATED/UPLOADING include `upload` and (for UPLOADING) `commit`; COMMITTED includes `download`; all include `files`.

**Managed artifact (committed):**

```json
{
  "id": "art_abc",
  "name": "my-dataset",
  "type": "parquet",
  "residence": "managed",
  "status": "COMMITTED",
  "sha256": "b3a3f0...",
  "size_bytes": 52428800,
  "content_url": null,
  "created_at": "2026-02-21T10:00:00",
  "committed_at": "2026-02-21T10:05:00",
  "_links": {
    "self": { "href": "/api/hpc/artifacts/art_abc", "method": "GET" },
    "download": {
      "href": "/api/hpc/artifacts/art_abc/files/{path}",
      "method": "GET"
    },
    "files": { "href": "/api/hpc/artifacts/art_abc/files", "method": "GET" }
  }
}
```

**NFS artifact:** `"residence": "posix"`, `"content_url": "file:///nfs/data/outputs/embeddings.parquet"`

**S3 artifact:** `"residence": "s3"`, `"content_url": "s3://data-lake/outputs/analysis.parquet"`

### PUT /api/hpc/artifacts/{id}/files/{path} {.unnumbered}

Uploads a file to an artifact by path. The `{path}` segment is the logical file name within the artifact (e.g. `data.parquet`, `model/weights.bin`). Upserts: if a file already exists at that path, it is replaced. Transitions the artifact from CREATED to UPLOADING on the first upload.

Accepts three body formats:

- **Raw binary** (preferred): `Content-Type` describes the file's media type; body is the raw file bytes. The server computes SHA-256 and stores the file.
- **Multipart**: `Content-Type: multipart/form-data` with a `file` part and optional `content_type` form param.
- **JSON metadata-only**: `Content-Type: application/json` with `{ "sha256": "...", "size_bytes": ..., "content_type": "..." }`. Registers file metadata without storing binary content. Used for posix/external artifacts where files reside on the filesystem.

**Request** (raw binary):

```
PUT /api/hpc/artifacts/art_abc/files/data.parquet
Content-Type: application/vnd.apache.parquet
X-EMX2-API-Version: 2025-01
...

<raw file bytes>
```

**Response:** `201 Created`

```json
{
  "id": "file-uuid-...",
  "artifact_id": "art_abc",
  "path": "data.parquet",
  "sha256": "b3a3f0...",
  "size_bytes": 52428800
}
```

For non-JSON request bodies (raw binary uploads), the client MUST include a `Content-SHA256` header containing the hex-encoded SHA-256 digest of the upload body. This is mandatory even without HMAC. When HMAC is enabled, the same value is used as the body hash component in the HMAC canonical string (see §8). The server verifies both the declared body hash and, when present, the HMAC signature.

### GET /api/hpc/artifacts/{id}/files/{path} {.unnumbered}

Downloads file content. Returns the raw bytes with appropriate `Content-Type`, `Content-Disposition: attachment`, `Content-Length`, and `X-Content-SHA256` headers.

For managed artifacts with stored content, serves bytes directly. For posix/external artifacts where the file metadata exists but no binary is stored, returns `302 Found` redirecting to `{content_url}/{path}`.

**Response headers:**

```
Content-Type: application/vnd.apache.parquet
Content-Disposition: attachment; filename="data.parquet"
Content-Length: 52428800
X-Content-SHA256: b3a3f0...
```

### HEAD /api/hpc/artifacts/{id}/files/{path} {.unnumbered}

Returns file metadata as headers without body content. Useful for checking existence and integrity without downloading.

**Response headers:** `X-Content-SHA256`, `Content-Length`, `Content-Type`. Status `200 OK` if found, `404 Not Found` otherwise.

### DELETE /api/hpc/artifacts/{id}/files/{path} {.unnumbered}

Deletes a file from an artifact. Only allowed when the artifact is not yet COMMITTED.

**Response:** `204 No Content` on success, `409 Conflict` if artifact is committed, `404 Not Found` if file does not exist.

### GET /api/hpc/artifacts/{id}/files {.unnumbered}

Lists files in an artifact with pagination and optional prefix filtering.

**Query parameters:** `prefix` (filter paths starting with this string), `limit` (default 100), `offset` (default 0).

**Response:** `200 OK`

```json
{
  "items": [
    {
      "id": "file-uuid-...",
      "path": "data.parquet",
      "sha256": "b3a3f0...",
      "size_bytes": 52428800,
      "content_type": "application/vnd.apache.parquet",
      "_links": {
        "content": {
          "href": "/api/hpc/artifacts/art_abc/files/data.parquet",
          "method": "GET"
        }
      }
    }
  ],
  "count": 1,
  "total_count": 1,
  "limit": 100,
  "offset": 0
}
```

### POST /api/hpc/artifacts/{id}/commit {.unnumbered}

Commits the artifact with a top-level SHA-256 hash and total size. The artifact MUST be in UPLOADING (managed) or REGISTERED (external) status. Immutable after commit — subsequent uploads and deletes MUST be rejected.

**Request:** `{ "sha256": "abc123...", "size_bytes": 1024 }`

**Response:** `200 OK` with full artifact metadata.

### Artifact examples by residence {.unnumbered}

**Multi-file model on NFS (posix):**

```json
{
  "id": "art_model_nfs",
  "name": "llama-3-8b",
  "type": "gguf",
  "residence": "posix",
  "status": "COMMITTED",
  "sha256": "d1e2f3...",
  "content_url": "file:///nfs/models/llama-3-8b/"
}
```

File listing for this artifact:

```json
{
  "items": [
    { "path": "model.gguf", "size_bytes": 4294967296, "sha256": "a1b2c3..." },
    { "path": "tokenizer.json", "size_bytes": 524288, "sha256": "d4e5f6..." }
  ]
}
```

For posix artifacts, the daemon registers file metadata without binary content. Consumers access files directly via the NFS mount at the `content_url` path. The GET file endpoint returns a `302` redirect to `{content_url}/{path}` for files without stored binary content.

# Appendix B: Sequence Diagram {.unnumbered}

![](./seq.pdf)
