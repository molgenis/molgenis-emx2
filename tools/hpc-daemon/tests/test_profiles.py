"""Tests for profile resolution and capability derivation."""

from emx2_hpc_daemon.profiles import derive_capabilities, resolve_profile


def test_resolve_exact_match(sample_config):
    resolved = resolve_profile(sample_config, "text-embedding", "gpu-medium")
    assert resolved is not None
    assert resolved.sif_image == "/nfs/images/text-embedding_v3.sif"
    assert resolved.partition == "gpu"
    assert resolved.cpus == 8
    assert resolved.memory == "64G"


def test_resolve_default_residence(sample_config):
    resolved = resolve_profile(sample_config, "text-embedding", "gpu-medium")
    assert resolved is not None
    assert resolved.output_residence == "managed"
    assert resolved.log_residence == "managed"


def test_resolve_posix_residence(sample_config):
    from emx2_hpc_daemon.config import ProfileEntry

    sample_config.profiles["posix-processor:default"] = ProfileEntry(
        sif_image="/nfs/images/posix.sif",
        output_residence="posix",
        log_residence="posix",
    )
    resolved = resolve_profile(sample_config, "posix-processor", "default")
    assert resolved is not None
    assert resolved.output_residence == "posix"
    assert resolved.log_residence == "posix"


def test_resolve_mixed_residence(sample_config):
    from emx2_hpc_daemon.config import ProfileEntry

    sample_config.profiles["mixed:default"] = ProfileEntry(
        sif_image="/nfs/images/mixed.sif",
        output_residence="posix",
        log_residence="managed",
    )
    resolved = resolve_profile(sample_config, "mixed", "default")
    assert resolved is not None
    assert resolved.output_residence == "posix"
    assert resolved.log_residence == "managed"


def test_resolve_no_match(sample_config):
    resolved = resolve_profile(sample_config, "nonexistent", "profile")
    assert resolved is None


def test_resolve_entrypoint_profile(sample_config):
    from emx2_hpc_daemon.config import ProfileEntry

    sample_config.profiles["vtm-pipeline:gpu-large"] = ProfileEntry(
        entrypoint="/nfs/scripts/vtm-pipeline.sh",
        partition="gpu",
        cpus=16,
        memory="128G",
        time="08:00:00",
    )
    resolved = resolve_profile(sample_config, "vtm-pipeline", "gpu-large")
    assert resolved is not None
    assert resolved.entrypoint == "/nfs/scripts/vtm-pipeline.sh"
    assert resolved.sif_image == ""
    assert resolved.partition == "gpu"
    assert resolved.cpus == 16


def test_derive_capabilities(sample_config):
    caps = derive_capabilities(sample_config)
    assert len(caps) == 2
    processors = {c["processor"] for c in caps}
    assert "text-embedding" in processors
    assert "data-pipeline" in processors
    for cap in caps:
        assert "profile" in cap
        assert "max_concurrent_jobs" in cap
