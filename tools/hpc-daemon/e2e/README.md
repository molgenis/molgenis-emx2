# HPC Bridge E2E Tests

End-to-end tests for the EMX2 HPC execution bridge, exercising the full flow:
**EMX2 API -> daemon -> Slurm -> artifacts**.
This directory is the system-truth layer for real Slurm behavior.

## Read First

For operational onboarding (enable HPC, issue worker credentials, run daemon
outside this test harness), start with:

- [apps/hpc/README.md](../../../apps/hpc/README.md)

This README is only for the real-Slurm VM e2e test system. It covers both the
host-entrypoint execution path and the Apptainer execution path.

## Architecture

```
┌──────────────────────────────┐
│  pytest (host)               │
│  submits jobs, asserts state │
└──────────────┬───────────────┘
               │ HTTP (HMAC auth)
               ▼
┌──────────────────────────────┐
│  EMX2 (host, :8080)         │
│  Java backend with HPC API  │
└──────────────┬───────────────┘
               │ polls / reports
    ┌──────────┴───────────┐
    │  Vagrant VM (QEMU)   │
    │  slurmctld + slurmd  │
    │  slurmdbd + MariaDB  │
    │  hpc-daemon (cron)   │
    └──────────────────────┘
```

The Vagrant VM runs a single-node Slurm cluster with full accounting
(slurmdbd + MariaDB) and the HPC daemon invoked every ~20 seconds via cron.
Tests also trigger explicit daemon cycles to keep orchestration deterministic.
EMX2 runs on the host. Tests run on the host against the EMX2 API.

## Prerequisites

### Host dependencies

| Dependency | Purpose | Install |
|------------|---------|---------|
| [Vagrant](https://www.vagrantup.com/) >= 2.4 | VM lifecycle management | [Download](https://developer.hashicorp.com/vagrant/install) |
| [vagrant-qemu](https://github.com/ppggff/vagrant-qemu) plugin | QEMU provider for Vagrant | `vagrant plugin install vagrant-qemu` |
| [QEMU](https://www.qemu.org/) >= 8.0 | Virtual machine hypervisor | `apt install qemu-system-x86` / `brew install qemu` |
| [uv](https://docs.astral.sh/uv/) >= 0.4 | Python package manager | `curl -LsSf https://astral.sh/uv/install.sh \| sh` |
| Python >= 3.11 | Test runner | via uv or system |

On Linux with KVM, QEMU runs with hardware acceleration. On macOS, the
vagrant-qemu plugin uses HVF (Apple Hypervisor Framework).

### VM provisions automatically

The `provision.sh` script installs these inside the VM (no host action needed):

- [Slurm](https://slurm.schedmd.com/) 23.11 (slurmctld, slurmd, slurmdbd)
- [MariaDB](https://mariadb.org/) for Slurm accounting (required by `select/cons_tres` in Slurm >= 23.11)
- [Munge](https://dun.github.io/munge/) for Slurm authentication
- [Chrony](https://chrony-project.org/) for time synchronisation (prevents MUNGE credential expiry in VMs)
- [Apptainer](https://apptainer.org/) plus a tiny local sandbox image used for deterministic container e2e coverage
- The `emx2-hpc-daemon` package (installed via uv from the synced source)

### Worker credential secret

The tests authenticate to EMX2 using HMAC-SHA256 with per-worker credentials.
The fixture bootstrap rotates a credential for worker `e2e-test-worker` and
syncs the returned secret to both:

1. `tools/hpc-daemon/.secret` on the host
2. `/etc/hpc-daemon/secret` in the VM

Requirements:

1. `MOLGENIS_HPC_ENABLED=true` on `_SYSTEM_`
2. `MOLGENIS_HPC_CREDENTIALS_KEY` set on `_SYSTEM_`
3. admin signin must work (defaults `admin/admin`, overridable via
   `EMX2_ADMIN_EMAIL` and `EMX2_ADMIN_PASSWORD`)

For manual credential/bootstrap flows, see
[apps/hpc/README.md](../../../apps/hpc/README.md).

### Running EMX2

A running EMX2 instance is required. Start it from the repo root:

```bash
./gradlew run
# or
docker compose up emx2
```

The VM connects to the host via QEMU's default gateway (`10.0.2.2:8080`).

## Quick start

```bash
cd tools/hpc-daemon/e2e

# Full deterministic cycle: reprovision VM + run tests
make e2e
```

This will:

1. Ensure VM is up and run `vagrant provision` with current e2e config
   (including Slurm
   smoke tests that verify the cluster works before installing the daemon)
2. Sync the daemon source into the VM and install it
3. Run a strict VM preflight (clock sync + Slurm/Munge/cron health)
4. Rotate/sync the e2e worker credential secret
5. Run the pytest suite against the EMX2 API on the host

## How it works

1. **Bootstrap** (`conftest.py`): uses existing `tools/hpc-daemon/.secret`
   first and verifies it. If invalid/missing, it rotates a worker credential
   for `e2e-test-worker`, writes/syncs the new secret to
   `tools/hpc-daemon/.secret` and `/etc/hpc-daemon/secret`, then makes an
   authenticated request to trigger lazy initialisation of HPC tables.

2. **Daemon** runs inside the VM as a cron job (every ~20s via the
   sleep-staggered cron trick). Each invocation runs a single
   `emx2-hpc-daemon once` cycle: register worker, recover in-flight jobs
   from persistent state (SQLite), poll for pending jobs, claim, submit to
   Slurm, monitor running jobs, upload artifacts, report transitions.

3. **Tests** submit jobs via the HPC API from the host, then poll the job
   status until it reaches the expected terminal state. The daemon (driven by
   cron) processes the jobs independently.

4. **Job scripts** in `scripts/` are simple bash scripts that run inside
   Slurm via the `host_entrypoint` execution mode (no containers):
   - `e2e_job.sh` -- writes `result.txt` and `result.json` (happy path)
   - `e2e_job_fail.sh` -- writes to stderr, exits with code 1 (failure path)
   - `e2e_job_slow.sh` -- writes a marker then sleeps 90s (cancellation path)
   - `e2e_job_posix.sh` -- writes `result.txt` + a 1MB `sample.bin` (posix artifact path)
   - `e2e_job_apptainer.sh` -- runs inside the Apptainer sandbox image for the
     real container execution path

## Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `EMX2_BASE_URL` | `http://localhost:8080` | URL of the running EMX2 instance (host-side) |
| `E2E_CLEANUP_POLICY` | `none` | Teardown policy: `non_terminal` (keep history), `all` (delete all created jobs), `none` (no cleanup). |

The VM's Vagrantfile translates `EMX2_BASE_URL` to `http://10.0.2.2:8080`
(QEMU user-mode networking gateway) by default.

The daemon config inside the VM (`/etc/hpc-daemon/daemon-config.yaml`) defines
profiles for happy-path, failure, slow/cancellation, posix, transform, and a
real Apptainer-backed execution path. Nested-output behavior is exercised by
the happy-path profile script. Managed and posix output residence paths are
both covered.

## Makefile targets

| Target | Description |
|--------|-------------|
| `make up` | Start the Vagrant VM |
| `make ensure-vm` | Ensure VM is running and reprovisioned |
| `make reset-vm` | Destroy and recreate the VM (clean-room state) |
| `make preflight` | Strict VM preflight (clock sync + service readiness) |
| `make refresh-secret` | Rotate/sync worker credential secret to host + VM |
| `make test` | Deterministic run: `ensure-vm` + sync + preflight + secret refresh + pytest |
| `make test-clean` | Hard reset run: `reset-vm` + sync + preflight + secret refresh + pytest |
| `make e2e` | Alias for `make test` (prints diagnostics on failure) |
| `make sync` | Sync daemon source, reinstall daemon, refresh scripts/preflight helper |
| `make status` | Show Slurm + daemon status inside the VM |
| `make logs` | Show daemon logs from the VM |
| `make diagnostics` | Collect Slurm + daemon + clock diagnostics from VM |
| `make down` | Halt the VM (preserves state) |
| `make clean` | Destroy the VM completely |

## Test cases

| File | What it tests |
|------|---------------|
| `test_01_worker.py` | Worker registration, health endpoint, HMAC auth, heartbeat |
| `test_02_lifecycle.py` | Happy path: submit -> COMPLETED -> download output + log artifacts |
| `test_03_failure.py` | Job exits non-zero -> FAILED, plus timeout -> FAILED |
| `test_04_cancellation.py` | Cancellation at CLAIMED, SUBMITTED, STARTED |
| `test_05_posix_artifacts.py` | Posix residence: file metadata registered, content_url set, no binary upload |
| `test_06_artifact_roundtrip.py` | Managed artifact roundtrip with input->transform->output verification |
| `test_07_delete_requires_cancel.py` | DELETE behavior for non-terminal and terminal jobs |
| `test_08_nested_output_paths.py` | Nested output path roundtrip for managed artifacts |
| `test_09_apptainer.py` | Real Apptainer execution with bound input artifact and managed outputs |

## Troubleshooting

```bash
# SSH into the VM
cd vm && vagrant ssh

# Check Slurm cluster state
sinfo                          # node should be 'idle'
squeue                         # running/pending jobs

# Check accounting (vagrant user must have an association)
sacctmgr list assoc format=Cluster,Account,User

# Check daemon logs
tail -f /var/log/hpc-daemon.log

# Check Slurm controller logs (needs sudo)
sudo tail -f /var/log/slurm/slurmctld.log

# Check slurmdbd logs
sudo tail -f /var/log/slurm/slurmdbd.log

# Manually trigger a daemon cycle
/usr/local/bin/hpc-daemon-once

# Re-provision without destroying
cd vm && vagrant provision

# Full rebuild
cd vm && vagrant destroy -f && vagrant up
```

**Time sync issues (macOS sleep/wake):**
QEMU VMs can drift after host sleep and cause HMAC timestamp mismatches.
Use `make preflight` (or `make test`, which includes preflight) to force
chrony step sync and verify skew before running tests. The daemon wrapper
also runs a quick clock preflight before each cron cycle.
