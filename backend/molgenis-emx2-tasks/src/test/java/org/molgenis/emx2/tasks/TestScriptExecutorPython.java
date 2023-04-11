package org.molgenis.emx2.tasks;

import org.junit.jupiter.api.Test;

public class TestScriptExecutorPython {

  @Test
  public void testPython() throws InterruptedException {
    System.out.println("first");
    ScriptTask r1 =
        new ScriptTask()
            .name("helloworld")
            .script(
                "import time\nprint('Hello, world!')\ntime.sleep(3)\nprint('Halfway')\ntime.sleep(3)\nprint('Complete')");
    r1.run();

    System.out.println("\nsecond");
    ScriptTask r2 =
        new ScriptTask()
            .name("error")
            .script("import sys\nprint('Error message', file=sys.stderr)");
    r2.run();
  }
}
