.PHONY: up test sync down logs clean status

# Start the Slurm VM (EMX2 must be running externally)
up:
	cd vm && vagrant up

# Sync daemon source to VM and reinstall
sync:
	cd vm && vagrant rsync
	cd vm && vagrant ssh -c 'cd /opt/hpc-daemon && uv tool install --force --reinstall --from . emx2-hpc-daemon'

# Run e2e tests (assumes EMX2 is running and VM is up)
# Syncs latest daemon code to the VM before running
test: sync
	cd .. && uv run --extra e2e pytest e2e/ -v --tb=short -x

# Show daemon + slurm status inside the VM
status:
	cd vm && vagrant ssh -c 'sinfo; echo "---"; tail -30 /var/log/hpc-daemon.log'

# Show daemon logs
logs:
	cd vm && vagrant ssh -c 'tail -100 /var/log/hpc-daemon.log'

# Stop the VM (preserves state)
down:
	cd vm && vagrant halt

# Destroy the VM completely
clean:
	cd vm && vagrant destroy -f

# Full cycle: start VM, run tests
e2e: up
	$(MAKE) test || ($(MAKE) logs; exit 1)
