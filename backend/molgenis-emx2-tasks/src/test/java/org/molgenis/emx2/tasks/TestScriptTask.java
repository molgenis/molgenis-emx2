package org.molgenis.emx2.tasks;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Constants.SYSTEM_SCHEMA;
import static org.molgenis.emx2.tasks.ScriptType.BASH;
import static org.molgenis.emx2.tasks.ScriptType.PYTHON;
import static org.molgenis.emx2.tasks.TaskStatus.COMPLETED;
import static org.molgenis.emx2.tasks.TaskStatus.ERROR;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Objects;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.ImportDirectoryTask;
import org.molgenis.emx2.sql.SqlDatabase;

@Tag("slow")
public class TestScriptTask {

  @Tag("windowsFail")
  @Test
  public void testPython() {
    System.out.println("first");
    ScriptTask r1 =
        new ScriptTask("hello")
            .type(PYTHON)
            // example with some characters that need escaping
            .parameters("\"netherlands & world\"")
            .extraFile(new Row())
            .script(
                """
import time
import sys
# you can get parameters via sys.argv[1]
print('Hello, '+sys.argv[1]+'!')
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
    assertEquals(COMPLETED, bashTask.getStatus());
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
            .setServerUrl(URI.create("http://localhost:8080/").toURL())
            .failureAddress("test@test.com");
    task.run();
    assertEquals(ERROR, task.getStatus());
  }

  @Test
  public void testPythonExtraFiles() throws MalformedURLException, InterruptedException {
    TaskServiceInDatabase taskService =
        new TaskServiceInDatabase(SYSTEM_SCHEMA, URI.create("http://localhost:8080/").toURL());

    SqlDatabase database = new SqlDatabase(true);
    database.becomeAdmin();
    Schema schema = database.getSchema(SYSTEM_SCHEMA);

    ClassLoader classLoader = getClass().getClassLoader();
    Path path =
        new File(Objects.requireNonNull(classLoader.getResource("TestScriptTask")).getFile())
            .toPath();
    ImportDirectoryTask importDirectoryTask = new ImportDirectoryTask(path, schema, false);
    importDirectoryTask.run();

    Task csvTask =
        taskService.getTask(
            taskService.submit(taskService.getScript("CSV attachment test").parameters("")));
    TaskStatus csvTaskStatus = csvTask.getStatus();
    while (csvTaskStatus != COMPLETED && csvTaskStatus != ERROR) {
      Thread.sleep(1000);
      csvTaskStatus = csvTask.getStatus();
    }
    Task zipTask =
        taskService.getTask(
            taskService.submit(taskService.getScript("ZIP attachment test").parameters("")));
    TaskStatus zipTaskStatus = zipTask.getStatus();
    while (zipTaskStatus != COMPLETED && zipTaskStatus != ERROR) {
      Thread.sleep(1000);
      zipTaskStatus = zipTask.getStatus();
    }
    assertEquals(COMPLETED, csvTaskStatus);
    assertEquals(COMPLETED, zipTaskStatus);
  }
}
