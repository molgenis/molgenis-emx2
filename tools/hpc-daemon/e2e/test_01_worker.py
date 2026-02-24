"""Worker registration and daemon health checks."""


def test_daemon_cycle_succeeds(hpc_client):
    """The daemon should complete a full cycle (register + poll) without error."""
    # The hpc_client fixture depends on wait_for_daemon, which already verified
    # that a daemon cycle completed successfully.  This test ensures the fixture
    # chain ran and the client is usable.
    health = hpc_client._request("GET", "/api/hpc/health")
    assert health["status"] == "ok"
    assert health["hpc_enabled"] is True


def test_can_poll_pending_jobs(hpc_client):
    """The client should be able to poll for pending jobs (proves auth works)."""
    jobs = hpc_client.poll_pending_jobs()
    assert isinstance(jobs, list)


def test_heartbeat(hpc_client):
    """The client should be able to send a heartbeat."""
    result = hpc_client.heartbeat()
    assert result.get("status") == "ok"
