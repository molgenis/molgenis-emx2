package org.molgenis.emx2.hpc.protocol;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class InputValidatorTest {

  @Test
  void validUuidPasses() {
    assertDoesNotThrow(
        () -> InputValidator.requireUuid("550e8400-e29b-41d4-a716-446655440000", "id"));
  }

  @Test
  void invalidUuidThrows() {
    assertThrows(
        IllegalArgumentException.class, () -> InputValidator.requireUuid("not-a-uuid", "id"));
  }

  @Test
  void nullUuidThrows() {
    assertThrows(IllegalArgumentException.class, () -> InputValidator.requireUuid(null, "id"));
  }

  @Test
  void blankUuidThrows() {
    assertThrows(IllegalArgumentException.class, () -> InputValidator.requireUuid("  ", "id"));
  }

  @Test
  void requireStringRejectsNull() {
    assertThrows(
        IllegalArgumentException.class, () -> InputValidator.requireString(null, "processor"));
  }

  @Test
  void requireStringRejectsBlank() {
    assertThrows(
        IllegalArgumentException.class, () -> InputValidator.requireString("", "processor"));
  }

  @Test
  void requireStringRejectsTooLong() {
    String longStr = "x".repeat(1025);
    assertThrows(
        IllegalArgumentException.class, () -> InputValidator.requireString(longStr, "processor"));
  }

  @Test
  void requireStringAcceptsNormal() {
    assertDoesNotThrow(() -> InputValidator.requireString("text-embedding", "processor"));
  }

  @Test
  void optionalStringAllowsNull() {
    assertDoesNotThrow(() -> InputValidator.optionalString(null, "profile"));
  }

  @Test
  void optionalStringRejectsTooLong() {
    String longStr = "x".repeat(1025);
    assertThrows(
        IllegalArgumentException.class, () -> InputValidator.optionalString(longStr, "profile"));
  }

  @Test
  void optionalTextAllowsLargerStrings() {
    String mediumStr = "x".repeat(5000);
    assertDoesNotThrow(() -> InputValidator.optionalText(mediumStr, "detail"));
  }

  @Test
  void optionalTextRejectsVeryLong() {
    String hugeStr = "x".repeat(65537);
    assertThrows(
        IllegalArgumentException.class, () -> InputValidator.optionalText(hugeStr, "detail"));
  }
}
