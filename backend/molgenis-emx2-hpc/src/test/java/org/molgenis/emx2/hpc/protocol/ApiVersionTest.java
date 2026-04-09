package org.molgenis.emx2.hpc.protocol;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class ApiVersionTest {

  @Test
  void validate_acceptsCurrentVersion() {
    assertDoesNotThrow(() -> ApiVersion.validate(ApiVersion.CURRENT));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void validate_rejectsMissingVersion(String version) {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> ApiVersion.validate(version));
    assertTrue(ex.getMessage().contains("Missing required header"));
  }

  @Test
  void validate_rejectsWrongVersion() {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> ApiVersion.validate("1999-01"));
    assertTrue(ex.getMessage().contains("Unsupported API version"));
    assertTrue(ex.getMessage().contains(ApiVersion.CURRENT));
  }
}
