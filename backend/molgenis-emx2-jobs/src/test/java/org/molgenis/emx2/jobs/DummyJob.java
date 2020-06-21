package org.molgenis.emx2.jobs;

import org.molgenis.emx2.MolgenisException;

import java.util.*;

public class DummyJob implements Job {
  private JobStatus status = JobStatus.SCHEDULED;
  private List<JobProgress> completed = new ArrayList<>();
  private JobProgress progress = null;
  private final int total = 100;

  @Override
  public JobStatus getStatus() {
    return status;
  }

  @Override
  public List<JobProgress> getCompleted() {
    return Collections.unmodifiableList(completed);
  }

  @Override
  public List<JobProgress> getProgress() {
    return List.of(progress);
  }

  @Override
  public void run() {
    status = JobStatus.RUNNING;
    for (int task = 1; task <= 5; task++) {
      for (int item = 1; item <= total; item++) {
        try {
          Thread.sleep(5);
        } catch (InterruptedException e) {
          throw new MolgenisException("Error", e);
        }
        progress = new JobProgress("task" + task, item, total);
      }
      completed.add(new JobProgress("task" + task, total, total));
    }
    status = JobStatus.COMPLETED;
  }
}
