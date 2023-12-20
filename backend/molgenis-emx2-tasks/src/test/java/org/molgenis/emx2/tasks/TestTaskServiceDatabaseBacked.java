package org.molgenis.emx2.tasks;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

@Tag("slow")
public class TestTaskServiceDatabaseBacked {
  private static Database database;

  @BeforeAll
  public static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testTaskServiceDatabaseBacked() throws InterruptedException {
    // we don't use 'ADMIN' schema
    Schema testSchema =
        database.dropCreateSchema(TestTaskServiceDatabaseBacked.class.getSimpleName());
    TaskServiceInDatabase taskService = new TaskServiceInDatabase(testSchema.getName());
    DummyTask dummyTask = new DummyTask();
    String id = taskService.submit(dummyTask);

    // wait until done, or too long
    int count = 0;
    while (!dummyTask.getStatus().equals(TaskStatus.COMPLETED) && count < 100) {
      Thread.sleep(50);
    }

    // check if we can retreive it from database
    Task task = taskService.getTaskFromDatabase(id);
    assertNotNull(task);
  }
}
