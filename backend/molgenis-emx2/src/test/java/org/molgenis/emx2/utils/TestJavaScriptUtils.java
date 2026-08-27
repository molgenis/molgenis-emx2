package org.molgenis.emx2.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.utils.JavaScriptUtils.executeJavascriptOnMap;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.MolgenisException;

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

  // Objects/arrays must be detached from the polyglot context before it is closed; without that,
  // any access to the result below throws 'The Context is already closed'.
  @Test
  void testComputedReturningNestedObjectsIsUsableAfterEvaluation() {
    Object result = executeJavascriptOnMap("[{name: 'x', nested: {n: 1}}, {name: 'y'}]", Map.of());
    List<?> list = (List<?>) result;
    assertEquals(2, list.size());
    Map<?, ?> first = (Map<?, ?>) list.get(0);
    assertEquals("x", first.get("name"));
    assertEquals(1, ((Map<?, ?>) first.get("nested")).get("n"));
    assertEquals("y", ((Map<?, ?>) list.get(1)).get("name"));
  }

  @Test
  void testComputedReturningListAsListClass() {
    Object result = executeJavascriptOnMap("['a', 'b'].concat(['c'])", Map.of(), List.class);
    assertEquals(List.of("a", "b", "c"), result);
  }

  @Test
  void testCyclicResultGivesCleanErrorInsteadOfStackOverflow() {
    String expression = "const a = {}; a.self = a; a";
    MolgenisException e =
        assertThrows(MolgenisException.class, () -> executeJavascriptOnMap(expression, Map.of()));
    assertTrue(e.getMessage().contains("nesting depth"), e.getMessage());
  }

  @Test
  void testEs6MapWithObjectKeysIsDetached() {
    Object result = executeJavascriptOnMap("new Map([[{id: 1}, 'v']])", Map.of());
    Map.Entry<?, ?> entry = ((Map<?, ?>) result).entrySet().iterator().next();
    assertEquals(1, ((Map<?, ?>) entry.getKey()).get("id"));
    assertEquals("v", entry.getValue());
  }
}
