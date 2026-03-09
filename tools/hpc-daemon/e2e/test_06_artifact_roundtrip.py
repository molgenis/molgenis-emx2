"""Full artifact round-trip: upload input → transform → download & verify output.

Exercises every leg of the artifact lifecycle:
  1. Create a managed input artifact
  2. Upload a CSV file (with Content-SHA256 integrity)
  3. Commit the input artifact
  4. Submit a job that references the input artifact
  5. Daemon claims, downloads inputs, runs transformation, uploads outputs
  6. Download output files and verify the transformation is correct

The entrypoint (e2e_job_transform.sh) sorts rows by temperature descending
and computes per-label statistics — a non-trivial transformation that proves
data actually flows through the system intact.
"""

import csv
import hashlib
import io
import json
import tempfile
from pathlib import Path

from conftest import create_job, wait_for_job_status


# --- Test data: a small but non-trivial CSV ---

INPUT_CSV = """\
sample_id,temperature,pressure,label
S001,23.5,1013.2,control
S002,37.8,1015.1,treatment
S003,19.2,1010.0,control
S004,42.1,1020.3,treatment
S005,28.9,1012.5,control
S006,35.6,1018.7,treatment
S007,15.0,1008.9,baseline
S008,22.3,1011.4,baseline
S009,41.0,1019.8,treatment
S010,26.7,1014.0,control
"""

# Pre-compute expected values so the test is self-contained.

def _expected_sorted_rows():
    """Return data rows sorted by temperature descending."""
    reader = csv.DictReader(io.StringIO(INPUT_CSV))
    rows = list(reader)
    rows.sort(key=lambda r: float(r["temperature"]), reverse=True)
    return rows


def _expected_stats():
    """Compute expected per-label statistics matching the awk script."""
    reader = csv.DictReader(io.StringIO(INPUT_CSV))
    labels: dict[str, dict] = {}
    total = 0
    for row in reader:
        total += 1
        label = row["label"]
        temp = float(row["temperature"])
        pres = float(row["pressure"])
        if label not in labels:
            labels[label] = {
                "count": 0,
                "sum_temp": 0.0,
                "sum_pres": 0.0,
                "min_temp": temp,
                "max_temp": temp,
                "min_pres": pres,
                "max_pres": pres,
            }
        s = labels[label]
        s["count"] += 1
        s["sum_temp"] += temp
        s["sum_pres"] += pres
        s["min_temp"] = min(s["min_temp"], temp)
        s["max_temp"] = max(s["max_temp"], temp)
        s["min_pres"] = min(s["min_pres"], pres)
        s["max_pres"] = max(s["max_pres"], pres)
    return total, labels


def test_artifact_roundtrip_transform(hpc_client):
    """Full round-trip: upload CSV → transform on Slurm → verify output."""

    # ---- Step 1: Create input artifact ----
    input_artifact = hpc_client.create_artifact(
        artifact_type="blob",
        residence="managed",
        name="e2e-transform-input",
    )
    input_id = input_artifact["id"]
    assert input_artifact["status"] == "CREATED"

    # ---- Step 2: Upload CSV file with Content-SHA256 ----
    csv_bytes = INPUT_CSV.encode("utf-8")
    csv_sha256 = hashlib.sha256(csv_bytes).hexdigest()

    hpc_client.upload_artifact_file(
        artifact_id=input_id,
        path="measurements.csv",
        file_content=csv_bytes,
        content_type="text/csv",
    )

    # Verify file was registered
    files = hpc_client.list_artifact_files(input_id)
    assert len(files) == 1
    f = files[0]
    assert f["path"] == "measurements.csv"
    assert f["sha256"] == csv_sha256
    assert int(f["size_bytes"]) == len(csv_bytes)

    # ---- Step 3: Commit the input artifact ----
    hpc_client.commit_artifact(
        artifact_id=input_id,
        sha256=csv_sha256,  # single-file tree hash == file hash
        size_bytes=len(csv_bytes),
    )
    committed = hpc_client.get_artifact(input_id)
    assert committed["status"] == "COMMITTED"

    # ---- Step 4: Submit job with input reference ----
    resp = create_job(
        hpc_client,
        processor="e2e-test",
        profile="transform",
        inputs=[{"artifact_id": input_id}],
    )
    job_id = resp["id"]
    assert resp["status"] == "PENDING"

    # ---- Step 5: Wait for job completion ----
    job = wait_for_job_status(hpc_client, job_id, "COMPLETED", timeout=180)
    assert job.get("slurm_job_id"), "slurm_job_id should be set"

    # ---- Step 6: Verify output artifact ----
    output_id = job.get("output_artifact_id")
    assert output_id, "output_artifact_id should be set"
    output_artifact = hpc_client.get_artifact(output_id)
    assert output_artifact["status"] == "COMMITTED"

    output_files = hpc_client.list_artifact_files(output_id)
    output_paths = sorted(f["path"] for f in output_files)
    assert "manifest.txt" in output_paths
    assert "sorted.csv" in output_paths
    assert "summary.json" in output_paths

    # ---- Step 7: Download and verify transformed content ----
    with tempfile.TemporaryDirectory() as tmpdir:
        for fname in ("sorted.csv", "summary.json", "manifest.txt"):
            hpc_client.download_artifact_file(
                output_id, fname, f"{tmpdir}/{fname}"
            )

        # -- Verify sorted.csv --
        sorted_text = Path(f"{tmpdir}/sorted.csv").read_text()
        reader = csv.DictReader(io.StringIO(sorted_text))
        sorted_rows = list(reader)
        expected_rows = _expected_sorted_rows()

        assert len(sorted_rows) == len(expected_rows), (
            f"Row count mismatch: got {len(sorted_rows)}, expected {len(expected_rows)}"
        )

        # Verify sort order: temperatures must be descending
        temps = [float(r["temperature"]) for r in sorted_rows]
        assert temps == sorted(temps, reverse=True), (
            f"Rows not sorted by temperature desc: {temps}"
        )

        # Verify first and last rows match expected
        assert sorted_rows[0]["sample_id"] == expected_rows[0]["sample_id"]
        assert sorted_rows[-1]["sample_id"] == expected_rows[-1]["sample_id"]

        # -- Verify summary.json --
        summary = json.loads(Path(f"{tmpdir}/summary.json").read_text())
        assert summary["job_id"] == job_id
        total, expected_labels = _expected_stats()
        assert summary["total_rows"] == total

        for label, expected in expected_labels.items():
            actual = summary["labels"][label]
            assert actual["count"] == expected["count"], (
                f"{label}: count {actual['count']} != {expected['count']}"
            )
            assert abs(actual["mean_temperature"] - expected["sum_temp"] / expected["count"]) < 0.1, (
                f"{label}: mean_temperature mismatch"
            )
            assert abs(actual["min_pressure"] - expected["min_pres"]) < 0.1
            assert abs(actual["max_pressure"] - expected["max_pres"]) < 0.1

        # -- Verify manifest.txt (sha256 checksums) --
        manifest_text = Path(f"{tmpdir}/manifest.txt").read_text()
        for line in manifest_text.strip().splitlines():
            expected_hash, fname = line.split()
            actual_hash = hashlib.sha256(
                Path(f"{tmpdir}/{fname}").read_bytes()
            ).hexdigest()
            assert actual_hash == expected_hash, (
                f"Manifest hash mismatch for {fname}: "
                f"manifest={expected_hash}, actual={actual_hash}"
            )

    # ---- Step 8: Verify log artifact ----
    log_id = job.get("log_artifact_id")
    assert log_id, "log_artifact_id should be set"
    log_artifact = hpc_client.get_artifact(log_id)
    assert log_artifact["status"] == "COMMITTED"
