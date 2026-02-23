package org.molgenis.emx2.hpc.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HpcJobStatusTest {

  @Test
  void pendingCanTransitionToClaimedAndCancelled() {
    assertTrue(HpcJobStatus.PENDING.canTransitionTo(HpcJobStatus.CLAIMED));
    assertTrue(HpcJobStatus.PENDING.canTransitionTo(HpcJobStatus.CANCELLED));
    assertFalse(HpcJobStatus.PENDING.canTransitionTo(HpcJobStatus.COMPLETED));
    assertFalse(HpcJobStatus.PENDING.canTransitionTo(HpcJobStatus.STARTED));
  }

  @Test
  void claimedCanTransitionToSubmittedAndCancelled() {
    assertTrue(HpcJobStatus.CLAIMED.canTransitionTo(HpcJobStatus.SUBMITTED));
    assertTrue(HpcJobStatus.CLAIMED.canTransitionTo(HpcJobStatus.CANCELLED));
    assertFalse(HpcJobStatus.CLAIMED.canTransitionTo(HpcJobStatus.COMPLETED));
    assertFalse(HpcJobStatus.CLAIMED.canTransitionTo(HpcJobStatus.PENDING));
  }

  @Test
  void submittedCanTransitionToStartedAndCancelled() {
    assertTrue(HpcJobStatus.SUBMITTED.canTransitionTo(HpcJobStatus.STARTED));
    assertTrue(HpcJobStatus.SUBMITTED.canTransitionTo(HpcJobStatus.CANCELLED));
    assertFalse(HpcJobStatus.SUBMITTED.canTransitionTo(HpcJobStatus.COMPLETED));
  }

  @Test
  void startedCanTransitionToTerminalStates() {
    assertTrue(HpcJobStatus.STARTED.canTransitionTo(HpcJobStatus.COMPLETED));
    assertTrue(HpcJobStatus.STARTED.canTransitionTo(HpcJobStatus.FAILED));
    assertTrue(HpcJobStatus.STARTED.canTransitionTo(HpcJobStatus.CANCELLED));
    assertFalse(HpcJobStatus.STARTED.canTransitionTo(HpcJobStatus.PENDING));
  }

  @Test
  void terminalStatesHaveNoTransitions() {
    assertTrue(HpcJobStatus.COMPLETED.isTerminal());
    assertTrue(HpcJobStatus.FAILED.isTerminal());
    assertTrue(HpcJobStatus.CANCELLED.isTerminal());
  }

  @Test
  void nonTerminalStatesAreNotTerminal() {
    assertFalse(HpcJobStatus.PENDING.isTerminal());
    assertFalse(HpcJobStatus.CLAIMED.isTerminal());
    assertFalse(HpcJobStatus.SUBMITTED.isTerminal());
    assertFalse(HpcJobStatus.STARTED.isTerminal());
  }
}
