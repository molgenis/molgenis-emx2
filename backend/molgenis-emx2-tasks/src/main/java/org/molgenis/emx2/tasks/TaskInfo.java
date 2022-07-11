package org.molgenis.emx2.tasks;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import java.util.UUID;
import javax.persistence.Column;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;

public class TaskInfo {

  public static final Table<Record> TABLE = table(name("tasks", "taskInfo"));

  public static final Field<String> ID = field("id", String.class);
  public static final Field<String> DESCRIPTION = field("description", String.class);
  public static final Field<String> STATUS = field("status", String.class);

  @Column(name = "id", nullable = false)
  public String id;

  @Column(name = "description", nullable = false)
  public String description;

  @Column(name = "status", nullable = false)
  public TaskStatus status = TaskStatus.WAITING;

  @Column(name = "total")
  public Integer total;

  @Column(name = "progress")
  public Integer progress = 0;

  @Column(name = "startTimeMilliseconds")
  public long startTimeMilliseconds;

  @Column(name = "endTimeMilliseconds")
  public long endTimeMilliseconds;

  @Column(name = "strict")
  public boolean strict;

  public TaskInfo() {}

  private TaskInfo(String id, String description) {
    this.id = id;
    this.description = description;
  }

  public static TaskInfo create(String description) {
    return new TaskInfo(UUID.randomUUID().toString(), description);
  }
}
