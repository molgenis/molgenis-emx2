package org.molgenis.emx2.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.MolgenisException;

@Tag("slow")
public class TestTask {

  @Test
  public void test1() throws InterruptedException {
    TaskService taskService = new TaskServiceInMemory();

    Task task = new DummyTask();
    String id = taskService.submit(task);
    System.out.println("Starting ...");

    // purge doesn't change
    assertEquals(1, taskService.getJobIds().size());

    while (!TaskStatus.COMPLETED.equals(taskService.getTask(id).getStatus())) {
      Thread.sleep(50);
      System.out.println(task);
    }
    System.out.println("Completed ...");
    System.out.println(task);

    assertEquals(1, taskService.getJobIds().size());

    // purge after complete removes
    taskService.removeOlderThan(0);
    assertEquals(0, taskService.getJobIds().size());

    taskService.shutdown();
  }

  @Test
  public void testScheduleSubTaskForEmptyParentTask_shouldThrow() {
    TaskService taskService = new TaskServiceInMemory();
    Task emptyParentTask = new Task();
    Task subTask = new Task();

    taskService.submit(emptyParentTask);
    subTask.setParentTask(emptyParentTask);
    assertThrows(MolgenisException.class, () -> taskService.submit(subTask));

    taskService.removeOlderThan(0);
    taskService.shutdown();
  }
}
