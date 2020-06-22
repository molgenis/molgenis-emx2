package org.molgenis.emx2.jobs;

import org.molgenis.emx2.MolgenisException;

public class DummyJob extends Job {
  private final int noItems = 100;
  private final int noTasks = 5;

  public DummyJob() {
    for (int i = 1; i <= noTasks; i++) {
      super.addTask(new JobTask("task" + i, noItems));
    }
  }

  @Override
  public void run() {
    start();
    System.out.println("started");
    for (int task = 1; task <= noTasks; task++) {
      getTask("task" + task).start();
      for (int item = 1; item <= noItems; item++) {
        try {
          Thread.sleep(5);
        } catch (InterruptedException e) {
          throw new MolgenisException("Error", e);
        }
        getTask("task" + task).updateCount(item);
      }
      getTask("task" + task).complete();
    }
    complete();
  }
}
