"""Rotate e2e worker credential and sync secret to host + VM.

Used by `make test` so e2e does not depend on stale manual secrets.
"""

from __future__ import annotations

import os
import shlex
import subprocess
import sys
import time
import uuid
from pathlib import Path

import httpx

EMX2_BASE_URL = os.environ.get("EMX2_BASE_URL", "http://localhost:8080")
EMX2_ADMIN_EMAIL = os.environ.get("EMX2_ADMIN_EMAIL", "admin")
EMX2_ADMIN_PASSWORD = os.environ.get("EMX2_ADMIN_PASSWORD", "admin")
WORKER_ID = os.environ.get("E2E_WORKER_ID", "e2e-test-worker")

THIS_DIR = Path(__file__).resolve().parent
VM_DIR = THIS_DIR / "vm"
SECRET_FILE = THIS_DIR.parent / ".secret"


def _protocol_headers() -> dict[str, str]:
    return {
        "X-EMX2-API-Version": "2025-01",
        "X-Request-Id": str(uuid.uuid4()),
        "X-Timestamp": str(int(time.time())),
        "Content-Type": "application/json",
    }


def _vagrant_ssh_checked(cmd: str, *, timeout: int = 120) -> str:
    result = subprocess.run(
        ["vagrant", "ssh", "-c", cmd],
        cwd=VM_DIR,
        capture_output=True,
        text=True,
        timeout=timeout,
    )
    if result.returncode != 0:
        raise RuntimeError(
            "vagrant ssh failed\n"
            f"command: {cmd}\n"
            f"stdout:\n{(result.stdout or '').strip()}\n"
            f"stderr:\n{(result.stderr or '').strip()}"
        )
    return (result.stdout or "").strip()


def _rotate_worker_secret() -> str:
    with httpx.Client(base_url=EMX2_BASE_URL, timeout=15) as client:
        health = client.get("/api/hpc/health")
        health.raise_for_status()
        health_body = health.json()
        if not health_body.get("hpc_enabled"):
            raise RuntimeError(
                "HPC is disabled. Set _SYSTEM_.MOLGENIS_HPC_ENABLED=true."
            )
        if not health_body.get("credentials_key_configured", False):
            raise RuntimeError(
                "Worker credentials key is missing. "
                "Set _SYSTEM_.MOLGENIS_HPC_CREDENTIALS_KEY first."
            )

        signin_query = 'mutation{signin(email:"%s",password:"%s"){status,message}}' % (
            EMX2_ADMIN_EMAIL,
            EMX2_ADMIN_PASSWORD,
        )
        signin = client.post("/api/graphql", json={"query": signin_query})
        signin.raise_for_status()
        payload = signin.json()
        status = payload.get("data", {}).get("signin", {}).get("status")
        if status != "SUCCESS":
            message = (
                payload.get("data", {})
                .get("signin", {})
                .get("message", "unknown error")
            )
            raise RuntimeError(f"Admin signin failed: {message}")

        rotate = client.post(
            f"/api/hpc/workers/{WORKER_ID}/credentials/rotate",
            headers=_protocol_headers(),
            json={},
        )
        rotate.raise_for_status()
        secret = rotate.json().get("secret")
        if not secret:
            raise RuntimeError("Rotate endpoint did not return secret.")
        return secret


def _write_secret(secret: str) -> None:
    SECRET_FILE.write_text(secret)
    os.chmod(SECRET_FILE, 0o600)


def _sync_secret_to_vm(secret: str) -> None:
    escaped = shlex.quote(secret)
    cmd = "sudo sh -lc " + shlex.quote(
        f"printf '%s' {escaped} > /etc/hpc-daemon/secret && "
        "chown vagrant:vagrant /etc/hpc-daemon/secret && "
        "chmod 600 /etc/hpc-daemon/secret"
    )
    _vagrant_ssh_checked(cmd)


def main() -> int:
    try:
        secret = _rotate_worker_secret()
        _write_secret(secret)
        _sync_secret_to_vm(secret)
        print(
            "Refreshed e2e worker secret and synced to VM "
            f"(worker_id={WORKER_ID}, emx2={EMX2_BASE_URL})."
        )
        return 0
    except Exception as exc:  # noqa: BLE001
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
