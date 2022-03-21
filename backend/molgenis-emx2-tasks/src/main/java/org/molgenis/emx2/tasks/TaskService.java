package org.molgenis.emx2.tasks;

import java.util.Set;

public interface TaskService {
  String submit(Task task);

  Set<String> getJobIds();

  Task getTask(String id);

  void removeOlderThan(long milliseconds);

  void shutdown();

  void removeTask(String id);

  void clear();
}
