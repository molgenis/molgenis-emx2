package org.molgenis.emx2.hpc.service;

import org.molgenis.emx2.Row;

/**
 * Result of an atomic job claim attempt. Distinguishes between success, the job not being in
 * PENDING status, and the worker lacking a matching capability.
 */
public record ClaimResult(Row job, ClaimOutcome outcome) {

  public enum ClaimOutcome {
    SUCCESS,
    NOT_PENDING,
    CAPABILITY_MISMATCH,
    CAPACITY_EXCEEDED
  }

  public boolean isSuccess() {
    return outcome == ClaimOutcome.SUCCESS;
  }

  public static ClaimResult success(Row job) {
    return new ClaimResult(job, ClaimOutcome.SUCCESS);
  }

  public static ClaimResult notPending() {
    return new ClaimResult(null, ClaimOutcome.NOT_PENDING);
  }

  public static ClaimResult capabilityMismatch() {
    return new ClaimResult(null, ClaimOutcome.CAPABILITY_MISMATCH);
  }

  public static ClaimResult capacityExceeded() {
    return new ClaimResult(null, ClaimOutcome.CAPACITY_EXCEEDED);
  }
}
