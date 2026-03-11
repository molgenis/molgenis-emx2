"""Session fixtures and e2e orchestration helpers for real Slurm tests."""

from __future__ import annotations

import contextvars
import itertools
import logging
import os
import shlex
import subprocess
import time
import uuid
from collections import defaultdict
from pathlib import Path

import httpx
import pytest

from emx2_hpc_daemon.client import HpcClient

logger = logging.getLogger(__name__)

EMX2_BASE_URL = os.environ.get("EMX2_BASE_URL", "http://localhost:8080")
EMX2_ADMIN_EMAIL = os.environ.get("EMX2_ADMIN_EMAIL", "admin")
EMX2_ADMIN_PASSWORD = os.environ.get("EMX2_ADMIN_PASSWORD", "admin")
WORKER_ID = "e2e-test-worker"
STARTUP_TIMEOUT = 180
DAEMON_READY_TIMEOUT = 180
VM_COMMAND_TIMEOUT = 120
JOB_POLL_INTERVAL_SECONDS = 3
DAEMON_KICK_INTERVAL_SECONDS = 12
VM_DIR = Path(__file__).resolve().parent / "vm"
SECRET_FILE = Path(__file__).resolve().parent.parent / ".secret"
DIAG_DIR = Path(__file__).resolve().parent / ".artifacts"
CLEANUP_POLICY = os.environ.get("E2E_CLEANUP_POLICY", "none").strip().lower()

_CURRENT_TEST_NODEID: contextvars.ContextVar[str] = contextvars.ContextVar(
    "hpc_e2e_current_test", default="<session>"
)
_TRACE_SEQ = itertools.count(1)
_CREATED_JOBS: dict[str, list[str]] = defaultdict(list)
_TERMINAL_STATUSES = {"COMPLETED", "FAILED", "CANCELLED"}


def _wait_for_emx2(base_url: str, timeout: int) -> None:
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        try:
            r = httpx.get(f"{base_url}/api/hpc/health", timeout=5)
            if r.status_code == 200:
                logger.info("EMX2 ready at %s", base_url)
                return
        except httpx.HTTPError:
            pass
        time.sleep(2)
    raise TimeoutError(f"EMX2 not healthy at {base_url} after {timeout}s")


def _vagrant_ssh(cmd: str, *, timeout: int = VM_COMMAND_TIMEOUT) -> subprocess.CompletedProcess:
    result = subprocess.run(
        ["vagrant", "ssh", "-c", cmd],
        capture_output=True,
        text=True,
        timeout=timeout,
        cwd=VM_DIR,
    )
    if result.returncode != 0:
        logger.warning(
            "vagrant ssh failed (rc=%d): %s",
            result.returncode,
            (result.stderr or result.stdout).strip(),
        )
    return result


def _vagrant_ssh_checked(cmd: str, *, timeout: int = VM_COMMAND_TIMEOUT) -> str:
    result = _vagrant_ssh(cmd, timeout=timeout)
    if result.returncode != 0:
        out = (result.stdout or "").strip()
        err = (result.stderr or "").strip()
        raise RuntimeError(f"VM command failed: {cmd}\nstdout:\n{out}\nstderr:\n{err}")
    return result.stdout.strip()


def _wait_for_vm_and_slurm(timeout: int = STARTUP_TIMEOUT) -> None:
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        result = _vagrant_ssh("true", timeout=20)
        if result.returncode == 0:
            break
        time.sleep(2)
    else:
        raise TimeoutError(f"Vagrant VM not reachable after {timeout}s")

    _vm_preflight()
    logger.info("VM and Slurm preflight checks passed")


def _vm_preflight() -> None:
    _vagrant_ssh_checked("sudo -n /usr/local/bin/hpc-e2e-preflight", timeout=90)


_DAEMON_CMD = "/usr/local/bin/hpc-daemon-once"


def _trigger_daemon_once(*, check: bool = True) -> subprocess.CompletedProcess:
    """Run a deterministic daemon cycle on-demand instead of waiting for cron cadence."""
    cmd = _DAEMON_CMD
    result = _vagrant_ssh(cmd, timeout=120)
    if check and result.returncode != 0:
        out = (result.stdout or "").strip()
        err = (result.stderr or "").strip()
        daemon_tail = _vagrant_ssh("tail -120 /var/log/hpc-daemon.log", timeout=30)
        daemon_log = (daemon_tail.stdout or "").strip()
        raise RuntimeError(
            "Manual daemon cycle failed\n"
            f"stdout:\n{out}\n"
            f"stderr:\n{err}\n"
            "daemon_log_tail:\n"
            f"{daemon_log}"
        )
    return result


def _next_trace_id() -> str:
    nodeid = _CURRENT_TEST_NODEID.get()
    seq = next(_TRACE_SEQ)
    return str(uuid.uuid5(uuid.NAMESPACE_URL, f"hpc-e2e:{nodeid}:{seq}"))


def _register_created_job(job_id: str) -> None:
    _CREATED_JOBS[_CURRENT_TEST_NODEID.get()].append(job_id)


def _protocol_headers() -> dict[str, str]:
    return {
        "X-EMX2-API-Version": "2025-01",
        "X-Request-Id": str(uuid.uuid4()),
        "X-Timestamp": str(int(time.time())),
        "Content-Type": "application/json",
    }


def _rotate_worker_credential(base_url: str, worker_id: str) -> str:
    with httpx.Client(base_url=base_url, timeout=10) as client:
        signin_query = (
            'mutation{signin(email:"%s",password:"%s"){status,message}}'
            % (EMX2_ADMIN_EMAIL, EMX2_ADMIN_PASSWORD)
        )
        signin = client.post("/api/graphql", json={"query": signin_query})
        signin.raise_for_status()
        payload = signin.json()
        status = (
            payload.get("data", {})
            .get("signin", {})
            .get("status")
        )
        if status != "SUCCESS":
            message = (
                payload.get("data", {})
                .get("signin", {})
                .get("message", "unknown error")
            )
            raise RuntimeError(f"Admin signin failed: {message}")

        rotate = client.post(
            f"/api/hpc/workers/{worker_id}/credentials/rotate",
            headers=_protocol_headers(),
            json={},
        )
        rotate.raise_for_status()
        secret = rotate.json().get("secret")
        if not secret:
            raise RuntimeError("Credential rotate did not return secret")
        return secret


def _sync_secret_to_vm(secret: str) -> None:
    escaped = shlex.quote(secret)
    _vagrant_ssh_checked(
        "sudo sh -lc "
        + shlex.quote(
            f"printf '%s' {escaped} > /etc/hpc-daemon/secret && "
            "chown vagrant:vagrant /etc/hpc-daemon/secret && "
            "chmod 600 /etc/hpc-daemon/secret"
        )
    )


def _make_support_client(base_url: str, secret: str) -> HpcClient:
    return HpcClient(
        base_url=base_url,
        worker_id=WORKER_ID,
        worker_secret=secret,
    )


def _verify_worker_secret(base_url: str, secret: str) -> None:
    client = _make_support_client(base_url, secret)
    try:
        client.poll_pending_jobs()
    except httpx.HTTPStatusError as exc:
        if exc.response.status_code == 401:
            raise RuntimeError(
                "HMAC authentication failed: worker secret is not active for "
                f"worker '{WORKER_ID}'."
            ) from exc
        raise
    finally:
        client.close()


def _cleanup_jobs_for_test(nodeid: str, base_url: str, secret: str, *, policy: str) -> None:
    job_ids = list(dict.fromkeys(_CREATED_JOBS.pop(nodeid, [])))
    if not job_ids or policy == "none":
        return
    if policy not in {"none", "non_terminal", "all"}:
        raise ValueError(f"Invalid E2E_CLEANUP_POLICY={policy!r}")
    try:
        _vm_preflight()
    except Exception:  # noqa: BLE001
        pass
    client = _make_support_client(base_url, secret)
    try:
        for job_id in reversed(job_ids):
            try:
                job = client.get_job(job_id)
            except Exception:
                continue

            status = job.get("status")
            if status not in _TERMINAL_STATUSES:
                try:
                    client.cancel_job(job_id)
                except Exception:
                    pass
                for _ in range(15):
                    try:
                        refreshed = client.get_job(job_id)
                    except Exception:
                        break
                    if refreshed.get("status") in _TERMINAL_STATUSES:
                        break
                    _trigger_daemon_once(check=False)
                    time.sleep(2)

            if policy == "all":
                try:
                    client._request("DELETE", f"/api/hpc/jobs/{job_id}")
                except Exception:
                    # Best-effort teardown. Diagnostics hook captures details on failures.
                    pass
    finally:
        client.close()


def _collect_diagnostics(nodeid: str, base_url: str, secret: str) -> str:
    lines: list[str] = []
    lines.append(f"test={nodeid}")
    lines.append(f"emx2_base_url={base_url}")
    lines.append("")

    vm_checks = [
        ("utc_time", "date -u"),
        ("timedatectl", "timedatectl --no-pager"),
        ("chrony_tracking", "chronyc tracking"),
        ("chrony_sources", "chronyc sources -v"),
        ("sinfo", "sinfo"),
        ("squeue", "squeue -o '%.18i %.9P %.30j %.8u %.2t %.10M %.6D %R'"),
        ("sacct_recent", "sacct -S now-2hours --format=JobID,State,ExitCode,Elapsed,NodeList"),
        ("daemon_log_tail", "tail -120 /var/log/hpc-daemon.log"),
        ("slurmctld_log_tail", "sudo tail -120 /var/log/slurm/slurmctld.log"),
        ("slurmd_log_tail", "sudo tail -120 /var/log/slurm/slurmd.log"),
        ("slurmdbd_log_tail", "sudo tail -120 /var/log/slurm/slurmdbd.log"),
    ]
    for label, cmd in vm_checks:
        result = _vagrant_ssh(cmd, timeout=90)
        lines.append(f"## {label}")
        lines.append(f"$ {cmd}")
        lines.append(f"rc={result.returncode}")
        if result.stdout:
            lines.append(result.stdout.rstrip())
        if result.stderr:
            lines.append("[stderr]")
            lines.append(result.stderr.rstrip())
        lines.append("")

    job_ids = list(dict.fromkeys(_CREATED_JOBS.get(nodeid, [])))
    if job_ids:
        support = _make_support_client(base_url, secret)
        try:
            lines.append("## jobs")
            for job_id in job_ids:
                lines.append(f"### job={job_id}")
                try:
                    job = support.get_job(job_id)
                    lines.append(f"status={job.get('status')}")
                    lines.append(f"worker_id={job.get('worker_id')}")
                    lines.append(f"slurm_job_id={job.get('slurm_job_id')}")
                except Exception as exc:  # noqa: BLE001
                    lines.append(f"job_fetch_error={exc}")
                    continue
                try:
                    transitions = support._request("GET", f"/api/hpc/jobs/{job_id}/transitions")
                    items = transitions.get("items", [])
                    for item in items:
                        lines.append(
                            f"{item.get('timestamp')} "
                            f"{item.get('from_status')}->{item.get('to_status')} "
                            f"worker={item.get('worker_id')} detail={item.get('detail')}"
                        )
                except Exception as exc:  # noqa: BLE001
                    lines.append(f"transition_fetch_error={exc}")
                lines.append("")
        finally:
            support.close()

    return "\n".join(lines)


@pytest.hookimpl(hookwrapper=True)
def pytest_runtest_makereport(item: pytest.Item, call: pytest.CallInfo):
    outcome = yield
    report = outcome.get_result()
    setattr(item, f"rep_{report.when}", report)

    if report.when != "call" or not report.failed:
        return

    base_url = item.funcargs.get("emx2_base_url", EMX2_BASE_URL)
    secret = item.funcargs.get("worker_secret")
    if not secret:
        return

    diag_text = _collect_diagnostics(item.nodeid, base_url, secret)
    DIAG_DIR.mkdir(parents=True, exist_ok=True)
    safe_name = (
        item.nodeid.replace("/", "__").replace("::", "__").replace("[", "_").replace("]", "_")
    )
    diag_path = DIAG_DIR / f"{safe_name}.log"
    diag_path.write_text(diag_text)
    report.sections.append(("hpc-diagnostics", f"saved={diag_path}\n{diag_text}"))


@pytest.fixture(autouse=True)
def _set_test_context(request: pytest.FixtureRequest):
    token = _CURRENT_TEST_NODEID.set(request.node.nodeid)
    try:
        yield
    finally:
        _CURRENT_TEST_NODEID.reset(token)


@pytest.fixture(autouse=True)
def _deterministic_teardown(
    request: pytest.FixtureRequest,
    emx2_base_url: str,
    worker_secret: str,
):
    yield
    _cleanup_jobs_for_test(
        request.node.nodeid,
        emx2_base_url,
        worker_secret,
        policy=CLEANUP_POLICY,
    )


@pytest.fixture(scope="session")
def emx2_base_url():
    return EMX2_BASE_URL


@pytest.fixture(scope="session")
def wait_for_services(emx2_base_url):
    _wait_for_emx2(emx2_base_url, STARTUP_TIMEOUT)
    _wait_for_vm_and_slurm(STARTUP_TIMEOUT)


@pytest.fixture(scope="session")
def bootstrap_hpc(wait_for_services, emx2_base_url):
    health = httpx.get(f"{emx2_base_url}/api/hpc/health", timeout=5).json()
    logger.info("HPC health: %s", health)
    if not health.get("hpc_enabled"):
        raise RuntimeError(
            "HPC is not enabled in EMX2. Set MOLGENIS_HPC_ENABLED=true."
        )

    if SECRET_FILE.exists():
        existing = SECRET_FILE.read_text().strip()
        if existing:
            try:
                _sync_secret_to_vm(existing)
                _verify_worker_secret(emx2_base_url, existing)
                logger.info("Using existing worker secret from %s", SECRET_FILE)
                logger.info("HPC auth verified and tables initialized")
                return existing
            except Exception as exc:  # noqa: BLE001
                logger.warning("Existing .secret failed verification, rotating: %s", exc)

    worker_secret = _rotate_worker_credential(emx2_base_url, WORKER_ID)
    SECRET_FILE.write_text(worker_secret)
    os.chmod(SECRET_FILE, 0o600)
    _sync_secret_to_vm(worker_secret)
    _verify_worker_secret(emx2_base_url, worker_secret)
    logger.info("Rotated worker secret and verified HMAC auth")
    logger.info("HPC auth verified and tables initialized")
    return worker_secret


@pytest.fixture(scope="session")
def worker_secret(bootstrap_hpc):
    return bootstrap_hpc


@pytest.fixture(scope="session")
def wait_for_daemon(bootstrap_hpc):
    _vm_preflight()
    _trigger_daemon_once(check=True)

    deadline = time.monotonic() + DAEMON_READY_TIMEOUT
    while time.monotonic() < deadline:
        result = _vagrant_ssh(
            "grep -c 'Single cycle complete' /var/log/hpc-daemon.log 2>/dev/null || echo 0",
            timeout=30,
        )
        count = int(result.stdout.strip().split()[-1]) if result.returncode == 0 else 0
        if count > 0:
            logger.info("Daemon cycle marker detected: %d successful cycle(s)", count)
            return
        _trigger_daemon_once(check=False)
        time.sleep(5)
    raise TimeoutError("Daemon did not complete a successful cycle in time")


@pytest.fixture(scope="session")
def hpc_client(wait_for_daemon, emx2_base_url, worker_secret):
    client = HpcClient(
        base_url=emx2_base_url,
        worker_id=WORKER_ID,
        worker_secret=worker_secret,
    )
    yield client
    client.close()


@pytest.fixture(scope="session")
def worker_client(wait_for_daemon, emx2_base_url, worker_secret):
    """Client that uses the daemon worker identity for worker lifecycle calls."""
    client = HpcClient(
        base_url=emx2_base_url,
        worker_id=WORKER_ID,
        worker_secret=worker_secret,
    )
    yield client
    client.close()


@pytest.fixture
def vm_run():
    """Run a command in the VM from tests."""

    def _run(cmd: str, *, check: bool = True, timeout: int = VM_COMMAND_TIMEOUT):
        result = _vagrant_ssh(cmd, timeout=timeout)
        if check and result.returncode != 0:
            out = (result.stdout or "").strip()
            err = (result.stderr or "").strip()
            raise RuntimeError(f"VM command failed: {cmd}\nstdout:\n{out}\nstderr:\n{err}")
        return result

    return _run


def create_job(client: HpcClient, processor: str, profile: str, **kwargs) -> dict:
    body = {"processor": processor, "profile": profile, **kwargs}
    trace_id = _next_trace_id()
    response = client._request(
        "POST",
        "/api/hpc/jobs",
        json=body,
        extra_headers={"X-Trace-Id": trace_id},
    )
    job_id = response.get("id")
    if job_id:
        _register_created_job(job_id)
    response["_trace_id"] = trace_id
    return response


def wait_for_job_status(
    client: HpcClient,
    job_id: str,
    target: str | set[str],
    timeout: int = 180,
    *,
    poll_interval_seconds: int = JOB_POLL_INTERVAL_SECONDS,
    daemon_kick_interval_seconds: int = DAEMON_KICK_INTERVAL_SECONDS,
    allow_unexpected_terminal: bool = False,
) -> dict:
    if isinstance(target, str):
        target = {target}
    deadline = time.monotonic() + timeout
    next_kick = 0.0
    last_status = None
    terminal = {"COMPLETED", "FAILED", "CANCELLED"}

    while time.monotonic() < deadline:
        now = time.monotonic()
        if now >= next_kick:
            _trigger_daemon_once(check=False)
            next_kick = now + daemon_kick_interval_seconds

        try:
            job = client.get_job(job_id)
        except httpx.HTTPStatusError as exc:
            if exc.response.status_code == 401:
                logger.warning("401 while polling job %s, running VM preflight", job_id)
                _vm_preflight()
                time.sleep(1)
                continue
            raise

        status = job.get("status")
        if status != last_status:
            logger.info("Job %s status=%s target=%s", job_id, status, sorted(target))
            last_status = status
        if status in target:
            return job
        if status in terminal and status not in target and not allow_unexpected_terminal:
            transitions = client._request("GET", f"/api/hpc/jobs/{job_id}/transitions")
            raise AssertionError(
                f"Job {job_id} reached unexpected terminal status {status}, expected {target}. "
                f"Transitions: {transitions.get('items', [])}"
            )
        time.sleep(poll_interval_seconds)

    raise TimeoutError(
        f"Job {job_id} did not reach {target} within {timeout}s (last={last_status})"
    )
