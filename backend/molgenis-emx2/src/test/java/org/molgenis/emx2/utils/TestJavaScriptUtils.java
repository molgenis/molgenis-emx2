package org.molgenis.emx2.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.utils.JavaScriptUtils.executeJavascriptOnMap;

import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.utils.generator.IdGenerator;

class TestJavaScriptUtils {

  static IdGenerator idGenerator =
      new IdGenerator() {
        @Override
        public String generateId() {
          return "123-abc";
        }
      };

  @BeforeAll
  public static void beforeAll() {
    JavaScriptUtils.idGenerator = idGenerator;
  }

  @Test
  void testCalculateComputedExpression() {
    String expression = "5 + 7";
    String outcome = executeJavascriptOnMap(expression, Map.of());
    assertEquals(12, Integer.parseInt(outcome));
  }

  @Test
  void testCalculateComputedExpressionWithAutoid() {
    assertEquals("123-abc", executeJavascriptOnMap("${mg_autoid}", Map.of()));
  }

  @Test
  void testCalculateComputedExpressionWithAutoIdAndPrefixAndPostFix() {
    assertEquals("foo-123-abc-bar", executeJavascriptOnMap("foo-${mg_autoid}-bar", Map.of()));
    assertEquals("foo-123-abc", executeJavascriptOnMap("foo-${mg_autoid}", Map.of()));
    assertEquals("123-abc-bar", executeJavascriptOnMap("${mg_autoid}-bar", Map.of()));
  }
}
