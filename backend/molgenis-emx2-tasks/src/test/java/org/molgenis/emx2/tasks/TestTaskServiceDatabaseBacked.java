package org.molgenis.emx2.tasks;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestTaskServiceDatabaseBacked {
  private static Database database;

  @BeforeAll
  public static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testTaskServiceDatabaseBacked() throws InterruptedException {
    Schema testSchema =
        database.dropCreateSchema(TestTaskServiceDatabaseBacked.class.getSimpleName());
    TaskService taskService = new TaskServiceInDatabase(testSchema);
    DummyTask dummyTask = new DummyTask();
    taskService.submit(dummyTask);

    // wait until done, or too long
    int count = 0;
    while (!dummyTask.getStatus().equals(TaskStatus.COMPLETED) && count < 100) {
      Thread.sleep(50);
    }
  }
}
