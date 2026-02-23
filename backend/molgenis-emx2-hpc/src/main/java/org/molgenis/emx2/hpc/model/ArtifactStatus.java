package org.molgenis.emx2.hpc.model;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * State machine for artifact lifecycle. Two paths:
 *
 * <pre>
 *   Managed upload:   CREATED → UPLOADING → COMMITTED
 *                     CREATED → FAILED
 *                     UPLOADING → FAILED
 *   External ref:     REGISTERED → COMMITTED
 *                     REGISTERED → FAILED
 * </pre>
 */
public enum ArtifactStatus {
  CREATED,
  UPLOADING,
  REGISTERED,
  COMMITTED,
  FAILED;

  private Set<ArtifactStatus> allowedTransitions = new HashSet<>();

  static {
    CREATED.allowedTransitions = EnumSet.of(UPLOADING, FAILED);
    UPLOADING.allowedTransitions = EnumSet.of(COMMITTED, FAILED);
    REGISTERED.allowedTransitions = EnumSet.of(COMMITTED, FAILED);
  }

  public boolean canTransitionTo(ArtifactStatus target) {
    return allowedTransitions.contains(target);
  }

  public Set<ArtifactStatus> allowedTransitions() {
    return Set.copyOf(allowedTransitions);
  }

  public boolean isTerminal() {
    return allowedTransitions.isEmpty();
  }
}
