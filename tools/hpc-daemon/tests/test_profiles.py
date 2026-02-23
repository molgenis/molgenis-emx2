"""Tests for profile resolution and capability derivation."""

from emx2_hpc_daemon.profiles import derive_capabilities, resolve_profile


def test_resolve_exact_match(sample_config):
    resolved = resolve_profile(sample_config, "text-embedding", "gpu-medium")
    assert resolved is not None
    assert resolved.sif_image == "/nfs/images/text-embedding_v3.sif"
    assert resolved.partition == "gpu"
    assert resolved.cpus == 8
    assert resolved.memory == "64G"


def test_resolve_no_match(sample_config):
    resolved = resolve_profile(sample_config, "nonexistent", "profile")
    assert resolved is None


def test_derive_capabilities(sample_config):
    caps = derive_capabilities(sample_config)
    assert len(caps) == 2
    processors = {c["processor"] for c in caps}
    assert "text-embedding" in processors
    assert "data-pipeline" in processors
    for cap in caps:
        assert "profile" in cap
        assert "max_concurrent_jobs" in cap
