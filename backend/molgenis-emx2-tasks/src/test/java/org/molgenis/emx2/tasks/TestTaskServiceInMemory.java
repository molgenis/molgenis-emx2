package org.molgenis.emx2.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.tasks.TaskStatus.CANCELLED;
import static org.molgenis.emx2.tasks.TaskStatus.RUNNING;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.MolgenisException;

public class TestTaskServiceInMemory {
  private final TaskServiceInMemory taskService = new TaskServiceInMemory();

  @AfterEach
  void tearDown() {
    taskService.shutdown();
  }

  @Test
  void cancel_shouldCancelRunningTaskAndCallStop() {
    StoppableTask task = new StoppableTask();
    task.setStatus(RUNNING);
    taskService.submit(task);

    Task cancelledTask = taskService.cancel(task.getId());

    assertEquals(task, cancelledTask);
    assertEquals(CANCELLED, task.getStatus());
    assertTrue(task.isStopped());
  }

  @Test
  void cancel_shouldThrowWhenTaskDoesNotExist() {
    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> taskService.cancel("does-not-exist"));

    assertEquals("Task with id 'does-not-exist' not found", exception.getMessage());
  }

  @Test
  void cancel_shouldThrowWhenTaskIsNotRunning() {
    Task task = new Task("completed task");
    task.setStatus(TaskStatus.COMPLETED);
    taskService.submit(task);

    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> taskService.cancel(task.getId()));

    assertEquals("Cannot cancel task with status: COMPLETED", exception.getMessage());
  }

  private static class StoppableTask extends Task {
    private boolean stopped;

    private StoppableTask() {
      super("stoppable task");
    }

    @Override
    public void run() {}

    @Override
    public void stop() {
      this.stopped = true;
    }

    private boolean isStopped() {
      return stopped;
    }
  }
}
