.PHONY: ensure-vm reset-vm up preflight refresh-secret test test-clean sync down logs diagnostics clean status

# Ensure VM is up and re-provisioned to current config/schema every run.
ensure-vm:
	cd vm && vagrant up
	cd vm && vagrant provision

# Hard reset when VM/runtime state is truly unrecoverable.
reset-vm:
	cd vm && vagrant destroy -f || true
	$(MAKE) ensure-vm

# Start the Slurm VM (EMX2 must be running externally)
up:
	cd vm && vagrant up

# Sync daemon source to VM and reinstall
sync:
	cd vm && vagrant rsync
	cd vm && vagrant ssh -c 'cd /opt/hpc-daemon && uv tool install --force --reinstall --from . emx2-hpc-daemon'
	cd vm && vagrant ssh -c 'sudo -n install -m 0755 /opt/hpc-daemon/e2e/vm/hpc-e2e-preflight.sh /usr/local/bin/hpc-e2e-preflight'
	cd vm && vagrant ssh -c 'sudo -n mkdir -p /opt/e2e/scripts && sudo -n cp /opt/hpc-daemon/e2e/scripts/e2e_job*.sh /opt/e2e/scripts/ && sudo -n chmod +x /opt/e2e/scripts/e2e_job*.sh'

# Run deterministic VM preflight checks (clock sync + service readiness)
preflight:
	cd vm && vagrant ssh -c 'sudo -n /usr/local/bin/hpc-e2e-preflight'

# Rotate/sync e2e worker credential before tests.
refresh-secret:
	cd .. && uv run --extra e2e python e2e/refresh_secret.py

# Run e2e tests (assumes EMX2 is running). Uses deterministic reprovision,
# not full destroy/recreate, for faster feedback.
test: ensure-vm sync preflight refresh-secret
	cd .. && uv run --extra e2e pytest e2e/ -v --tb=short -x

# Full hard-reset test run.
test-clean: reset-vm sync preflight refresh-secret
	cd .. && uv run --extra e2e pytest e2e/ -v --tb=short -x

# Show daemon + slurm status inside the VM
status:
	cd vm && vagrant ssh -c 'sinfo; echo "---"; tail -30 /var/log/hpc-daemon.log'

# Show daemon logs
logs:
	cd vm && vagrant ssh -c 'tail -100 /var/log/hpc-daemon.log'

# Capture a broader diagnostic snapshot from inside the VM
diagnostics:
	cd vm && vagrant ssh -c 'date -u; echo "--- timedatectl ---"; timedatectl --no-pager; echo "--- chrony ---"; chronyc tracking; chronyc sources -v; echo "--- slurm ---"; sinfo; squeue -o "%.18i %.9P %.30j %.8u %.2t %.10M %.6D %R"; sacct -S now-2hours --format=JobID,State,ExitCode,Elapsed,NodeList; echo "--- daemon log ---"; tail -200 /var/log/hpc-daemon.log; echo "--- slurmctld ---"; sudo tail -120 /var/log/slurm/slurmctld.log; echo "--- slurmd ---"; sudo tail -120 /var/log/slurm/slurmd.log; echo "--- slurmdbd ---"; sudo tail -120 /var/log/slurm/slurmdbd.log'

# Stop the VM (preserves state)
down:
	cd vm && vagrant halt

# Destroy the VM completely
clean:
	cd vm && vagrant destroy -f

# Full cycle: deterministic test run with reprovision
e2e:
	$(MAKE) test || ($(MAKE) diagnostics; exit 1)
