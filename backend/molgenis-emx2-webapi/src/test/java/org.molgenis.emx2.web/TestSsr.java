package org.molgenis.emx2.web;

import java.util.Map;
import javax.script.ScriptException;
import org.junit.Test;

public class TestSsr {
  @Test
  public void testSsr() throws ScriptException {
    String result = SsrService.renderRoute("/", Map.of("key", "hello world"));
    System.out.println("found result:\n" + result);
  }
}
