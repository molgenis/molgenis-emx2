package org.molgenis.emx2.tasks;

import org.molgenis.emx2.MolgenisException;

public class DummyTask extends Task {
  private final int noItems = 100;
  private final int noTasks = 5;

  public DummyTask() {
    super("dummy", false);
    for (int i = 1; i <= noTasks; i++) {
      super.add(new Task("task" + i, false));
    }
  }

  @Override
  public void run() {
    start();
    System.out.println("started");
    for (Task t : this) {
      t.start();
      for (int item = 1; item <= noItems; item++) {
        try {
          Thread.sleep(5);
        } catch (InterruptedException e) {
          throw new MolgenisException("Error", e);
        }
        t.setIndex(item);
      }
      t.complete();
    }
    complete();
  }
}
