package org.molgenis.emx2.tasks;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.jooq.DSLContext;
import org.molgenis.emx2.sql.SqlDatabase;

public class TaskServicePersisted implements TaskService {

  private final ThreadPoolExecutor executorService;
  private final DSLContext jooq;

  public TaskServicePersisted(String taskSchema) {
    executorService =
        new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    var database = new SqlDatabase(false);
    database.setActiveUser("admin");
    jooq = database.getJooq();

    // TODO add tasks that haven't started yet to pool
  }

  @Override
  public String submit(Task task) {
    var taskInfo = TaskInfo.create(task.getDescription());
    jooq.insertInto(TaskInfo.TABLE, TaskInfo.ID, TaskInfo.DESCRIPTION, TaskInfo.STATUS)
        .values(taskInfo.id, taskInfo.description, taskInfo.status.toString())
        .execute();
    executorService.submit(task);
    return task.getId();
  }

  @Override
  public Set<String> getTaskIds() {
    return jooq.select(TaskInfo.ID).from(TaskInfo.TABLE).limit(100).stream()
        .map(record -> record.getValue(TaskInfo.ID))
        .map(Object::toString)
        .collect(Collectors.toSet());
  }

  @Override
  public TaskInfo getTaskInfo(String id) {
    return jooq.selectFrom(TaskInfo.TABLE).where("id = ?", id).fetchOneInto(TaskInfo.class);
  }

  @Override
  public Collection<TaskInfo> listTaskInfos() {
    return jooq.selectFrom(TaskInfo.TABLE).fetchInto(TaskInfo.class);
  }

  @Override
  public void removeOlderThan(long milliseconds) {}

  @Override
  public void shutdown() {
    // todo, kill all jobs first
    executorService.shutdown();
  }

  @Override
  public void removeTask(String id) {
    jooq.deleteFrom(TaskInfo.TABLE).where("id = ?", id).execute();
  }

  @Override
  public void clear() {
    jooq.truncateTable(TaskInfo.TABLE).execute();
  }

  @Override
  public void updateTaskInfo(TaskInfo task) {}
}
