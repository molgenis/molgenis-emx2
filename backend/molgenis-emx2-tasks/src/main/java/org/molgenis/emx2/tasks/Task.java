package org.molgenis.emx2.tasks;

import static org.molgenis.emx2.tasks.TaskStatus.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.function.Consumer;
import org.molgenis.emx2.MolgenisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For documenting processes that consist of multiple steps and elements. For example. batch
 * insert/upload tasks.
 */
public class Task implements Runnable, Iterable<Task> {
  // some unique id
  private String id = UUID.randomUUID().toString();
  // for the toString method
  private static ObjectMapper mapper =
      new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
  private Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());
  // human readable description
  private String description;
  // status of the tas
  private TaskStatus status = WAITING;
  // total size of the task, used for progress monitoring
  private Integer total;
  // position in the total size, if available, used for progress monitoring
  private Integer progress;
  // start time to measure run time
  private long startTimeMilliseconds = System.currentTimeMillis();
  // end time to calculate run time
  long endTimeMilliseconds;
  // subtasks/steps in this task
  private List<Task> subTasks = new ArrayList<>();
  // this parameter is used to indicate if steps should fail on unexpected state or should simply
  // try to complete
  private boolean strict = false;

  public Task(String description) {
    Objects.requireNonNull(description, "description cannot be null");
    this.description = description;
  }

  public Task(String description, TaskStatus status) {
    this(description);
    Objects.requireNonNull(status, "status cannot be null");
    this.status = status;
  }

  public Task(String description, boolean strict) {
    this(description);
    this.strict = strict;
  }

  public Task addSubTask(String message) {
    Task step = new Task(message);
    this.subTasks.add(step);
    return step;
  }

  public Task addSubTask(String message, TaskStatus status) {
    Task step = new Task(message, status);
    this.subTasks.add(step);
    return step;
  }

  public void addSubTask(Task task) {
    Objects.requireNonNull(task, "task cannot be null");
    this.subTasks.add(task);
  }

  public List<Task> getSubTasks() {
    return subTasks;
  }

  public Integer getProgress() {
    if (RUNNING.equals(status)) return progress;
    return null;
  }

  public Task setProgress(int progress) {
    this.progress = progress;
    return this;
  }

  public String getDescription() {
    String message = description;
    switch (status) {
      case WAITING:
        if (getDuration() > 0) {
          message += " for " + getDuration() + "ms";
        }
        return message;
      case RUNNING:
        if (getDuration() > 0) {
          message += " for " + getDuration() + "ms";
          if (getProgress() != null && getProgress() > 0) {
            message += " at " + 1000 * getProgress() / getDuration() + " items/sec";
          }
        }
        return message;
      default:
        if (getDuration() > 0) {
          message += " in " + getDuration() + "ms";
          if (getTotal() != null && getTotal() > 0) {
            message += " (" + 1000 * getTotal() / getDuration() + " items/sec)";
          }
        }
        return message;
    }
  }

  public Task setDescription(String description) {
    Objects.requireNonNull(description, "description cannot be null");
    this.description = description;
    return this;
  }

  public TaskStatus getStatus() {
    return status;
  }

  public Integer getTotal() {
    if (COMPLETED.equals(status)) {
      return progress;
    }
    return total;
  }

  public Task setTotal(int total) {
    this.total = total;
    return this;
  }

  public Task start() {
    this.startTimeMilliseconds = System.currentTimeMillis();
    this.status = RUNNING;
    this.logger.info(getDescription() + ": started");
    return this;
  }

  public Task complete() {
    this.status = COMPLETED;
    this.endTimeMilliseconds = System.currentTimeMillis();
    this.logger.info(getDescription());
    return this;
  }

  public Task complete(String description) {
    this.setDescription(description);
    complete();
    return this;
  }

  public void completeWithError(String message) {
    this.setDescription(message);
    this.complete();
    this.status = ERROR;
    logger.error(message);
    throw new MolgenisException(message);
  }

  public long getDuration() {
    if (endTimeMilliseconds == 0) {
      return System.currentTimeMillis() - startTimeMilliseconds;
    } else {
      return endTimeMilliseconds - startTimeMilliseconds;
    }
  }

  public Task setStatus(TaskStatus status) {
    Objects.requireNonNull(status, "status can not be null");
    this.status = status;
    return this;
  }

  public void skipped(String description) {
    this.skipped();
    this.setDescription(description);
  }

  public void skipped() {
    this.complete();
    this.setStatus(SKIPPED);
  }

  public void error() {
    this.complete();
    this.setStatus(ERROR);
  }

  public void error(String description) {
    this.error();
    this.setDescription(description);
  }

  @Override
  public void run() {}

  @Override
  public Iterator iterator() {
    return this.subTasks.iterator();
  }

  @Override
  public void forEach(Consumer action) {
    this.subTasks.forEach(action);
  }

  @Override
  public Spliterator spliterator() {
    return this.subTasks.spliterator();
  }

  public boolean isStrict() {
    return strict;
  }

  public String toString() {
    try {
      return mapper.writeValueAsString(this);
    } catch (Exception e) {
      // should never happen
      throw new MolgenisException("internal error", e);
    }
  }

  public String getId() {
    return id;
  }
}
