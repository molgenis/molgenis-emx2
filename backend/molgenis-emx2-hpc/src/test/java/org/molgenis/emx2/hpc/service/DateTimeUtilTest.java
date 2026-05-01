package org.molgenis.emx2.hpc.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class DateTimeUtilTest {

  @Test
  void parsesIso8601WithT() {
    LocalDateTime result = DateTimeUtil.parse("2026-03-09T12:50:07");
    assertNotNull(result);
    assertEquals(2026, result.getYear());
    assertEquals(3, result.getMonthValue());
    assertEquals(9, result.getDayOfMonth());
    assertEquals(12, result.getHour());
    assertEquals(50, result.getMinute());
  }

  @Test
  void parsesPostgresFormatWithSpace() {
    LocalDateTime result = DateTimeUtil.parse("2026-03-09 12:50:07.586847");
    assertNotNull(result);
    assertEquals(2026, result.getYear());
    assertEquals(12, result.getHour());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "not-a-date", "2026-13-45"})
  void returnsNullForInvalidInput(String input) {
    assertNull(DateTimeUtil.parse(input));
  }
}
