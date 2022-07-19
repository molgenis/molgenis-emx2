package org.molgenis.emx2.tasks;

import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.SelectColumn.s;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Operator;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.sql.SqlDatabase;

public class TaskServicePersisted implements TaskService {

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private final ThreadPoolExecutor taskExecutor =
      new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
  private final Map<String, TaskInfo> activeTasks = new ConcurrentHashMap<>();
  private final Table tasksTable;

  public TaskServicePersisted() {
    var database = new SqlDatabase(false);
    database.setActiveUser("admin");
    tasksTable = database.getSchema("tasks").getTable(TaskInfo.TABLE_NAME);

    scheduler.scheduleAtFixedRate(this::backupActiveTasks, 5, 5, TimeUnit.SECONDS);
  }

  @Override
  public String submit(Task task) {
    tasksTable.insert(task.getInfo().toRow());
    activeTasks.put(task.getInfo().id, task.getInfo());
    taskExecutor.submit(task);
    return task.getId();
  }

  @Override
  public Set<String> getTaskIds() {
    return tasksTable.select(s(TaskInfo.ID)).limit(100).retrieveRows().stream()
        .map(row -> row.get(TaskInfo.ID, String.class))
        .collect(Collectors.toSet());
  }

  @Override
  public TaskInfo getTaskInfo(String id) {
    var row = tasksTable.where(f(TaskInfo.ID, Operator.EQUALS, id)).limit(1).retrieveRows().get(0);
    return TaskInfo.fromRow(row);
  }

  @Override
  public Collection<TaskInfo> listTaskInfos() {
    return tasksTable.retrieveRows().stream().map(TaskInfo::fromRow).toList();
  }

  @Override
  public void removeOlderThan(long milliseconds) {
    var rowsToDelete =
        tasksTable.retrieveRows().stream()
            .map(TaskInfo::fromRow)
            .filter(TaskInfo::isFinished)
            .filter(taskInfo -> taskInfo.isOlderThan(milliseconds))
            .map(TaskInfo::toRow)
            .toList();

    tasksTable.delete(rowsToDelete);
  }

  @Override
  public void shutdown() {
    // todo, kill all jobs first
    scheduler.shutdown();
    taskExecutor.shutdown();
  }

  @Override
  public void removeTask(String id) {
    if (activeTasks.containsKey(id)) {
      throw new MolgenisException("Can't remove active tasks");
    }

    tasksTable.delete(new Row(TaskInfo.ID, id));
  }

  @Override
  public void clear() {
    tasksTable.truncate();
  }

  private void backupActiveTasks() {
    if (activeTasks.isEmpty()) {
      return;
    }

    var taskInfos = activeTasks.values().stream().map(TaskInfo::toRow).toList();
    tasksTable.update(taskInfos);
    activeTasks.values().removeIf(TaskInfo::isFinished);
  }
}
