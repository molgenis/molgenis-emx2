package org.molgenis.emx2.jobs;

import java.time.LocalDateTime;
import java.util.Set;

public interface JobService {
  String add(Job job);

  Set<String> getJobIds();

  Job getJob(String id);

  void removeBeforeTime(LocalDateTime time);

  void shutdown();
}
