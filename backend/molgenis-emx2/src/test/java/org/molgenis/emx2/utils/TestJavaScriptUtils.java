package org.molgenis.emx2.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.utils.JavaScriptUtils.executeJavascriptOnMap;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("slow")
class TestJavaScriptUtils {

  @Test
  void testCalculateComputedExpression() {
    String expression = "5 + 7";
    Object outcome = executeJavascriptOnMap(expression, Map.of());
    assertEquals(12, outcome);
  }

  @Test
  void testComputedUsingDateTime() {
    String expression = "`The date is: ${date}`";
    LocalDateTime date = LocalDateTime.of(2024, Month.JANUARY, 22, 10, 8);
    Object result = executeJavascriptOnMap(expression, Map.of("date", date));
    String expectedResult = "The date is: 2024-01-22T10:08";
    assertEquals(expectedResult, result);
  }

  @Test
  void testComputedUsingDate() {
    String expression = "`The date is: ${date}`";
    LocalDate date = LocalDate.of(2024, Month.JANUARY, 22);
    Object result = executeJavascriptOnMap(expression, Map.of("date", date));
    String expectedResult = "The date is: 2024-01-22";
    assertEquals(expectedResult, result);
  }
}
