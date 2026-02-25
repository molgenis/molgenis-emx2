#!/bin/bash
# Provision a single-node Slurm cluster + HPC daemon for e2e testing.
set -euo pipefail

export DEBIAN_FRONTEND=noninteractive

echo "=== Installing Slurm + Munge + Chrony + MariaDB ==="
apt-get update -qq
apt-get install -y -qq slurm-wlm slurmdbd munge curl chrony mariadb-server

echo "=== Configuring time sync ==="
systemctl enable chrony
systemctl restart chrony
# QEMU VMs can boot with a wildly wrong hardware clock (unlike KVM which
# passes through the host clock). Force an immediate step correction.
sleep 2
chronyc makestep
for i in $(seq 1 10); do
    if chronyc waitsync 1 0.1 0 0 &>/dev/null; then
        echo "Time synchronized."
        break
    fi
    sleep 1
done
echo "VM time: $(date -u)"

echo "=== Installing uv (system-wide) ==="
if ! command -v uv &>/dev/null; then
    curl -LsSf https://astral.sh/uv/install.sh | env UV_INSTALL_DIR=/usr/local/bin sh
fi

echo "=== Configuring Munge ==="
if [ ! -f /etc/munge/munge.key ]; then
    dd if=/dev/urandom of=/etc/munge/munge.key bs=1024 count=1 2>/dev/null
    chown munge:munge /etc/munge/munge.key
    chmod 400 /etc/munge/munge.key
fi
systemctl enable munge
systemctl restart munge

# Verify munge before anything else that depends on it
echo "=== Verifying Munge ==="
for i in $(seq 1 10); do
    if munge -n | unmunge &>/dev/null; then
        echo "Munge authentication working."
        break
    fi
    sleep 1
done

# ---- Write ALL Slurm config files FIRST (sacctmgr/slurmctld need them) ----

echo "=== Writing Slurm configuration ==="
cat > /etc/slurm/slurm.conf <<'SLURM_CONF'
ClusterName=e2e
SlurmctldHost=localhost
SlurmUser=slurm

AuthType=auth/munge
ProctrackType=proctrack/linuxproc
TaskPlugin=task/none

SlurmctldPort=6817
SlurmdPort=6818

SlurmdSpoolDir=/var/spool/slurmd
StateSaveLocation=/var/spool/slurmctld
SlurmdLogFile=/var/log/slurm/slurmd.log
SlurmctldLogFile=/var/log/slurm/slurmctld.log

ReturnToService=2
SlurmctldTimeout=120
SlurmdTimeout=120

AccountingStorageType=accounting_storage/slurmdbd
AccountingStorageHost=localhost
AccountingStoragePort=6819
JobCompType=jobcomp/none

SchedulerType=sched/builtin
SelectType=select/cons_tres
SelectTypeParameters=CR_CPU_Memory

PartitionName=normal Nodes=localhost Default=YES MaxTime=00:10:00 State=UP
NodeName=localhost CPUs=2 RealMemory=1800 State=UNKNOWN
SLURM_CONF

cat > /etc/slurm/cgroup.conf <<'CGROUP_CONF'
ConstrainCores=no
ConstrainRAMSpace=no
ConstrainSwapSpace=no
CGROUP_CONF

mkdir -p /var/spool/slurmctld /var/spool/slurmd /var/log/slurm
chown slurm:slurm /var/spool/slurmctld /var/spool/slurmd /var/log/slurm

# ---- MariaDB ----

echo "=== Configuring MariaDB for Slurm accounting ==="
systemctl enable mariadb
systemctl start mariadb

mysql -e "CREATE DATABASE IF NOT EXISTS slurm_acct_db;"
mysql -e "CREATE USER IF NOT EXISTS 'slurm'@'localhost' IDENTIFIED BY 'slurm';"
mysql -e "GRANT ALL ON slurm_acct_db.* TO 'slurm'@'localhost';"
mysql -e "FLUSH PRIVILEGES;"

# ---- slurmdbd (needs slurm.conf + MariaDB) ----

echo "=== Configuring slurmdbd ==="
cat > /etc/slurm/slurmdbd.conf <<'SLURMDBD_CONF'
AuthType=auth/munge
DbdHost=localhost
DbdPort=6819
SlurmUser=slurm
StorageType=accounting_storage/mysql
StorageHost=localhost
StoragePort=3306
StorageUser=slurm
StoragePass=slurm
StorageLoc=slurm_acct_db
LogFile=/var/log/slurm/slurmdbd.log
PidFile=/run/slurmdbd.pid
SLURMDBD_CONF
chown slurm:slurm /etc/slurm/slurmdbd.conf
chmod 600 /etc/slurm/slurmdbd.conf

systemctl enable slurmdbd
systemctl restart slurmdbd

# Wait for slurmdbd to accept connections
echo "=== Waiting for slurmdbd ==="
for i in $(seq 1 15); do
    if sacctmgr -i list cluster 2>/dev/null | grep -q "e2e"; then
        echo "slurmdbd ready (cluster already exists)."
        break
    fi
    if sacctmgr -i add cluster e2e 2>/dev/null; then
        echo "slurmdbd ready, cluster 'e2e' registered."
        break
    fi
    sleep 2
done

# Create default account and vagrant user association
sleep 2
echo "=== Setting up Slurm accounting ==="
sacctmgr -i add account default Description="Default account" Organization="e2e" || true
sacctmgr -i add user vagrant DefaultAccount=default || true
sacctmgr list assoc format=Cluster,Account,User,Partition
echo "Accounting setup complete."

# ---- slurmctld + slurmd (need slurm.conf + slurmdbd running) ----

echo "=== Starting Slurm controller and daemon ==="
systemctl enable slurmctld slurmd
systemctl restart slurmctld
sleep 2
systemctl restart slurmd

# Wait for the node to register and become idle
echo "=== Waiting for Slurm node ==="
for i in $(seq 1 30); do
    state=$(sinfo -N --noheader -o "%T" 2>/dev/null | head -1)
    if [ "$state" = "idle" ]; then
        echo "Slurm cluster ready."
        break
    fi
    if [ "$state" = "idle*" ] && [ "$i" -gt 5 ]; then
        echo "Node non-responsive, restarting slurmd..."
        systemctl restart slurmd
    fi
    sleep 2
done
sinfo

echo "=== Installing e2e job scripts ==="
mkdir -p /opt/e2e/scripts /data/jobs/tmp
chown -R vagrant:vagrant /data/jobs
cp /opt/hpc-daemon/e2e/scripts/e2e_job*.sh /opt/e2e/scripts/
chmod +x /opt/e2e/scripts/*.sh

# ---- Smoke tests: verify Slurm actually works before installing the daemon ----

echo "=== Smoke test 1: munge authentication ==="
if ! munge -n | unmunge >/dev/null 2>&1; then
    echo "FATAL: munge authentication broken"
    exit 1
fi
echo "PASS: munge auth works"

echo "=== Smoke test 2: node is idle ==="
node_state=$(sinfo -N --noheader -o "%T" 2>/dev/null | head -1)
if [ "$node_state" != "idle" ]; then
    echo "FATAL: node state is '$node_state', expected 'idle'"
    sinfo
    exit 1
fi
echo "PASS: node is idle"

echo "=== Smoke test 3: vagrant user has accounting association ==="
assoc_count=$(sacctmgr -n list assoc user=vagrant format=User 2>/dev/null | grep -c vagrant || true)
if [ "$assoc_count" -eq 0 ]; then
    echo "FATAL: vagrant user has no accounting association"
    sacctmgr list assoc format=Cluster,Account,User
    exit 1
fi
echo "PASS: vagrant has accounting association"

echo "=== Smoke test 4: sbatch submit + run + complete ==="
SMOKE_DIR=$(mktemp -d /tmp/slurm-smoke.XXXXXX)
chown vagrant:vagrant "$SMOKE_DIR"

su - vagrant -c "sbatch --wait --output=${SMOKE_DIR}/stdout.log --error=${SMOKE_DIR}/stderr.log --wrap 'echo SMOKE_OK > ${SMOKE_DIR}/result.txt'"
smoke_exit=$?

if [ "$smoke_exit" -ne 0 ]; then
    echo "FATAL: sbatch --wait exited with $smoke_exit"
    cat "${SMOKE_DIR}/stdout.log" 2>/dev/null
    cat "${SMOKE_DIR}/stderr.log" 2>/dev/null
    sudo cat /var/log/slurm/slurmctld.log | tail -20
    exit 1
fi

if [ ! -f "${SMOKE_DIR}/result.txt" ] || ! grep -q "SMOKE_OK" "${SMOKE_DIR}/result.txt"; then
    echo "FATAL: smoke test job did not produce expected output"
    ls -la "$SMOKE_DIR"
    cat "${SMOKE_DIR}/stdout.log" 2>/dev/null
    cat "${SMOKE_DIR}/stderr.log" 2>/dev/null
    exit 1
fi

# Verify sacct can see the completed job
sacct_state=$(sacct --noheader --format=State -j 1 2>/dev/null | head -1 | tr -d ' ')
if [ "$sacct_state" != "COMPLETED" ]; then
    echo "FATAL: sacct shows state '$sacct_state', expected 'COMPLETED'"
    sacct --format=JobID,State,ExitCode,Reason -j 1
    exit 1
fi

rm -rf "$SMOKE_DIR"
echo "PASS: job submitted, ran, completed, output verified"

echo "=== Smoke test 5: entrypoint script runs correctly ==="
SMOKE2_DIR=$(mktemp -d /tmp/slurm-smoke2.XXXXXX)
chown vagrant:vagrant "$SMOKE2_DIR"
mkdir -p "${SMOKE2_DIR}/output" "${SMOKE2_DIR}/input" "${SMOKE2_DIR}/work"
chown -R vagrant:vagrant "$SMOKE2_DIR"

cat > "${SMOKE2_DIR}/run.sh" <<RUNEOF
#!/bin/bash
export HPC_JOB_ID=smoke-test
export HPC_INPUT_DIR=${SMOKE2_DIR}/input
export HPC_OUTPUT_DIR=${SMOKE2_DIR}/output
export HPC_WORK_DIR=${SMOKE2_DIR}/work
export HPC_PARAMETERS='{}'
exec /opt/e2e/scripts/e2e_job.sh
RUNEOF
chmod +x "${SMOKE2_DIR}/run.sh"
chown vagrant:vagrant "${SMOKE2_DIR}/run.sh"

su - vagrant -c "sbatch --wait --output=${SMOKE2_DIR}/stdout.log --error=${SMOKE2_DIR}/stderr.log ${SMOKE2_DIR}/run.sh"
smoke2_exit=$?

if [ "$smoke2_exit" -ne 0 ]; then
    echo "FATAL: entrypoint smoke test exited with $smoke2_exit"
    cat "${SMOKE2_DIR}/stdout.log" 2>/dev/null
    cat "${SMOKE2_DIR}/stderr.log" 2>/dev/null
    exit 1
fi

if [ ! -f "${SMOKE2_DIR}/output/result.txt" ] || ! grep -q "Hello from e2e job" "${SMOKE2_DIR}/output/result.txt"; then
    echo "FATAL: entrypoint did not produce expected output"
    ls -la "${SMOKE2_DIR}/output/"
    cat "${SMOKE2_DIR}/output/result.txt" 2>/dev/null
    exit 1
fi

rm -rf "$SMOKE2_DIR"
echo "PASS: entrypoint script produces correct output"

echo "=== All smoke tests passed ==="

echo "=== Installing HPC daemon ==="
su - vagrant -c "cd /opt/hpc-daemon && uv tool install --force --reinstall --from . emx2-hpc-daemon"

echo "=== Writing daemon config ==="
mkdir -p /etc/hpc-daemon
SECRET_FILE="/opt/hpc-daemon/.secret"
if [ ! -f "$SECRET_FILE" ]; then
    echo "FATAL: .secret file not found at $SECRET_FILE"
    echo "Create tools/hpc-daemon/.secret with the shared secret before provisioning."
    exit 1
fi
cp "$SECRET_FILE" /etc/hpc-daemon/secret
chmod 600 /etc/hpc-daemon/secret
chown vagrant:vagrant /etc/hpc-daemon/secret

cat > /etc/hpc-daemon/daemon-config.yaml <<DAEMON_CONF
emx2:
  base_url: "${EMX2_BASE_URL}"
  worker_id: "e2e-test-worker"
  shared_secret_file: /etc/hpc-daemon/secret

worker:
  poll_interval_seconds: 20
  max_concurrent_jobs: 4

slurm:
  default_partition: "normal"

profiles:
  "e2e-test:bash":
    entrypoint: "/opt/e2e/scripts/e2e_job.sh"
    partition: "normal"
    cpus: 1
    memory: "256M"
    time: "00:05:00"
    output_residence: "managed"
    log_residence: "managed"
  "e2e-test:fail":
    entrypoint: "/opt/e2e/scripts/e2e_job_fail.sh"
    partition: "normal"
    cpus: 1
    memory: "256M"
    time: "00:05:00"
    output_residence: "managed"
    log_residence: "managed"
  "e2e-test:slow":
    entrypoint: "/opt/e2e/scripts/e2e_job_slow.sh"
    partition: "normal"
    cpus: 1
    memory: "256M"
    time: "00:05:00"
    output_residence: "managed"
    log_residence: "managed"
  "e2e-test:posix":
    entrypoint: "/opt/e2e/scripts/e2e_job_posix.sh"
    partition: "normal"
    cpus: 1
    memory: "256M"
    time: "00:05:00"
    output_residence: "posix"
    log_residence: "posix"

apptainer:
  tmp_dir: "/data/jobs/tmp"
DAEMON_CONF

echo "=== Setting up daemon cron job ==="
touch /var/log/hpc-daemon.log
chown vagrant:vagrant /var/log/hpc-daemon.log

cat > /usr/local/bin/hpc-daemon-once <<'WRAPPER'
#!/bin/bash
exec /home/vagrant/.local/bin/emx2-hpc-daemon -c /etc/hpc-daemon/daemon-config.yaml once -v >> /var/log/hpc-daemon.log 2>&1
WRAPPER
chmod +x /usr/local/bin/hpc-daemon-once

# Run every ~20 seconds via cron
cat > /etc/cron.d/hpc-daemon <<'CRON'
* * * * * vagrant /usr/local/bin/hpc-daemon-once
* * * * * vagrant sleep 20 && /usr/local/bin/hpc-daemon-once
* * * * * vagrant sleep 40 && /usr/local/bin/hpc-daemon-once
CRON
chmod 644 /etc/cron.d/hpc-daemon

echo "=== Provision complete ==="
sinfo
