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

  // --- validateContentUrl ---

  @Test
  void posixContentUrlValidAbsolutePath() {
    assertDoesNotThrow(
        () -> InputValidator.validateContentUrl("file:///data/shared/output", "posix"));
  }

  @Test
  void posixContentUrlRejectsTraversal() {
    assertThrows(
        IllegalArgumentException.class,
        () -> InputValidator.validateContentUrl("file:///data/jobs/../etc/passwd", "posix"));
  }

  @Test
  void posixContentUrlRejectsWrongScheme() {
    assertThrows(
        IllegalArgumentException.class,
        () -> InputValidator.validateContentUrl("http://evil.com/payload", "posix"));
  }

  @Test
  void posixContentUrlRejectsBarePath() {
    assertThrows(
        IllegalArgumentException.class,
        () -> InputValidator.validateContentUrl("/bare/path", "posix"));
  }

  @Test
  void posixContentUrlRejectsRelativePath() {
    assertThrows(
        IllegalArgumentException.class,
        () -> InputValidator.validateContentUrl("file://relative/path", "posix"));
  }

  @Test
  void posixContentUrlRejectsNull() {
    assertThrows(
        IllegalArgumentException.class, () -> InputValidator.validateContentUrl(null, "posix"));
  }

  @Test
  void managedContentUrlAllowsNull() {
    assertDoesNotThrow(() -> InputValidator.validateContentUrl(null, "managed"));
  }

  @Test
  void posixContentUrlRejectsNullBytes() {
    assertThrows(
        IllegalArgumentException.class,
        () -> InputValidator.validateContentUrl("file:///data/\0evil", "posix"));
  }

  @Test
  void nonPosixResidenceSkipsValidation() {
    assertDoesNotThrow(() -> InputValidator.validateContentUrl(null, null));
    assertDoesNotThrow(() -> InputValidator.validateContentUrl("anything", "managed"));
  }

  // --- validateFilePath ---

  @Test
  void filePathAcceptsSimpleName() {
    assertDoesNotThrow(() -> InputValidator.validateFilePath("result.txt", "path"));
  }

  @Test
  void filePathAcceptsNestedPath() {
    assertDoesNotThrow(() -> InputValidator.validateFilePath("output/data/result.json", "path"));
  }

  @Test
  void filePathRejectsNull() {
    assertThrows(
        IllegalArgumentException.class, () -> InputValidator.validateFilePath(null, "path"));
  }

  @Test
  void filePathRejectsBlank() {
    assertThrows(
        IllegalArgumentException.class, () -> InputValidator.validateFilePath("  ", "path"));
  }

  @Test
  void filePathRejectsAbsolute() {
    assertThrows(
        IllegalArgumentException.class,
        () -> InputValidator.validateFilePath("/etc/shadow", "path"));
  }

  @Test
  void filePathRejectsBackslashAbsolute() {
    assertThrows(
        IllegalArgumentException.class,
        () -> InputValidator.validateFilePath("\\windows\\system32", "path"));
  }

  @Test
  void filePathRejectsTraversal() {
    assertThrows(
        IllegalArgumentException.class,
        () -> InputValidator.validateFilePath("../../etc/shadow", "path"));
  }

  @Test
  void filePathRejectsMiddleTraversal() {
    assertThrows(
        IllegalArgumentException.class,
        () -> InputValidator.validateFilePath("output/../../../etc/passwd", "path"));
  }

  @Test
  void filePathRejectsNullBytes() {
    assertThrows(
        IllegalArgumentException.class,
        () -> InputValidator.validateFilePath("result\0.txt", "path"));
  }

  @Test
  void filePathRejectsTooLong() {
    String longPath = "x".repeat(1025);
    assertThrows(
        IllegalArgumentException.class, () -> InputValidator.validateFilePath(longPath, "path"));
  }
}
