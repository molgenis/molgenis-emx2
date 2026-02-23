package org.molgenis.emx2.hpc.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ArtifactStatusTest {

  @Test
  void createdCanTransitionToUploadingAndFailed() {
    assertTrue(ArtifactStatus.CREATED.canTransitionTo(ArtifactStatus.UPLOADING));
    assertTrue(ArtifactStatus.CREATED.canTransitionTo(ArtifactStatus.FAILED));
    assertFalse(ArtifactStatus.CREATED.canTransitionTo(ArtifactStatus.COMMITTED));
  }

  @Test
  void uploadingCanTransitionToCommittedAndFailed() {
    assertTrue(ArtifactStatus.UPLOADING.canTransitionTo(ArtifactStatus.COMMITTED));
    assertTrue(ArtifactStatus.UPLOADING.canTransitionTo(ArtifactStatus.FAILED));
    assertFalse(ArtifactStatus.UPLOADING.canTransitionTo(ArtifactStatus.CREATED));
  }

  @Test
  void registeredCanTransitionToCommittedAndFailed() {
    assertTrue(ArtifactStatus.REGISTERED.canTransitionTo(ArtifactStatus.COMMITTED));
    assertTrue(ArtifactStatus.REGISTERED.canTransitionTo(ArtifactStatus.FAILED));
    assertFalse(ArtifactStatus.REGISTERED.canTransitionTo(ArtifactStatus.UPLOADING));
  }

  @Test
  void terminalStatesHaveNoTransitions() {
    assertTrue(ArtifactStatus.COMMITTED.isTerminal());
    assertTrue(ArtifactStatus.FAILED.isTerminal());
  }
}
