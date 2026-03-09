package org.molgenis.emx2.hpc.service;

import java.time.LocalDateTime;

/**
 * Utility for parsing datetime strings from the database. PostgreSQL returns timestamps in the
 * format {@code 2026-03-09 12:50:07.586847} (space-separated), but {@link LocalDateTime#parse}
 * expects ISO-8601 format with a {@code T} separator. This helper normalises the format before
 * parsing.
 */
final class DateTimeUtil {

  private DateTimeUtil() {}

  /**
   * Parses a datetime string that may use either a space or {@code T} as the date/time separator.
   * Returns {@code null} if the input is null, blank, or unparseable.
   */
  static LocalDateTime parse(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return LocalDateTime.parse(value.replace(' ', 'T'));
    } catch (Exception e) {
      return null;
    }
  }
}
