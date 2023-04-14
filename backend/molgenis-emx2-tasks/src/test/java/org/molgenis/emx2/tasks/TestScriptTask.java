package org.molgenis.emx2.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.tasks.TaskStatus.COMPLETED;

import org.junit.jupiter.api.Test;

public class TestScriptTask {

  @Test
  public void testPython() throws InterruptedException {
    System.out.println("first");
    ScriptTask r1 =
        new ScriptTask()
            .name("helloworld")
            .dependencies("numpy==1.23.4")
            .script(
                """
import time
import numpy as np
print('Hello, world!')
a = np.array([1, 2, 3, 4, 5, 6])
print(a)
time.sleep(3)
print('Halfway')
time.sleep(3)
print('Complete')
""");
    r1.run();
    // System.out.println(r1);
    assertEquals(COMPLETED, r1.getStatus());

    System.out.println("\nsecond");
    ScriptTask r2 =
        new ScriptTask()
            .name("error")
            .script("""
import sys
print('Error message', file=sys.stderr)
""");
    // System.out.println(r2);
    r2.run();
  }
}
