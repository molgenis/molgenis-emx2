package org.molgenis.emx2.tasks;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.molgenis.emx2.tasks.TaskStatus.RUNNING;

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

  private String id;
  private String description;
  private TaskStatus status = TaskStatus.WAITING;
  private Integer total;
  private int progress;
  private long startTimeMilliseconds;
  private long endTimeMilliseconds;
  // this parameter is used to indicate if steps should fail on unexpected state or should simply
  // try to complete
  private boolean strict;

  // TODO task type
  // TODO log

  public TaskInfo() {}

  private TaskInfo(String id, String description) {
    this.id = id;
    this.description = description;

    requireNonNull(id, "id can't be null");
    requireNonNull(description, "description can't be null");
  }

  public String getDescription() {
    String message = description;
    switch (status) {
      case WAITING -> {
        if (getDuration() > 0) {
          message += " for " + getDuration() + "ms";
        }
        return message;
      }
      case RUNNING -> {
        if (getDuration() > 0) {
          message += " for " + getDuration() + "ms";
          if (getProgress() != null && getProgress() > 0) {
            message += " at " + 1000L * getProgress() / getDuration() + " items/sec";
          }
        }
        return message;
      }
      default -> {
        if (getDuration() > 0) {
          message += " in " + getDuration() + "ms";
          if (total != null && total > 0) {
            message += " (" + 1000L * total / getDuration() + " items/sec)";
          }
        }
        return message;
      }
    }
  }

  public long getDuration() {
    if (endTimeMilliseconds == 0) {
      return System.currentTimeMillis() - startTimeMilliseconds;
    } else {
      return endTimeMilliseconds - startTimeMilliseconds;
    }
  }

  public Integer getProgress() {
    return progress;
  }

  public String getId() {
    return id;
  }

  public TaskStatus getStatus() {
    return status;
  }

  public Integer getTotal() {
    return total;
  }

  public long getStartTimeMilliseconds() {
    return startTimeMilliseconds;
  }

  public long getEndTimeMilliseconds() {
    return endTimeMilliseconds;
  }

  public boolean isStrict() {
    return strict;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setStatus(TaskStatus status) {
    this.status = status;
  }

  public void setTotal(Integer total) {
    this.total = total;
  }

  public void setProgress(int progress) {
    this.progress = progress;
  }

  public void setStartTimeMilliseconds(long startTimeMilliseconds) {
    this.startTimeMilliseconds = startTimeMilliseconds;
  }

  public void setEndTimeMilliseconds(long endTimeMilliseconds) {
    this.endTimeMilliseconds = endTimeMilliseconds;
  }

  public void setStrict(boolean strict) {
    this.strict = strict;
  }

  public static TaskInfo create(String description) {
    return new TaskInfo(UUID.randomUUID().toString(), description);
  }

  public boolean isFinished() {
    return status != TaskStatus.RUNNING && status != TaskStatus.WAITING;
  }

  public boolean isOlderThan(long milliseconds) {
    return endTimeMilliseconds <= System.currentTimeMillis() - milliseconds;
  }

  // TODO equals & hashcode

  public Row toRow() {
    return new Row(
        ID, id,
        DESCRIPTION, description,
        STATUS, status.toString(),
        TOTAL, total,
        PROGRESS, progress,
        START_TIME_MILLISECONDS, startTimeMilliseconds,
        END_TIME_MILLISECONDS, endTimeMilliseconds,
        STRICT, strict);
  }

  public static TaskInfo fromRow(Row row) {
    var taskInfo = new TaskInfo(row.getString(ID), row.getString(DESCRIPTION));
    taskInfo.status = TaskStatus.valueOf(row.getString(STATUS));
    taskInfo.total = row.getInteger(TOTAL);
    taskInfo.progress = row.getInteger(PROGRESS);
    taskInfo.startTimeMilliseconds = ofNullable(row.getLong(START_TIME_MILLISECONDS)).orElse(0L);
    taskInfo.endTimeMilliseconds = ofNullable(row.getLong(END_TIME_MILLISECONDS)).orElse(0L);
    taskInfo.strict = row.getBoolean(STRICT);
    return taskInfo;
  }
}
