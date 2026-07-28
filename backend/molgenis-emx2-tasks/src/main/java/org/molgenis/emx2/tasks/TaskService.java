package org.molgenis.emx2.tasks;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface TaskService {
  List<ScriptTask> getScripts();

  String submit(Task task);

  Task cancel(String taskId);

  ScriptTask getScript(String name);

  Set<String> getJobIds();

  Task getTask(String id);

  Collection<Task> listTasks();

  void removeOlderThan(long milliseconds);

  void shutdown();

  void removeTask(String id);

  void clear();
}
