package org.molgenis.emx2.tasks;

import org.junit.Assert;
import org.junit.Test;

public class TestTask {

  @Test
  public void test1() throws InterruptedException {
    TaskService taskService = new TaskServiceInMemory();

    Task task = new DummyTask();
    String id = taskService.submit(task);
    System.out.println("Starting ...");

    // purge doesn't change
    Assert.assertEquals(1, taskService.getJobIds().size());

    while (!TaskStatus.COMPLETED.equals(taskService.getTask(id).getStatus())) {
      Thread.sleep(50);
      System.out.println(task);
    }
    System.out.println("Completed ...");
    System.out.println(task);

    Assert.assertEquals(1, taskService.getJobIds().size());

    // purge after complete removes
    taskService.removeOlderThan(0);
    Assert.assertEquals(0, taskService.getJobIds().size());

    taskService.shutdown();
  }
}
