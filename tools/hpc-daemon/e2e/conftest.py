"""Session-scoped fixtures for HPC bridge e2e tests.

Assumes:
- EMX2 is running externally (default: http://localhost:8080)
- Slurm VM is running via Vagrant (make up) with daemon cron job installed
"""

from __future__ import annotations

import logging
import os
import subprocess
import time

import httpx
import pytest

from emx2_hpc_daemon.client import HpcClient

logger = logging.getLogger(__name__)

EMX2_BASE_URL = os.environ.get("EMX2_BASE_URL", "http://localhost:8080")
SHARED_SECRET = os.environ.get(
    "HPC_SHARED_SECRET", "e2e-test-secret-do-not-use-in-production"
)
WORKER_ID = "e2e-test-worker"
STARTUP_TIMEOUT = 180  # seconds
VM_DIR = os.path.join(os.path.dirname(__file__), "vm")


def _wait_for_emx2(base_url: str, timeout: int) -> None:
    """Poll EMX2 health endpoint until it responds."""
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        try:
            r = httpx.get(f"{base_url}/api/hpc/health", timeout=5)
            if r.status_code == 200:
                logger.info("EMX2 is healthy at %s", base_url)
                return
        except httpx.ConnectError:
            pass
        time.sleep(3)
    raise TimeoutError(f"EMX2 not healthy at {base_url} after {timeout}s")


def _vagrant_ssh(cmd: str) -> subprocess.CompletedProcess:
    """Run a command inside the Vagrant VM, return the CompletedProcess."""
    result = subprocess.run(
        ["vagrant", "ssh", "-c", cmd],
        capture_output=True,
        text=True,
        timeout=60,
        cwd=VM_DIR,
    )
    if result.returncode != 0:
        logger.warning(
            "vagrant ssh (rc=%d): %s",
            result.returncode,
            result.stderr.strip() or result.stdout.strip(),
        )
    return result


_DAEMON_CMD = (
    "/home/vagrant/.local/bin/emx2-hpc-daemon"
    " -c /etc/hpc-daemon/daemon-config.yaml once -v"
)


def _trigger_daemon_once() -> subprocess.CompletedProcess:
    """Trigger a single daemon cycle inside the VM (don't wait for cron)."""
    return _vagrant_ssh(_DAEMON_CMD)


@pytest.fixture(scope="session")
def emx2_base_url():
    """The base URL of the external EMX2 instance."""
    return EMX2_BASE_URL


@pytest.fixture(scope="session")
def shared_secret():
    """The shared HMAC secret."""
    return SHARED_SECRET


@pytest.fixture(scope="session")
def wait_for_services(emx2_base_url):
    """Wait for EMX2 to be ready (Slurm VM is managed by Vagrant)."""
    _wait_for_emx2(emx2_base_url, STARTUP_TIMEOUT)


@pytest.fixture(scope="session")
def bootstrap_hpc(wait_for_services, emx2_base_url, shared_secret):
    """Bootstrap HPC: set the shared secret in EMX2 and trigger lazy init.

    Uses the EMX2 GraphQL API to set MOLGENIS_HPC_SHARED_SECRET, then makes
    an HMAC-authenticated request to trigger table creation.
    """
    # Check if HPC is already enabled (e.g. secret set via UI before test run)
    health_r = httpx.get(f"{emx2_base_url}/api/hpc/health", timeout=5)
    health = health_r.json()
    logger.info("HPC health before bootstrap: %s", health)

    if not health.get("hpc_enabled"):
        # Sign in as admin
        signin_query = """
        mutation {
          signin(email: "admin", password: "admin") {
                token
          }
        }
        """
        r = httpx.post(
            f"{emx2_base_url}/api/graphql",
            json={"query": signin_query},
            timeout=10,
        )
        r.raise_for_status()
        data = r.json()
        token = data["data"]["signin"]["token"]
        logger.info("Signed in as admin, got token")

        # Set the HPC shared secret via GraphQL settings mutation
        settings_query = (
            """
        mutation {
          change(settings: [{key: "MOLGENIS_HPC_SHARED_SECRET", value: "%s"}]) {
            message
          }
        }
        """
            % shared_secret
        )
        r = httpx.post(
            f"{emx2_base_url}/api/graphql",
            json={"query": settings_query},
            headers={"x-molgenis-token": token},
            timeout=10,
        )
        r.raise_for_status()
        settings_data = r.json()
        if "errors" in settings_data:
            raise RuntimeError(f"Failed to set HPC secret: {settings_data['errors']}")
        logger.info("Set HPC shared secret in EMX2: %s", settings_data)

    # Make an authenticated request to trigger lazy init of HPC tables
    client = HpcClient(
        base_url=emx2_base_url,
        worker_id=WORKER_ID,
        shared_secret=shared_secret,
    )
    try:
        client.poll_pending_jobs()
        logger.info("HPC tables initialized")
    finally:
        client.close()


@pytest.fixture(scope="session")
def wait_for_daemon(bootstrap_hpc):
    """Wait for the cron-driven daemon to complete at least one successful cycle.

    Checks the daemon log inside the VM for a successful cycle marker.
    Falls back to triggering a manual cycle if cron hasn't run yet.
    """
    deadline = time.monotonic() + 120
    while time.monotonic() < deadline:
        # Check if cron-driven daemon has run at least once
        result = _vagrant_ssh(
            "grep -c 'Single cycle complete' /var/log/hpc-daemon.log 2>/dev/null || echo 0"
        )
        count = int(result.stdout.strip().split()[-1]) if result.returncode == 0 else 0
        if count > 0:
            logger.info("Daemon has completed %d cycle(s) via cron", count)
            return
        # If cron hasn't fired yet, trigger once manually to bootstrap
        _trigger_daemon_once()
        time.sleep(10)
    raise TimeoutError("Daemon did not complete a successful cycle within 120s")


@pytest.fixture(scope="session")
def hpc_client(wait_for_daemon, emx2_base_url, shared_secret):
    """HpcClient connected to the external EMX2 instance."""
    client = HpcClient(
        base_url=emx2_base_url,
        worker_id="e2e-test-submitter",
        shared_secret=shared_secret,
    )
    yield client
    client.close()


def create_job(client: HpcClient, processor: str, profile: str, **kwargs) -> dict:
    """Submit a job via the HPC API and return the response."""
    body = {"processor": processor, "profile": profile, **kwargs}
    return client._request("POST", "/api/hpc/jobs", json=body)


def wait_for_job_status(
    client: HpcClient,
    job_id: str,
    target: str | set[str],
    timeout: int = 180,
) -> dict:
    """Poll a job until it reaches the target status (or one of a set).

    The daemon is driven by cron (every minute) inside the VM; this function
    just polls EMX2 and waits for the status to converge.
    """
    if isinstance(target, str):
        target = {target}
    deadline = time.monotonic() + timeout
    last_status = None
    while time.monotonic() < deadline:
        job = client.get_job(job_id)
        status = job.get("status")
        if status != last_status:
            logger.info("Job %s status: %s", job_id, status)
            last_status = status
        if status in target:
            return job
        time.sleep(5)
    raise TimeoutError(
        f"Job {job_id} did not reach {target} within {timeout}s (last: {last_status})"
    )
