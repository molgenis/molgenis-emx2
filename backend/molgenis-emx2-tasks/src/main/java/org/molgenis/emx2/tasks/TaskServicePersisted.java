package org.molgenis.emx2.tasks;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.molgenis.emx2.sql.SqlDatabase;

public class TaskServicePersisted implements TaskService {

  private final ThreadPoolExecutor executorService;
  private final DSLContext jooq;
  private final org.jooq.Table<Record> tasksTable = table(name("tasks", "taskInfo"));

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
    executorService.submit(task);

    return task.getId();
  }

  @Override
  public Set<String> getTaskIds() {
    return jooq.select(field("id"))
        .from(tasksTable)
        .limit(100)
        .stream()
        .map(record -> record.getValue("id"))
        .map(Object::toString)
        .collect(Collectors.toSet());
  }

  @Override
  public TaskInfo getTaskInfo(String id) {
    return jooq.selectFrom(tasksTable).where("id = ?", id).fetchOneInto(TaskInfo.class);
  }

  @Override
  public Collection<TaskInfo> listTaskInfos() {
    return jooq.selectFrom(tasksTable).fetchInto(TaskInfo.class);
  }

  @Override
  public void removeOlderThan(long milliseconds) {
  }

  @Override
  public void shutdown() {
    // todo, kill all jobs first
    executorService.shutdown();
  }

  @Override
  public void removeTask(String id) {
    jooq.deleteFrom(tasksTable).where("id = ?", id).execute();
  }

  @Override
  public void clear() {
    jooq.truncateTable(tasksTable).execute();
  }

  @Override
  public void updateTaskInfo(TaskInfo task) {
  }
}
