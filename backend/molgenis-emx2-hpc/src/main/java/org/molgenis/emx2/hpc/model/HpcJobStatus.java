package org.molgenis.emx2.hpc.model;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * State machine for HPC job lifecycle. Transitions:
 *
 * <pre>
 *   PENDING → CLAIMED → SUBMITTED → STARTED → COMPLETED
 *                                            → FAILED
 *   PENDING → CANCELLED
 *   CLAIMED → CANCELLED
 *   SUBMITTED → CANCELLED
 *   STARTED → CANCELLED
 *   STARTED → FAILED
 * </pre>
 */
public enum HpcJobStatus {
  PENDING,
  CLAIMED,
  SUBMITTED,
  STARTED,
  COMPLETED,
  FAILED,
  CANCELLED;

  // Use HashSet to avoid EnumSet.noneOf() in constructor (enum not yet fully initialized)
  private Set<HpcJobStatus> allowedTransitions = new HashSet<>();

  static {
    PENDING.allowedTransitions = EnumSet.of(CLAIMED, CANCELLED);
    CLAIMED.allowedTransitions = EnumSet.of(SUBMITTED, CANCELLED);
    SUBMITTED.allowedTransitions = EnumSet.of(STARTED, CANCELLED);
    STARTED.allowedTransitions = EnumSet.of(COMPLETED, FAILED, CANCELLED);
  }

  /** Returns true if transitioning from this status to {@code target} is valid. */
  public boolean canTransitionTo(HpcJobStatus target) {
    return allowedTransitions.contains(target);
  }

  /** Returns the set of statuses reachable from this status. */
  public Set<HpcJobStatus> allowedTransitions() {
    return Set.copyOf(allowedTransitions);
  }

  /** Returns true if this is a terminal state (no further transitions possible). */
  public boolean isTerminal() {
    return allowedTransitions.isEmpty();
  }
}
