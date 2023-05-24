package org.molgenis.emx2.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Row.row;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestTaskServiceScheduler {
  private static String SCHEMA_NAME = TestTaskServiceScheduler.class.getSimpleName();

  @BeforeAll
  public static void init() {
    TestDatabaseFactory.getTestDatabase().dropCreateSchema(SCHEMA_NAME);
  }

  @Test
  public void testTaskServiceScheduler() throws InterruptedException {
    TaskServiceInDatabase service = new TaskServiceInDatabase(SCHEMA_NAME);
    TaskServiceScheduler scheduler = new TaskServiceScheduler(service);

    Row scriptRow = row("name", "test", "script", "print('hello');", "cron", "0/1 * * * * ?");

    service.getScriptTable().save(scriptRow);

    scheduler.schedule(new ScriptTask(scriptRow));
    assertTrue(scheduler.scheduledTaskNames().contains("test"));

    // see if jobs emerge
    int waitCount = 0;
    while ((service.getJobTable().retrieveRows().size() == 0
            || !TaskStatus.COMPLETED.equals(
                new ScriptTask(service.getJobTable().retrieveRows().get(0)).getStatus()))
        && waitCount < 10) {
      waitCount++;
      Thread.sleep(500);
    }
    // ok, some jobs were happenning
    assertTrue(service.getJobTable().retrieveRows().size() > 0);
    scheduler.unschedule("test");

    // ok, give jobs time to finish
    Thread.sleep(3000);
    service.getJobTable().truncate();
    // see that no new jobs emerge
    Thread.sleep(3000);
    assertEquals(0, service.getJobTable().retrieveRows().size());
  }
}
