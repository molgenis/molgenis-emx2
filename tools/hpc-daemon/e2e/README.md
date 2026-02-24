# HPC Bridge E2E Tests

End-to-end tests for the EMX2 HPC execution bridge, exercising the full flow:
**EMX2 API -> daemon -> Slurm -> artifacts**.

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
- The `emx2-hpc-daemon` package (installed via uv from the synced source)

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

# Full cycle: provision VM + run tests
make e2e
```

This will:

1. Start a QEMU VM via Vagrant and run `provision.sh`
2. Sync the daemon source into the VM and install it
3. Run the pytest suite against the EMX2 API on the host

## How it works

1. **Bootstrap** (`conftest.py`): signs in to EMX2 as admin, sets the
   `MOLGENIS_HPC_SHARED_SECRET` setting, and makes an HMAC-authenticated
   request to trigger lazy initialisation of HPC tables.

2. **Daemon** runs inside the VM as a cron job (every ~20s via the
   sleep-staggered cron trick). Each invocation runs a single
   `emx2-hpc-daemon once` cycle: register worker, recover in-flight jobs
   from persistent state (TinyDB), poll for pending jobs, claim, submit to
   Slurm, monitor running jobs, upload artifacts, report transitions.

3. **Tests** submit jobs via the HPC API from the host, then poll the job
   status until it reaches the expected terminal state. The daemon (driven by
   cron) processes the jobs independently.

4. **Job scripts** in `scripts/` are simple bash scripts that run inside
   Slurm (no containers):
   - `e2e_job.sh` -- writes `result.txt` with the job ID (happy path)
   - `e2e_job_fail.sh` -- exits with code 1 (failure path)
   - `e2e_job_slow.sh` -- sleeps, writes progress updates (cancellation path)

## Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `EMX2_BASE_URL` | `http://localhost:8080` | URL of the running EMX2 instance (host-side) |
| `HPC_SHARED_SECRET` | `e2e-test-secret-do-not-use-in-production` | HMAC shared secret (set in EMX2 by test bootstrap) |

The VM's Vagrantfile translates `EMX2_BASE_URL` to `http://10.0.2.2:8080`
(QEMU user-mode networking gateway) by default.

## Makefile targets

| Target | Description |
|--------|-------------|
| `make up` | Start the Vagrant VM |
| `make test` | Sync daemon source + run pytest (EMX2 and VM must be running) |
| `make e2e` | Full cycle: `up` + `test` (prints logs on failure) |
| `make sync` | Sync daemon source to VM and reinstall |
| `make status` | Show Slurm + daemon status inside the VM |
| `make logs` | Show daemon logs from the VM |
| `make down` | Halt the VM (preserves state) |
| `make clean` | Destroy the VM completely |

## Test cases

| File | What it tests |
|------|---------------|
| `test_01_worker.py` | Worker registration, capabilities, heartbeat |
| `test_02_lifecycle.py` | Happy path: submit -> COMPLETED -> download artifacts |
| `test_03_failure.py` | Job exits non-zero -> FAILED + log artifact uploaded |
| `test_04_cancellation.py` | Cancel via API -> scancel propagation |

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

### Common issues

**Node shows `idle*`**: slurmd lost contact with slurmctld. Check time
sync (`chronyc tracking`) and MUNGE (`munge -n | unmunge`). Restart slurmd.

**Jobs stuck in PENDING with `(InvalidAccount)`**: Slurm accounting is not
set up. Verify `sacctmgr list assoc` shows the `vagrant` user with the
`default` account. Re-provision if needed.

**Jobs stuck in COMPLETING (CG)**: Usually caused by a UID mismatch
(`SlurmUser` in `slurm.conf` must match the user running slurmctld).
Clear spool state and restart:
```bash
sudo systemctl stop slurmctld slurmd
sudo rm -f /var/spool/slurmctld/job_state* /var/spool/slurmctld/node_state*
sudo systemctl start slurmctld slurmd
```
