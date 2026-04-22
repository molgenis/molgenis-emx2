"""Nested output paths round-trip for managed artifacts."""

from pathlib import Path

from conftest import create_job, wait_for_job_status
from emx2_hpc_daemon.testkit import deterministic_temp_dir


def test_nested_output_paths_roundtrip(hpc_client):
    resp = create_job(hpc_client, processor="e2e-test", profile="bash")
    job_id = resp["id"]
    assert resp["status"] == "PENDING"

    job = wait_for_job_status(hpc_client, job_id, "COMPLETED", timeout=150)
    assert job["status"] == "COMPLETED"

    output_id = job.get("output_artifact_id")
    assert output_id, "output_artifact_id must be set"

    files = hpc_client.list_artifact_files(output_id)
    paths = sorted(f["path"] for f in files)
    assert "results/nested/output.txt" in paths
    assert "results/nested/deeper/meta.json" in paths
    assert "reports/2026/03/report.txt" in paths

    with deterministic_temp_dir(f"e2e-nested-{job_id}") as tmpdir:
        root = Path(tmpdir)
        hpc_client.download_artifact_file(
            output_id,
            "results/nested/output.txt",
            str(root / "results/nested/output.txt"),
        )
        hpc_client.download_artifact_file(
            output_id,
            "results/nested/deeper/meta.json",
            str(root / "results/nested/deeper/meta.json"),
        )

        text = (root / "results/nested/output.txt").read_text()
        assert job_id in text
        meta = (root / "results/nested/deeper/meta.json").read_text()
        assert '"kind":"nested"' in meta
