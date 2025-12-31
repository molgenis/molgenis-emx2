package org.molgenis.emx2.tasks;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Row.row;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestTaskServiceScheduler {
  private static String SCHEMA_NAME = TestTaskServiceScheduler.class.getSimpleName();
  Logger logger = LoggerFactory.getLogger(TestTaskServiceScheduler.class.getSimpleName());

  @BeforeAll
  public static void init() {
    TestDatabaseFactory.getTestDatabase().dropCreateSchema(SCHEMA_NAME);
  }

  @Test
  @Disabled
  public void testTaskServiceScheduler() throws InterruptedException, SchedulerException {
    TaskServiceInDatabase service = new TaskServiceInDatabase(SCHEMA_NAME, null);
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

    // ok, give jobs time to terminate, lets check at source
    waitCount = 0;
    List<JobExecutionContext> runningJobs = scheduler.quartzScheduler.getCurrentlyExecutingJobs();
    scheduler.unschedule("test");
    while (runningJobs.size() > 0) {
      logger.debug("Waiting for jobs to terminate: " + runningJobs.size());
      Thread.sleep(1000);
      runningJobs = scheduler.quartzScheduler.getCurrentlyExecutingJobs();
      if (waitCount++ > 10) {
        fail("polling took too long");
      }
    }
    // allowing running steps to finish
    Thread.sleep(5000); // wait for processes to be killed
    // remove the jobs
    service.getJobTable().truncate();
    // check no new jobs emerge in reasonable time
    Thread.sleep(5000);
    // give running python time to terminate
    assertEquals(0, service.getJobTable().retrieveRows().size());
  }

  @Test
  public void testCronPersistence() {
    TaskServiceInDatabase service = new TaskServiceInDatabase(SCHEMA_NAME, null);
    TaskServiceScheduler scheduler = new TaskServiceScheduler(service);

    // not there from before
    assertEquals(1, service.getScripts().size());

    // add a new script (listener will submit it)
    Row scriptRow = row("name", "test", "script", "print('hello');", "cron", "0 0 0 1 * ?");
    service.getScriptTable().save(scriptRow);

    // ensure it is in the database and can be retrieved
    assertEquals("test", service.getScripts().get(1).getName());

    // destroy and recreate see if task comes back
    scheduler.shutdown();
    service = new TaskServiceInDatabase(SCHEMA_NAME, null);

    // make sure only 'test' is in the scheduled tasks, not other scripts
    scheduler = new TaskServiceScheduler(service);
    assertEquals("test", scheduler.scheduledTaskNames().get(0));
  }
}
