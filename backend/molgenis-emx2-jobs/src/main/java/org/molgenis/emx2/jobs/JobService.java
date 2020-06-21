package org.molgenis.emx2.jobs;

import java.util.List;
import java.util.Set;

public interface JobService {
  String add(Job job);

  Set<String> getJobs();

  JobStatus getStatus(String id);

  List<JobProgress> getProgress(String jobId);

  List<JobProgress> getCompleted(String jobId);

  void purge();

  void shutdown();
}
