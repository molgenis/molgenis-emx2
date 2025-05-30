package org.molgenis.emx2.tasks;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.tasks.ScriptType.BASH;
import static org.molgenis.emx2.tasks.ScriptType.PYTHON;
import static org.molgenis.emx2.tasks.TaskStatus.COMPLETED;
import static org.molgenis.emx2.tasks.TaskStatus.ERROR;

import java.net.MalformedURLException;
import java.net.URL;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("slow")
public class TestScriptTask {

  @Tag("windowsFail")
  @Test
  public void testPython() throws InterruptedException {
    System.out.println("first");
    ScriptTask r1 =
        new ScriptTask("hello")
            .type(PYTHON)
            .dependencies("numpy==2.2.4")
            // example with some characters that need escaping
            .parameters("\"netherlands & world\"")
            .script(
                """
import time
import numpy as np
import sys
# you can get parameters via sys.argv[1]
print('Hello, '+sys.argv[1]+'!')
a = np.array([1, 2, 3, 4, 5, 6])
print(a)
time.sleep(1)
print('Halfway')
time.sleep(1)
print('Complete')
""");
    r1.run();
    if (ERROR.equals(r1.getStatus())) {
      System.out.println(r1);
      fail(r1.getSubTasks().stream().map(Task::getDescription).toList().toString());
    } else {
      assertEquals(COMPLETED, r1.getStatus());
    }

    // check for the arguments
    assertTrue(r1.toString().contains("world"));

    System.out.println("\nsecond");
    ScriptTask r2 =
        new ScriptTask("error")
            .type(PYTHON)
            .script(
                """
import sys
print('Error message', file=sys.stderr)
""");
    // System.out.println(r2);
    r2.run();
  }

  @Test
  public void testBashScript() {
    Task bashTask =
        new ScriptTask("bashTest")
            .type(BASH)
            .script(
                """
echo "hello world"
ls -la
echo "bey"
""");
    bashTask.run();
    assertEquals(bashTask.getStatus(), COMPLETED);
  }

  @Test
  public void testPythonScript_shouldFail() throws MalformedURLException {
    Task task =
        new ScriptTask("error")
            .type(PYTHON)
            .script(
                """
import sys
failureVariable = fail
print('unreachable')
""")
            .setServerUrl(new URL("http://localhost:8080/"))
            .failureAddress("test@test.com");
    task.run();
    assertEquals(task.getStatus(), ERROR);
  }
}
