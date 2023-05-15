package org.molgenis.emx2.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.utils.generator.IdGenerator;

class TestJavaScriptUtils {

  IdGenerator idGenerator =
      new IdGenerator() {
        @Override
        public String generateId() {
          return "123-abc";
        }
      };
  private final JavaScriptUtils jsUtils = new JavaScriptUtils(idGenerator);

  @Test
  void testCalculateComputedExpression() {
    String expression = "5 + 7";
    String outcome = jsUtils.executeJavascriptOnMap(expression, Map.of());
    assertEquals(12, Integer.parseInt(outcome));
  }

  @Test
  void testCalculateComputedExpressionWithAutoid() {
    assertEquals("123-abc", jsUtils.executeJavascriptOnMap("${mg_autoid}", Map.of()));
  }

  @Test
  void testCalculateComputedExpressionWithAutoIdAndPrefixAndPostFix() {
    assertEquals(
        "foo-123-abc-bar",
        jsUtils.executeJavascriptOnMap("'foo-' + ${mg_autoid} + '-bar' ", Map.of()));
    assertEquals("foo-123-abc", jsUtils.executeJavascriptOnMap("'foo-' + ${mg_autoid}", Map.of()));
    assertEquals("123-abc-bar", jsUtils.executeJavascriptOnMap("${mg_autoid} + '-bar' ", Map.of()));
  }
}
