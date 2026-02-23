package org.molgenis.emx2.hpc.protocol;

import java.util.UUID;

/**
 * Input validation utilities for HPC API endpoints. Validates path parameters, required fields, and
 * string length bounds.
 */
public final class InputValidator {

  private static final int MAX_STRING_LENGTH = 1024;
  private static final int MAX_TEXT_LENGTH = 65536;

  private InputValidator() {}

  /** Validates that a path parameter is a valid UUID format. */
  public static void requireUuid(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
    try {
      UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(fieldName + " must be a valid UUID format");
    }
  }

  /** Validates that a required string field is present and within bounds. */
  public static void requireString(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
    if (value.length() > MAX_STRING_LENGTH) {
      throw new IllegalArgumentException(
          fieldName + " must be at most " + MAX_STRING_LENGTH + " characters");
    }
  }

  /** Validates an optional string field is within bounds. */
  public static void optionalString(String value, String fieldName) {
    if (value != null && value.length() > MAX_STRING_LENGTH) {
      throw new IllegalArgumentException(
          fieldName + " must be at most " + MAX_STRING_LENGTH + " characters");
    }
  }

  /** Validates an optional text field (larger bound) is within bounds. */
  public static void optionalText(String value, String fieldName) {
    if (value != null && value.length() > MAX_TEXT_LENGTH) {
      throw new IllegalArgumentException(
          fieldName + " must be at most " + MAX_TEXT_LENGTH + " characters");
    }
  }

  /** Parses an integer query parameter, returning the default on null or invalid input. */
  public static int parseIntParam(String value, int defaultValue) {
    if (value == null) return defaultValue;
    try {
      int v = Integer.parseInt(value);
      return Math.max(0, v);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }
}
