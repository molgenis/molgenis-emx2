package org.molgenis.emx2.jobs;

import java.util.List;

public interface Job extends Runnable {
  List<JobProgress> getCompleted();

  List<JobProgress> getProgress();

  JobStatus getStatus();
}
