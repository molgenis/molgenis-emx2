package org.molgenis.emx2.tasks;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import java.util.UUID;
import org.molgenis.emx2.Row;

public class TaskInfo {

  public static final String TABLE_NAME = "taskInfo";

  public static final String ID = "id";
  public static final String DESCRIPTION = "description";
  public static final String STATUS = "status";
  public static final String TOTAL = "total";
  public static final String PROGRESS = "progress";
  public static final String START_TIME_MILLISECONDS = "startTimeMilliseconds";
  public static final String END_TIME_MILLISECONDS = "endTimeMilliseconds";
  public static final String LOG = "log";
  public static final String STRICT = "strict";

  public String id;
  public String description;
  public TaskStatus status = TaskStatus.WAITING;
  public Integer total;
  public Integer progress = 0;
  public long startTimeMilliseconds;
  public long endTimeMilliseconds;
  // this parameter is used to indicate if steps should fail on unexpected state or should simply
  // try to complete
  public boolean strict;

  // TODO task type
  // TODO log

  public TaskInfo() {}

  private TaskInfo(String id, String description) {
    this.id = id;
    this.description = description;

    requireNonNull(id, "id can't be null");
    requireNonNull(description, "description can't be null");
  }

  public static TaskInfo create(String description) {
    return new TaskInfo(UUID.randomUUID().toString(), description);
  }

  public boolean isFinished() {
    return status != TaskStatus.RUNNING && status != TaskStatus.WAITING;
  }

  //TODO equals & hashcode
}
