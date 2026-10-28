package org.molgenis.emx2.tasks;

import org.molgenis.emx2.MolgenisException;

public class DummyTask extends Task {
  private static final int NO_ITEMS = 100;
  private static final int NO_TASKS = 5;

  public DummyTask() {
    super("dummy", false);
    for (int i = 1; i <= NO_TASKS; i++) {
      super.addSubTask("task" + i);
    }
  }

  @Override
  public void run() {
    start();
    System.out.println("started");
    for (Task t : this) {
      t.start();
      for (int item = 1; item <= NO_ITEMS; item++) {
        try {
          Thread.sleep(5);
        } catch (InterruptedException e) {
          throw new MolgenisException("Error", e);
        }
        t.setProgress(item);
      }
      t.complete();
    }
    complete();
  }
}
