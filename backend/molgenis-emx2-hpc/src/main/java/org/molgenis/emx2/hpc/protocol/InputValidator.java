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

  /**
   * Validates an artifact file path. Rejects paths that are absolute, contain traversal segments
   * ({@code ..}), null bytes, or exceed the maximum length. Mirrors the defense in {@link
   * #validateContentUrl} for the file upload path.
   */
  public static void validateFilePath(String path, String fieldName) {
    if (path == null || path.isBlank()) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
    if (path.length() > MAX_STRING_LENGTH) {
      throw new IllegalArgumentException(
          fieldName + " must be at most " + MAX_STRING_LENGTH + " characters");
    }
    if (path.indexOf('\0') >= 0) {
      throw new IllegalArgumentException(fieldName + " must not contain null bytes");
    }
    if (path.startsWith("/") || path.startsWith("\\")) {
      throw new IllegalArgumentException(fieldName + " must be a relative path");
    }
    for (String segment : path.replace('\\', '/').split("/")) {
      if ("..".equals(segment)) {
        throw new IllegalArgumentException(
            fieldName + " must not contain path traversal (..) segments");
      }
    }
  }

  /**
   * Validates content_url for posix artifacts. For posix residence, requires a {@code file://}
   * scheme pointing to an absolute path with no traversal ({@code ..}) segments or null bytes.
   */
  public static void validateContentUrl(String contentUrl, String residence) {
    if (!"posix".equals(residence)) {
      return; // only validate for posix
    }
    if (contentUrl == null || contentUrl.isBlank()) {
      throw new IllegalArgumentException("content_url is required for posix artifacts");
    }
    if (contentUrl.length() > MAX_STRING_LENGTH) {
      throw new IllegalArgumentException(
          "content_url must be at most " + MAX_STRING_LENGTH + " characters");
    }
    if (contentUrl.indexOf('\0') >= 0) {
      throw new IllegalArgumentException("content_url must not contain null bytes");
    }
    if (!contentUrl.startsWith("file://")) {
      throw new IllegalArgumentException("content_url must start with file:// for posix artifacts");
    }
    String path = contentUrl.substring("file://".length());
    if (!path.startsWith("/")) {
      throw new IllegalArgumentException(
          "content_url must be an absolute path (file:///...) for posix artifacts");
    }
    // Check for path traversal
    for (String segment : path.split("/")) {
      if ("..".equals(segment)) {
        throw new IllegalArgumentException(
            "content_url must not contain path traversal (..) segments");
      }
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
