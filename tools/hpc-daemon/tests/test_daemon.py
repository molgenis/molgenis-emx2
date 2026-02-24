"""Tests for daemon helper methods and end-to-end artifact upload flows."""

from __future__ import annotations

import hashlib
from pathlib import Path
from unittest.mock import MagicMock

from emx2_hpc_daemon.backend import StatusResult
from emx2_hpc_daemon.daemon import HpcDaemon, _build_slurm_detail
from emx2_hpc_daemon.slurm import SlurmJobInfo


def _get_transition_call(mock_client, index=0):
    """Extract (job_id, status, kwargs) from a transition_job mock call.

    transition_job is called as transition_job(job_id, status, detail=..., ...)
    so the first two are positional args, the rest are keyword args.
    """
    call_obj = mock_client.transition_job.call_args_list[index]
    args, kwargs = call_obj
    return args[0], args[1], kwargs


# ---------------------------------------------------------------------------
# Unit tests: _classify_output_files
# ---------------------------------------------------------------------------


class TestClassifyOutputFiles:
    """Test the _classify_output_files static method."""

    def test_separates_log_and_output_files(self, tmp_path: Path):
        # Output files
        (tmp_path / "result.csv").write_text("data")
        (tmp_path / "report.html").write_text("<html/>")

        # Log files
        (tmp_path / "slurm-12345.out").write_text("slurm stdout")
        (tmp_path / "slurm-12345.err").write_text("slurm stderr")
        (tmp_path / "container-stdout.log").write_text("stdout")
        (tmp_path / "container-stderr.log").write_text("stderr")
        (tmp_path / "debug.log").write_text("debug info")

        # Excluded
        (tmp_path / ".hpc_progress.json").write_text("{}")

        output_files, log_files = HpcDaemon._classify_output_files(str(tmp_path))

        output_names = sorted(f.name for f in output_files)
        log_names = sorted(f.name for f in log_files)

        assert output_names == ["report.html", "result.csv"]
        assert log_names == [
            "container-stderr.log",
            "container-stdout.log",
            "debug.log",
            "slurm-12345.err",
            "slurm-12345.out",
        ]

    def test_empty_directory(self, tmp_path: Path):
        output_files, log_files = HpcDaemon._classify_output_files(str(tmp_path))
        assert output_files == []
        assert log_files == []

    def test_only_progress_file(self, tmp_path: Path):
        (tmp_path / ".hpc_progress.json").write_text("{}")
        output_files, log_files = HpcDaemon._classify_output_files(str(tmp_path))
        assert output_files == []
        assert log_files == []

    def test_only_log_files(self, tmp_path: Path):
        (tmp_path / "slurm-999.out").write_text("log")
        (tmp_path / "app.log").write_text("log")

        output_files, log_files = HpcDaemon._classify_output_files(str(tmp_path))
        assert output_files == []
        assert sorted(f.name for f in log_files) == ["app.log", "slurm-999.out"]

    def test_only_output_files(self, tmp_path: Path):
        (tmp_path / "result.csv").write_text("data")
        (tmp_path / "image.png").write_bytes(b"\x89PNG")

        output_files, log_files = HpcDaemon._classify_output_files(str(tmp_path))
        assert sorted(f.name for f in output_files) == ["image.png", "result.csv"]
        assert log_files == []

    def test_ignores_directories(self, tmp_path: Path):
        (tmp_path / "subdir").mkdir()
        (tmp_path / "result.csv").write_text("data")

        output_files, log_files = HpcDaemon._classify_output_files(str(tmp_path))
        assert [f.name for f in output_files] == ["result.csv"]
        assert log_files == []


# ---------------------------------------------------------------------------
# Helpers: build a daemon with a mocked client and simulated backend
# ---------------------------------------------------------------------------


def _make_daemon(sample_config) -> tuple[HpcDaemon, MagicMock]:
    """Create an HpcDaemon with SimulatedBackend and a fully-mocked client."""
    daemon = HpcDaemon(sample_config, simulate=True)
    mock_client = MagicMock()

    # Stub register_worker so register() doesn't fail
    mock_client.register_worker.return_value = {}

    # Default: create_artifact returns an id, commit_artifact returns {}
    mock_client.create_artifact.side_effect = _fake_create_artifact
    mock_client.upload_artifact_file.return_value = {}
    mock_client.register_artifact_file.return_value = {}
    mock_client.commit_artifact.return_value = {}
    mock_client.transition_job.return_value = {}
    mock_client.get_job.return_value = {"status": "STARTED"}
    mock_client.heartbeat.return_value = {}

    daemon.client = mock_client
    return daemon, mock_client


_artifact_counter = 0


def _fake_create_artifact(**kwargs):
    """Return a fake artifact response with a unique id."""
    global _artifact_counter
    _artifact_counter += 1
    art_type = kwargs.get("artifact_type", "blob")
    return {"id": f"art-{art_type}-{_artifact_counter:04d}", "_links": {}}


# ---------------------------------------------------------------------------
# E2E tests: _monitor_running_jobs with artifact upload
# ---------------------------------------------------------------------------


class TestMonitorUploadsArtifacts:
    """End-to-end: verify that _monitor_running_jobs uploads both output and
    log artifacts and passes their IDs through to transition_job."""

    def test_completed_job_uploads_output_and_log_artifacts(
        self, sample_config, tmp_path: Path
    ):
        """On COMPLETED, both output and log artifacts should be created and
        their IDs passed to transition_job."""
        daemon, mock_client = _make_daemon(sample_config)

        # Populate output dir with output + log files
        output_dir = tmp_path / "output"
        output_dir.mkdir()
        (output_dir / "result.csv").write_text("col1,col2\n1,2\n")
        (output_dir / "slurm-12345.out").write_text("Job submitted\n")
        (output_dir / "container-stderr.log").write_text("warning: foo\n")

        # Track a STARTED job
        daemon.tracker.track(
            emx2_job_id="job-aaa-111",
            slurm_job_id="sim-job-aaa",
            status="STARTED",
            output_dir=str(output_dir),
            processor="text-embedding",
            profile="gpu-medium",
        )

        # SimulatedBackend: STARTED → COMPLETED
        daemon._monitor_running_jobs()

        # transition_job must have been called with both artifact IDs
        assert mock_client.transition_job.call_count == 1
        job_id, status, kwargs = _get_transition_call(mock_client)
        assert job_id == "job-aaa-111"
        assert status == "COMPLETED"
        assert kwargs["log_artifact_id"] is not None
        assert kwargs["output_artifact_id"] is not None
        assert kwargs["log_artifact_id"] != kwargs["output_artifact_id"]

        # create_artifact should have been called twice (log + output)
        create_calls = mock_client.create_artifact.call_args_list
        types_created = [c.kwargs.get("artifact_type") for c in create_calls]
        assert "log" in types_created
        assert "blob" in types_created

        # upload_artifact_file should have been called for the managed files
        uploaded_paths = [
            c.kwargs.get("path")
            for c in mock_client.upload_artifact_file.call_args_list
        ]
        assert "result.csv" in uploaded_paths
        assert "slurm-12345.out" in uploaded_paths
        assert "container-stderr.log" in uploaded_paths

        # Job should be removed from tracker (terminal)
        assert daemon.tracker.active_count() == 0

    def test_failed_job_uploads_log_artifact_only(self, sample_config, tmp_path: Path):
        """On FAILED, log artifacts should be uploaded but not output artifacts."""
        daemon, mock_client = _make_daemon(sample_config)

        output_dir = tmp_path / "output"
        output_dir.mkdir()
        # Only log files (job crashed before producing output)
        (output_dir / "slurm-99999.out").write_text("FAILED\n")
        (output_dir / "container-stderr.log").write_text("Segfault\n")

        daemon.tracker.track(
            emx2_job_id="job-bbb-222",
            slurm_job_id="sim-job-bbb",
            status="STARTED",
            output_dir=str(output_dir),
            processor="text-embedding",
            profile="gpu-medium",
        )

        # Make the simulated backend return FAILED instead of COMPLETED
        failed_result = StatusResult(
            hpc_status="FAILED",
            slurm_info=SlurmJobInfo(
                state="FAILED", exit_code="1:0", reason="NonZeroExitCode"
            ),
        )
        daemon._backend.query_status = MagicMock(return_value=failed_result)

        daemon._monitor_running_jobs()

        assert mock_client.transition_job.call_count == 1
        job_id, status, kwargs = _get_transition_call(mock_client)
        assert status == "FAILED"
        assert kwargs["log_artifact_id"] is not None
        # No output files → no output artifact
        assert kwargs["output_artifact_id"] is None

        # Only one artifact created (the log artifact)
        create_calls = mock_client.create_artifact.call_args_list
        assert len(create_calls) == 1
        assert create_calls[0].kwargs["artifact_type"] == "log"

    def test_completed_job_no_log_files(self, sample_config, tmp_path: Path):
        """If there are no log files, log_artifact_id should be None."""
        daemon, mock_client = _make_daemon(sample_config)

        output_dir = tmp_path / "output"
        output_dir.mkdir()
        (output_dir / "result.csv").write_text("data")

        daemon.tracker.track(
            emx2_job_id="job-ccc-333",
            slurm_job_id="sim-job-ccc",
            status="STARTED",
            output_dir=str(output_dir),
            processor="text-embedding",
            profile="gpu-medium",
        )

        daemon._monitor_running_jobs()

        job_id, status, kwargs = _get_transition_call(mock_client)
        assert status == "COMPLETED"
        assert kwargs["output_artifact_id"] is not None
        assert kwargs["log_artifact_id"] is None

    def test_completed_job_empty_output_dir(self, sample_config, tmp_path: Path):
        """Empty output dir → both artifact IDs should be None."""
        daemon, mock_client = _make_daemon(sample_config)

        output_dir = tmp_path / "output"
        output_dir.mkdir()

        daemon.tracker.track(
            emx2_job_id="job-ddd-444",
            slurm_job_id="sim-job-ddd",
            status="STARTED",
            output_dir=str(output_dir),
            processor="text-embedding",
            profile="gpu-medium",
        )

        daemon._monitor_running_jobs()

        _, _, kwargs = _get_transition_call(mock_client)
        assert kwargs["output_artifact_id"] is None
        assert kwargs["log_artifact_id"] is None

    def test_log_artifact_uploaded_with_correct_type_and_name(
        self, sample_config, tmp_path: Path
    ):
        """Log artifact should be created with type='log' and name prefix 'log-'."""
        daemon, mock_client = _make_daemon(sample_config)

        output_dir = tmp_path / "output"
        output_dir.mkdir()
        (output_dir / "result.csv").write_text("ok")
        (output_dir / "slurm-1.out").write_text("log")

        daemon.tracker.track(
            emx2_job_id="job-eee-555",
            slurm_job_id="sim-job-eee",
            status="STARTED",
            output_dir=str(output_dir),
            processor="text-embedding",
            profile="gpu-medium",
        )

        daemon._monitor_running_jobs()

        # Find the log artifact creation call
        log_calls = [
            c
            for c in mock_client.create_artifact.call_args_list
            if c.kwargs.get("artifact_type") == "log"
        ]
        assert len(log_calls) == 1
        assert log_calls[0].kwargs["name"].startswith("log-")

        # Find the output artifact creation call
        output_calls = [
            c
            for c in mock_client.create_artifact.call_args_list
            if c.kwargs.get("artifact_type") == "blob"
        ]
        assert len(output_calls) == 1
        assert output_calls[0].kwargs["name"].startswith("output-")

    def test_failed_job_with_output_files_uploads_log_only(
        self, sample_config, tmp_path: Path
    ):
        """On FAILED, even if output files exist, only log artifact is uploaded
        (output artifacts are only for COMPLETED)."""
        daemon, mock_client = _make_daemon(sample_config)

        output_dir = tmp_path / "output"
        output_dir.mkdir()
        (output_dir / "partial-result.csv").write_text("incomplete")
        (output_dir / "slurm-1.out").write_text("error log")
        (output_dir / "container-stderr.log").write_text("crash")

        daemon.tracker.track(
            emx2_job_id="job-fff-666",
            slurm_job_id="sim-job-fff",
            status="STARTED",
            output_dir=str(output_dir),
            processor="text-embedding",
            profile="gpu-medium",
        )

        failed_result = StatusResult(
            hpc_status="FAILED",
            slurm_info=SlurmJobInfo(
                state="FAILED", exit_code="1:0", reason="NonZeroExitCode"
            ),
        )
        daemon._backend.query_status = MagicMock(return_value=failed_result)

        daemon._monitor_running_jobs()

        _, status, kwargs = _get_transition_call(mock_client)
        assert status == "FAILED"
        assert kwargs["log_artifact_id"] is not None
        # Output artifact should NOT be uploaded on failure
        assert kwargs["output_artifact_id"] is None

    def test_transition_receives_log_artifact_id_kwarg(
        self, sample_config, tmp_path: Path
    ):
        """Verify the client's transition_job is called with log_artifact_id
        as a keyword argument (not just positional)."""
        daemon, mock_client = _make_daemon(sample_config)

        output_dir = tmp_path / "output"
        output_dir.mkdir()
        (output_dir / "debug.log").write_text("some log")
        (output_dir / "output.txt").write_text("result")

        daemon.tracker.track(
            emx2_job_id="job-ggg-777",
            slurm_job_id="sim-job-ggg",
            status="STARTED",
            output_dir=str(output_dir),
            processor="text-embedding",
            profile="gpu-medium",
        )

        daemon._monitor_running_jobs()

        # The call must pass log_artifact_id and output_artifact_id as kwargs
        _, _, kwargs = _get_transition_call(mock_client)
        assert "log_artifact_id" in kwargs
        assert "output_artifact_id" in kwargs


# ---------------------------------------------------------------------------
# E2E tests: posix residence artifact uploads
# ---------------------------------------------------------------------------


class TestPosixResidenceArtifactUpload:
    """Verify log+output artifact separation works with posix residence."""

    def test_posix_residence_creates_separate_log_artifact(
        self, sample_config, tmp_path: Path
    ):
        """With posix residence, both output and log artifacts should be
        created with residence='posix'."""
        # Configure the profile to use posix residence
        sample_config.profiles["text-embedding:gpu-medium"].output_residence = "posix"
        sample_config.profiles["text-embedding:gpu-medium"].log_residence = "posix"

        daemon, mock_client = _make_daemon(sample_config)

        output_dir = tmp_path / "output"
        output_dir.mkdir()
        (output_dir / "result.csv").write_text("data")
        (output_dir / "slurm-1.out").write_text("log output")

        daemon.tracker.track(
            emx2_job_id="job-hhh-888",
            slurm_job_id="sim-job-hhh",
            status="STARTED",
            output_dir=str(output_dir),
            processor="text-embedding",
            profile="gpu-medium",
        )

        daemon._monitor_running_jobs()

        # Both artifacts should be created with posix residence
        create_calls = mock_client.create_artifact.call_args_list
        assert len(create_calls) == 2
        residences = [c.kwargs["residence"] for c in create_calls]
        assert all(r == "posix" for r in residences)

        # No binary upload_artifact_file calls for posix
        assert mock_client.upload_artifact_file.call_count == 0

        # But register_artifact_file should be called for each file
        # (1 output file + 1 log file = 2 register calls)
        assert mock_client.register_artifact_file.call_count == 2

        # Verify register was called with correct metadata for the output file
        reg_calls = mock_client.register_artifact_file.call_args_list
        reg_kwargs_list = [c.kwargs for c in reg_calls]
        result_reg = next(k for k in reg_kwargs_list if k["path"] == "result.csv")
        assert result_reg["sha256"] == hashlib.sha256(b"data").hexdigest()
        assert result_reg["size_bytes"] == len(b"data")

        # commit_artifact should be called for each artifact
        assert mock_client.commit_artifact.call_count == 2

    def test_posix_registers_file_metadata(self, sample_config, tmp_path: Path):
        """Posix artifact flow registers metadata for each file with correct hashes."""
        sample_config.profiles["text-embedding:gpu-medium"].output_residence = "posix"
        sample_config.profiles["text-embedding:gpu-medium"].log_residence = "posix"
        daemon, mock_client = _make_daemon(sample_config)

        output_dir = tmp_path / "output"
        output_dir.mkdir()
        file_a = output_dir / "alpha.txt"
        file_b = output_dir / "beta.bin"
        file_a.write_text("aaa")
        file_b.write_bytes(b"\x00\x01\x02")

        daemon.tracker.track(
            emx2_job_id="job-posix-meta",
            slurm_job_id="sim-posix-meta",
            status="STARTED",
            output_dir=str(output_dir),
            processor="text-embedding",
            profile="gpu-medium",
        )

        daemon._monitor_running_jobs()

        # register_artifact_file called for each output file (2) + 0 log files
        reg_calls = mock_client.register_artifact_file.call_args_list
        reg_paths = sorted(c.kwargs["path"] for c in reg_calls)
        assert "alpha.txt" in reg_paths
        assert "beta.bin" in reg_paths

        # Verify hashes match
        alpha_reg = next(c.kwargs for c in reg_calls if c.kwargs["path"] == "alpha.txt")
        assert alpha_reg["sha256"] == hashlib.sha256(b"aaa").hexdigest()
        assert alpha_reg["size_bytes"] == 3

        beta_reg = next(c.kwargs for c in reg_calls if c.kwargs["path"] == "beta.bin")
        assert beta_reg["sha256"] == hashlib.sha256(b"\x00\x01\x02").hexdigest()
        assert beta_reg["size_bytes"] == 3

        # No binary uploads
        assert mock_client.upload_artifact_file.call_count == 0

        # commit_artifact called with correct tree hash
        assert mock_client.commit_artifact.call_count >= 1

    def test_mixed_residence_output_posix_log_managed(
        self, sample_config, tmp_path: Path
    ):
        """Output artifacts use posix, log artifacts use managed upload."""
        sample_config.profiles["text-embedding:gpu-medium"].output_residence = "posix"
        sample_config.profiles["text-embedding:gpu-medium"].log_residence = "managed"

        daemon, mock_client = _make_daemon(sample_config)

        output_dir = tmp_path / "output"
        output_dir.mkdir()
        (output_dir / "result.csv").write_text("data")
        (output_dir / "slurm-1.out").write_text("log output")

        daemon.tracker.track(
            emx2_job_id="job-mixed-001",
            slurm_job_id="sim-mixed-001",
            status="STARTED",
            output_dir=str(output_dir),
            processor="text-embedding",
            profile="gpu-medium",
        )

        daemon._monitor_running_jobs()

        # Two artifacts created: one posix (output), one managed (log)
        create_calls = mock_client.create_artifact.call_args_list
        assert len(create_calls) == 2
        residences = [c.kwargs["residence"] for c in create_calls]
        assert "posix" in residences
        assert "managed" in residences

        # Output file registered (posix), log file uploaded (managed)
        assert mock_client.register_artifact_file.call_count == 1  # output
        assert mock_client.upload_artifact_file.call_count == 1  # log

        assert mock_client.commit_artifact.call_count == 2


# ---------------------------------------------------------------------------
# Tree hash computation tests
# ---------------------------------------------------------------------------


def _java_tree_hash(files: dict[str, bytes]) -> str:
    """Reference implementation of the Java tree hash algorithm.

    Single file: SHA-256 of the file content.
    Multi-file: SHA-256 of concatenated "path:sha256hex" sorted by path.
    """
    per_file = sorted(
        (name, hashlib.sha256(content).hexdigest()) for name, content in files.items()
    )
    if len(per_file) == 1:
        return per_file[0][1]
    tree_str = "".join(f"{name}:{h}" for name, h in per_file)
    return hashlib.sha256(tree_str.encode()).hexdigest()


class TestTreeHashComputation:
    """Verify that commit_artifact receives a tree hash matching the Java
    algorithm: single-file = file sha256, multi-file = SHA-256 of sorted
    'path:sha256hex' concatenation."""

    def test_single_file_commit_uses_file_sha256(self, sample_config, tmp_path: Path):
        """Single-file artifact: commit sha256 = the file's own sha256."""
        daemon, mock_client = _make_daemon(sample_config)

        output_dir = tmp_path / "output"
        output_dir.mkdir()
        content = b"single file content"
        (output_dir / "result.csv").write_bytes(content)

        daemon.tracker.track(
            emx2_job_id="job-hash-001",
            slurm_job_id="sim-hash-001",
            status="STARTED",
            output_dir=str(output_dir),
            processor="text-embedding",
            profile="gpu-medium",
        )

        daemon._monitor_running_jobs()

        # commit_artifact called once (output only, no log files)
        assert mock_client.commit_artifact.call_count == 1
        call_kwargs = mock_client.commit_artifact.call_args.kwargs
        expected = hashlib.sha256(content).hexdigest()
        assert call_kwargs["sha256"] == expected

    def test_multi_file_commit_uses_tree_hash(self, sample_config, tmp_path: Path):
        """Multi-file artifact: commit sha256 = tree hash of sorted path:hash pairs."""
        daemon, mock_client = _make_daemon(sample_config)

        output_dir = tmp_path / "output"
        output_dir.mkdir()

        # Only log files so we get a single multi-file artifact
        files = {
            "slurm-123.out": b"slurm log output",
            "container-stderr.log": b"stderr content here",
            "container-stdout.log": b"stdout content here",
        }
        for name, content in files.items():
            (output_dir / name).write_bytes(content)

        daemon.tracker.track(
            emx2_job_id="job-hash-002",
            slurm_job_id="sim-hash-002",
            status="STARTED",
            output_dir=str(output_dir),
            processor="text-embedding",
            profile="gpu-medium",
        )

        daemon._monitor_running_jobs()

        # commit_artifact called once (log artifact only, no output files)
        assert mock_client.commit_artifact.call_count == 1
        call_kwargs = mock_client.commit_artifact.call_args.kwargs
        expected = _java_tree_hash(files)
        assert call_kwargs["sha256"] == expected

    def test_multi_file_hash_is_not_naive_concatenation(
        self, sample_config, tmp_path: Path
    ):
        """The tree hash must NOT be a naive SHA-256 over concatenated content."""
        daemon, mock_client = _make_daemon(sample_config)

        output_dir = tmp_path / "output"
        output_dir.mkdir()

        files = {
            "a.log": b"content-a",
            "b.log": b"content-b",
        }
        for name, content in files.items():
            (output_dir / name).write_bytes(content)

        daemon.tracker.track(
            emx2_job_id="job-hash-003",
            slurm_job_id="sim-hash-003",
            status="STARTED",
            output_dir=str(output_dir),
            processor="text-embedding",
            profile="gpu-medium",
        )

        daemon._monitor_running_jobs()

        call_kwargs = mock_client.commit_artifact.call_args.kwargs
        actual_hash = call_kwargs["sha256"]

        # Naive concatenation hash (the old buggy approach)
        naive = hashlib.sha256(b"content-a" + b"content-b").hexdigest()
        assert actual_hash != naive

        # Should match the tree hash
        assert actual_hash == _java_tree_hash(files)

    def test_tree_hash_sorting_matters(self, sample_config, tmp_path: Path):
        """Tree hash must sort by path, so file creation order doesn't matter."""
        files = {
            "zebra.log": b"z-content",
            "alpha.log": b"a-content",
        }
        expected = _java_tree_hash(files)

        for order in [["zebra.log", "alpha.log"], ["alpha.log", "zebra.log"]]:
            daemon, mock_client = _make_daemon(sample_config)

            output_dir = tmp_path / f"output-{'_'.join(order)}"
            output_dir.mkdir()
            for name in order:
                (output_dir / name).write_bytes(files[name])

            daemon.tracker.track(
                emx2_job_id=f"job-sort-{order[0][:1]}",
                slurm_job_id=f"sim-sort-{order[0][:1]}",
                status="STARTED",
                output_dir=str(output_dir),
                processor="text-embedding",
                profile="gpu-medium",
            )

            daemon._monitor_running_jobs()

            call_kwargs = mock_client.commit_artifact.call_args.kwargs
            assert call_kwargs["sha256"] == expected

    def test_posix_tree_hash_matches_managed(self, sample_config, tmp_path: Path):
        """Posix residence should compute the same tree hash as managed."""
        files = {
            "slurm-1.out": b"slurm log",
            "app.log": b"app log",
        }
        expected = _java_tree_hash(files)

        sample_config.profiles["text-embedding:gpu-medium"].output_residence = "posix"
        sample_config.profiles["text-embedding:gpu-medium"].log_residence = "posix"
        daemon, mock_client = _make_daemon(sample_config)

        output_dir = tmp_path / "output"
        output_dir.mkdir()
        for name, content in files.items():
            (output_dir / name).write_bytes(content)

        daemon.tracker.track(
            emx2_job_id="job-posix-hash",
            slurm_job_id="sim-posix-hash",
            status="STARTED",
            output_dir=str(output_dir),
            processor="text-embedding",
            profile="gpu-medium",
        )

        daemon._monitor_running_jobs()

        # Log artifact commit (the only commit, since no non-log files)
        assert mock_client.commit_artifact.call_count == 1
        call_kwargs = mock_client.commit_artifact.call_args.kwargs
        assert call_kwargs["sha256"] == expected


# ---------------------------------------------------------------------------
# Slurm detail string tests
# ---------------------------------------------------------------------------


class TestBuildSlurmDetail:
    """Verify _build_slurm_detail produces structured detail strings."""

    def test_completed_job_minimal(self):
        info = SlurmJobInfo(state="COMPLETED", exit_code="0:0", reason="None")
        detail = _build_slurm_detail(info)
        assert detail == "slurm_state=COMPLETED"

    def test_failed_with_exit_code(self):
        info = SlurmJobInfo(
            state="FAILED",
            exit_code="137:0",
            reason="NonZeroExitCode",
            node_list="gpu-01",
            elapsed="00:45:12",
        )
        detail = _build_slurm_detail(info)
        assert "slurm_state=FAILED" in detail
        assert "exit_code=137:0" in detail
        assert "reason=NonZeroExitCode" in detail
        assert "node=gpu-01" in detail
        assert "elapsed=00:45:12" in detail

    def test_oom_preserves_slurm_state(self):
        """OUT_OF_MEMORY should appear as slurm_state, not just 'FAILED'."""
        info = SlurmJobInfo(
            state="OUT_OF_MEMORY",
            exit_code="137:0",
            reason="OutOfMemory",
            node_list="compute-02",
        )
        detail = _build_slurm_detail(info)
        assert "slurm_state=OUT_OF_MEMORY" in detail
        assert "reason=OutOfMemory" in detail

    def test_pending_with_reason(self):
        info = SlurmJobInfo(state="PENDING", reason="Priority")
        detail = _build_slurm_detail(info)
        assert "slurm_state=PENDING" in detail
        assert "reason=Priority" in detail

    def test_transition_detail_includes_slurm_metadata(
        self, sample_config, tmp_path: Path
    ):
        """End-to-end: completed job transition should have structured detail."""
        daemon, mock_client = _make_daemon(sample_config)

        output_dir = tmp_path / "output"
        output_dir.mkdir()
        (output_dir / "result.csv").write_text("data")

        daemon.tracker.track(
            emx2_job_id="job-detail-001",
            slurm_job_id="sim-detail-001",
            status="STARTED",
            output_dir=str(output_dir),
            processor="text-embedding",
            profile="gpu-medium",
        )

        daemon._monitor_running_jobs()

        _, status, kwargs = _get_transition_call(mock_client)
        assert status == "COMPLETED"
        # SimulatedBackend returns exit_code="0:0" and reason="None",
        # so detail should be minimal but include slurm_state
        assert "slurm_state=COMPLETED" in kwargs["detail"]
