package org.molgenis.emx2.hpc.model;

/**
 * A (processor, profile) pair that a worker advertises it can handle, along with concurrency limit.
 * Example: processor="text-embedding", profile="gpu-medium", maxConcurrentJobs=4
 */
public record WorkerCapability(String processor, String profile, int maxConcurrentJobs) {}
