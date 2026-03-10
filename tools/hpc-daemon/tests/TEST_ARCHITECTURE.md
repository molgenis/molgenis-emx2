# HPC Test Architecture

Canonical behavior source: `apps/hpc/doc/design.md`
Protocol contract: `protocol/hpc-protocol.json`

## Suite Layout

```
backend/molgenis-emx2-hpc/src/test/java/org/molgenis/emx2/hpc/
├── protocol/                 # Auth, headers, validation, contract conformance
│   ├── HmacVerifierTest.java
│   ├── InputValidatorTest.java
│   └── HpcApiContractTest.java     # ← protocol/hpc-protocol.json conformance
└── service/                  # Behavior-focused integration tests
    ├── ArtifactServiceTest.java
    ├── ArtifactServiceIntegrationTest.java
    ├── JobServiceIntegrationTest.java
    ├── WorkerServiceIntegrationTest.java
    └── HpcServiceIntegrationTestBase.java

backend/molgenis-emx2-webapi/src/test/java/org/molgenis/emx2/web/
├── HpcApiE2ETest.java                 # API integration: lifecycle, claims, immutability
└── HpcApiAuthRotationE2ETest.java     # HMAC secret rotation/disable behavior

tools/hpc-daemon/
├── tests/                    # Python unit tests (no network, no Slurm)
│   ├── conftest.py           # Shared fixtures
│   ├── mock_slurm.py         # Mock Slurm cluster
│   ├── test_backend.py       # Backend strategy + input normalization
│   ├── test_client.py        # HMAC signing
│   ├── test_contract.py      # protocol/hpc-protocol.json conformance
│   ├── test_daemon_scenarios.py # End-to-end daemon scenarios
│   ├── test_mock_slurm.py    # Slurm module integration via mock
│   ├── test_profiles.py      # Profile resolution + capabilities
│   ├── test_shell_backend.py # ShellBackend: submit, query, cancel, env vars
│   ├── test_slurm.py         # Batch script generation
│   └── test_tracker.py       # SQLite state persistence
└── e2e/                      # Real Slurm (Vagrant VM + EMX2)
    ├── conftest.py            # Session fixtures, wait helpers
    ├── test_01_worker.py      # Daemon cycle, auth, heartbeat
    ├── test_02_lifecycle.py   # Happy path: PENDING → COMPLETED
    ├── test_03_failure.py     # Job failure + log artifact
    ├── test_04_cancellation.py # Cancel propagation to Slurm
    ├── test_05_posix_artifacts.py # POSIX residence roundtrip
    ├── test_06_artifact_roundtrip.py # Managed upload → transform → download
    ├── test_07_delete_requires_cancel.py # NEW: DELETE rejects non-terminal
    └── scripts/               # Entrypoint scripts for test profiles
```

## Requirement Matrix

Requirements extracted from `design.md` and `protocol/hpc-protocol.json`.
Level: M=MUST, S=SHOULD, Y=MAY.

### Job Lifecycle

| ID | Requirement | Level | Java Test | Python Test | E2E Test |
|----|-------------|-------|-----------|-------------|----------|
| REQ-JOB-STATE-001 | Jobs follow state machine: PENDING→CLAIMED→SUBMITTED→STARTED→{COMPLETED,FAILED,CANCELLED} | M | JobServiceIntegrationTest, HpcApiE2ETest | test_contract | test_02 |
| REQ-JOB-STATE-002 | Terminal states (COMPLETED, FAILED, CANCELLED) have no outgoing transitions | M | JobServiceIntegrationTest | test_contract | — |
| REQ-JOB-STATE-003 | Invalid transitions MUST return 409 Conflict | M | JobServiceIntegrationTest, HpcApiE2ETest | — | — |
| REQ-JOB-CLAIM-001 | Claim MUST be atomic: only one worker succeeds | M | HpcApiE2ETest (claimIsAtomicUnderRace) | — | — |
| REQ-JOB-CLAIM-002 | Claim MUST verify worker has matching capability | M | HpcApiE2ETest (claimRejectsWorkerWithoutMatchingCapability) | — | — |
| REQ-JOB-CANCEL-001 | Cancel from any non-terminal state → CANCELLED | M | HpcApiE2ETest | test_daemon_scenarios | test_04 |
| REQ-JOB-DELETE-001 | DELETE on non-terminal job MUST return 409 | M | HpcApiE2ETest (deleteNonTerminalJobReturnsConflict) | — | test_07 ✓ |
| REQ-JOB-DELETE-002 | DELETE on terminal job deletes job + transitions | M | — | — | test_07 ✓ |
| REQ-JOB-TIMEOUT-001 | EMX2 expires stale CLAIMED/STARTED jobs lazily | M | — | — | — |
| REQ-JOB-IDEMPOTENT-001 | Duplicate transitions SHOULD return 200 OK | S | JobServiceIntegrationTest | — | — |
| REQ-JOB-IDEMPOTENT-002 | Non-identical submissions to same state MUST return 409 | M | — | — | — |
| REQ-JOB-TRANSITION-001 | Transition history is ordered and auditable | M | HpcApiE2ETest (transitionAuditTrail) | — | test_02 |
| REQ-JOB-INPUT-001 | Inputs accept array of strings | M | — | test_backend, test_daemon_scenarios | — |
| REQ-JOB-INPUT-002 | Inputs accept array of objects with artifact_id | M | — | test_backend, test_daemon_scenarios | test_06 |
| REQ-JOB-INPUT-003 | Inputs accept named-reference objects | M | — | test_backend, test_daemon_scenarios | — |

### Artifacts

| ID | Requirement | Level | Java Test | Python Test | E2E Test |
|----|-------------|-------|-----------|-------------|----------|
| REQ-ART-STATE-001 | Managed: CREATED→UPLOADING→COMMITTED | M | ArtifactServiceIntegrationTest | test_contract | test_06 |
| REQ-ART-STATE-002 | External: REGISTERED→COMMITTED | M | ArtifactServiceIntegrationTest | — | — |
| REQ-ART-STATE-003 | Stale CREATED/UPLOADING expire to FAILED | M | — | — | — |
| REQ-ART-HASH-001 | Single-file: SHA-256 of file bytes | M | ArtifactServiceTest, ArtifactServiceIntegrationTest | test_daemon_scenarios | test_06 |
| REQ-ART-HASH-002 | Multi-file: tree hash SHA256(concat(sorted path:sha256)) | M | ArtifactServiceTest, ArtifactServiceIntegrationTest | test_daemon_scenarios | — |
| REQ-ART-HASH-003 | Commit verifies client hash against computed hash | M | — | — | test_06 |
| REQ-ART-IMMUTABLE-001 | Committed artifacts MUST NOT be modified | M | HpcApiE2ETest (committedArtifactRejectsFurtherMutation) | — | — |
| REQ-ART-FILE-001 | Files are path-addressed within artifacts | M | — | — | test_06 |
| REQ-ART-FILE-002 | Cannot delete files from committed artifact | M | HpcApiE2ETest (committedArtifactRejectsFurtherMutation) | — | — |
| REQ-ART-FILE-003 | Cannot add files to COMMITTED artifact | M | HpcApiE2ETest (committedArtifactRejectsFurtherMutation) | — | — |
| REQ-ART-RESIDENCE-001 | Managed: binary content in EMX2 FILE columns | M | — | — | test_06 |
| REQ-ART-RESIDENCE-002 | POSIX: file:// path, symlink on staging | M | — | test_daemon_scenarios | test_05 |
| REQ-ART-LOG-001 | Daemon uploads execution logs as type "log" | S | — | test_daemon_scenarios | test_02 |

### Authentication

| ID | Requirement | Level | Java Test | Python Test | E2E Test |
|----|-------------|-------|-----------|-------------|----------|
| REQ-AUTH-HMAC-001 | HMAC signs METHOD\nPATH\nSHA256(body)\nTIMESTAMP\nNONCE | M | HmacVerifierTest | test_client | test_01 |
| REQ-AUTH-HMAC-002 | Timestamp drift > 5 min MUST be rejected | M | HmacVerifierTest | — | — |
| REQ-AUTH-HMAC-003 | Replayed nonce MUST be rejected | M | HmacVerifierTest | — | — |
| REQ-AUTH-HMAC-004 | Secret < 32 chars MUST be rejected | M | HmacVerifierTest | — | — |
| REQ-AUTH-CASCADE-001 | Auth cascade: HMAC → JWT → session → 401 | M | — | — | test_01 |
| REQ-AUTH-HEADER-001 | X-EMX2-API-Version required | M | HpcApiContractTest | test_contract | — |
| REQ-AUTH-HEADER-002 | X-Request-Id required | M | — | — | — |
| REQ-AUTH-SHA256-001 | Content-SHA256 verified on upload if present | M | HmacVerifierTest | — | test_06 |

### Workers

| ID | Requirement | Level | Java Test | Python Test | E2E Test |
|----|-------------|-------|-----------|-------------|----------|
| REQ-WORKER-REG-001 | Register upserts worker + replaces capabilities | M | — | — | test_01 |
| REQ-WORKER-HEARTBEAT-001 | Heartbeat updates last_heartbeat_at | M | — | — | test_01 |
| REQ-WORKER-STALE-001 | Workers without heartbeat > threshold are removed | M | HpcApiE2ETest (staleWorkersAreExpiredDuringPolling) | — | — |
| REQ-WORKER-DELETE-001 | Delete nullifies worker_id on associated jobs | M | — | — | — |

### API Shape (from protocol/hpc-protocol.json)

| ID | Requirement | Level | Java Test | Python Test | E2E Test |
|----|-------------|-------|-----------|-------------|----------|
| REQ-PROTO-STATUS-001 | Job statuses match spec enum | M | HpcApiContractTest | test_contract | — |
| REQ-PROTO-STATUS-002 | Artifact statuses match spec enum | M | HpcApiContractTest | test_contract | — |
| REQ-PROTO-TRANS-001 | Transitions match spec mapping | M | HpcApiContractTest | test_contract | — |
| REQ-PROTO-ERROR-001 | Errors use RFC 9457 ProblemDetail shape | M | HpcApiE2ETest (invalidTransitionPendingToCompleted) | — | — |
| REQ-PROTO-HATEOAS-001 | Responses include _links advertising legal actions | M | HpcApiE2ETest | — | — |
| REQ-PROTO-HATEOAS-002 | Links absent for illegal transitions | M | HpcApiE2ETest | — | — |

### Daemon Behavior

| ID | Requirement | Level | Java Test | Python Test | E2E Test |
|----|-------------|-------|-----------|-------------|----------|
| REQ-DAEMON-STAGE-001 | Managed artifacts downloaded via GET | M | — | test_backend, test_shell_backend | test_06 |
| REQ-DAEMON-STAGE-002 | POSIX artifacts symlinked from file:// path | M | — | test_backend | test_05 |
| REQ-DAEMON-STAGE-003 | Hash verified after staging | M | — | test_backend | — |
| REQ-DAEMON-PROGRESS-001 | Read last line of .hpc_progress.jsonl (NDJSON) | S | — | — | — |
| REQ-DAEMON-PROGRESS-002 | Validate/sanitize progress fields | S | — | — | — |
| REQ-DAEMON-ENV-001 | Wrapper gets HPC_JOB_ID, HPC_INPUT_DIR, HPC_OUTPUT_DIR, HPC_WORK_DIR, HPC_PARAMETERS | M | — | test_slurm, test_shell_backend ✓ | test_02 |
| REQ-DAEMON-RECOVER-001 | Daemon recovers tracked jobs on restart | S | — | test_tracker | — |
| REQ-DAEMON-OUTPUT-001 | Output files classified: logs vs output | M | — | test_daemon_scenarios | — |
| REQ-DAEMON-UPLOAD-001 | Managed output uploaded + committed with tree hash | M | — | test_daemon_scenarios | test_06 |
| REQ-DAEMON-UPLOAD-002 | POSIX output registered + committed with tree hash | M | — | test_daemon_scenarios | test_05 |

### Validation

| ID | Requirement | Level | Java Test | Python Test | E2E Test |
|----|-------------|-------|-----------|-------------|----------|
| REQ-VALID-UUID-001 | Job/artifact IDs must be valid UUIDs | M | InputValidatorTest | — | — |
| REQ-VALID-PATH-001 | File paths reject traversal (../) | M | InputValidatorTest | — | — |
| REQ-VALID-PATH-002 | File paths reject absolute paths | M | InputValidatorTest | — | — |
| REQ-VALID-PATH-003 | File paths reject null bytes | M | InputValidatorTest | — | — |
| REQ-VALID-URL-001 | POSIX content_url must be absolute file:// path | M | InputValidatorTest | — | — |

## Coverage Gaps (current)

| ID | Gap | Priority |
|----|-----|----------|
| — | Previously identified high/medium gaps are covered by tests in this matrix revision. | — |

## Test Commands

```bash
# Java tests
./gradlew :backend:molgenis-emx2-hpc:test
./gradlew :backend:molgenis-emx2-webapi:test --tests org.molgenis.emx2.web.HpcApiE2ETest

# Python unit tests
cd tools/hpc-daemon && uv run pytest tests/ -v

# E2E tests (requires Vagrant VM + EMX2)
cd tools/hpc-daemon/e2e && make e2e
```
