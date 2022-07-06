package org.molgenis.emx2.tasks;

import java.util.Collection;
import java.util.Set;

public interface TaskService {
  String submit(Task task);

  Set<String> getTaskIds();

  TaskInfo getTaskInfo(String id);

  Collection<TaskInfo> listTaskInfos();

  void removeOlderThan(long milliseconds);

  void shutdown();

  void removeTask(String id);

  void clear();

  void updateTaskInfo(TaskInfo taskInfo);
}
