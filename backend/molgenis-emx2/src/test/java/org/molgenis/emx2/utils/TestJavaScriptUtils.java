package org.molgenis.emx2.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.utils.JavaScriptUtils.executeJavascriptOnMap;

import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("slow")
class TestJavaScriptUtils {

  @Test
  void testCalculateComputedExpression() {
    String expression = "5 + 7";
    String outcome = executeJavascriptOnMap(expression, Map.of());
    assertEquals(12, Integer.parseInt(outcome));
  }
}
