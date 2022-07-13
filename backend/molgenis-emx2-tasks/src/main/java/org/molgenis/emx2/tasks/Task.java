package org.molgenis.emx2.tasks;

import static org.molgenis.emx2.tasks.TaskStatus.COMPLETED;
import static org.molgenis.emx2.tasks.TaskStatus.ERROR;
import static org.molgenis.emx2.tasks.TaskStatus.RUNNING;
import static org.molgenis.emx2.tasks.TaskStatus.SKIPPED;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import org.molgenis.emx2.MolgenisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For documenting processes that consist of multiple steps and elements. For example. batch
 * insert/upload tasks.
 */
public class Task implements Runnable, Iterable<Task> {

  // for the toString method
  private static final ObjectMapper mapper =
      new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
  private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());
  private final List<Task> subTasks = new ArrayList<>();

  private final TaskInfo info;

  public Task(String description) {
    info = TaskInfo.create(description);
  }

  public Task(String description, TaskStatus status) {
    this(description);
    this.info.status = status;
  }

  public Task(String description, boolean strict) {
    this(description);
    this.info.strict = strict;
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

  public String getId() {
    return info.id;
  }

  public TaskInfo getInfo() {
    return info;
  }

  public List<Task> getSubTasks() {
    return Collections.unmodifiableList(subTasks);
  }

  public Integer getProgress() {
    if (RUNNING.equals(info.status)) {
      return info.progress;
    }
    return null;
  }

  public Task setProgress(int progress) {
    this.info.progress = progress;
    return this;
  }

  public String getDescription() {
    String message = info.description;
    switch (info.status) {
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
          if (getTotal() != null && getTotal() > 0) {
            message += " (" + 1000L * getTotal() / getDuration() + " items/sec)";
          }
        }
        return message;
      }
    }
  }

  public Task setDescription(String description) {
    Objects.requireNonNull(description, "description cannot be null");
    info.description = description;
    return this;
  }

  public TaskStatus getStatus() {
    return info.status;
  }

  public Integer getTotal() {
    if (COMPLETED.equals(info.status)) {
      return info.progress;
    }
    return info.total;
  }

  public Task setTotal(int total) {
    info.total = total;
    return this;
  }

  public Task start() {
    info.startTimeMilliseconds = System.currentTimeMillis();
    info.status = RUNNING;
    logger.info(getDescription() + ": started");
    return this;
  }

  public Task complete() {
    info.status = COMPLETED;
    info.endTimeMilliseconds = System.currentTimeMillis();
    logger.info(getDescription());
    return this;
  }

  public Task complete(String description) {
    setDescription(description);
    complete();
    return this;
  }

  public void completeWithError(String message) {
    setDescription(message);
    complete();
    info.status = ERROR;
    logger.error(message);
    throw new MolgenisException(message);
  }

  public long getDuration() {
    if (info.endTimeMilliseconds == 0) {
      return System.currentTimeMillis() - info.startTimeMilliseconds;
    } else {
      return info.endTimeMilliseconds - info.startTimeMilliseconds;
    }
  }

  public Task setStatus(TaskStatus status) {
    Objects.requireNonNull(status, "status can not be null");
    info.status = status;
    return this;
  }

  public void setSkipped(String description) {
    setSkipped();
    setDescription(description);
  }

  public void setSkipped() {
    complete();
    setStatus(SKIPPED);
  }

  public void setError() {
    complete();
    setStatus(ERROR);
  }

  public void setError(String description) {
    setError();
    setDescription(description);
  }

  @Override
  public void run() {}

  @Override
  public Iterator<Task> iterator() {
    return subTasks.iterator();
  }

  @Override
  public void forEach(Consumer<? super Task> action) {
    subTasks.forEach(action);
  }

  @Override
  public Spliterator<Task> spliterator() {
    return subTasks.spliterator();
  }

  public boolean isStrict() {
    return info.strict;
  }

  public String toString() {
    try {
      return mapper.writeValueAsString(this);
    } catch (Exception e) {
      throw new MolgenisException("internal error", e);
    }
  }

  // TODO equals & hashcode
}
