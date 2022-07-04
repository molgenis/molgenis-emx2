package org.molgenis.emx2.tasks;

import java.util.Collection;
import java.util.Set;

public interface TaskService {
  String submit(Task task);

  Set<String> getTaskIds();

  Task getTask(String id);

  Collection<Task> listTasks();

  void removeOlderThan(long milliseconds);

  void shutdown();

  void removeTask(String id);

  void clear();

  void updateTask(Task task);
}
