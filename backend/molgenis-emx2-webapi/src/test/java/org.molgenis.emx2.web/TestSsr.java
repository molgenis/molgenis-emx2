package org.molgenis.emx2.web;

import javax.script.ScriptException;
import org.junit.Test;

public class TestSsr {
  @Test
  public void testSsr() throws ScriptException {
    String result = SsrService.renderRoute("/", "{key:\"hell0\"}");
    System.out.println("found result:\n" + result);
  }
}
