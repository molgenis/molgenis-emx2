package org.molgenis.emx2.hpc.protocol;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ApiVersionTest {

  @Test
  void validVersionDoesNotThrow() {
    assertDoesNotThrow(() -> ApiVersion.validate("2025-01"));
  }

  @Test
  void nullVersionThrows() {
    assertThrows(IllegalArgumentException.class, () -> ApiVersion.validate(null));
  }

  @Test
  void wrongVersionThrows() {
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> ApiVersion.validate("2024-01"));
    assertTrue(e.getMessage().contains("Unsupported"));
  }

  @Test
  void blankVersionThrows() {
    assertThrows(IllegalArgumentException.class, () -> ApiVersion.validate("  "));
  }
}
