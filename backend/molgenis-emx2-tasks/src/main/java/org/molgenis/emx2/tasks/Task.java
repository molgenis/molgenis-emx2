package org.molgenis.emx2.tasks;

import static org.molgenis.emx2.tasks.TaskStatus.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import org.molgenis.emx2.MolgenisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For documenting processes that consist of multiple steps and elements. For example. batch
 * insert/upload tasks.
 */
public class Task<T extends Task> implements Runnable, Iterable<Task> {
  // some unique id
  private final String id = UUID.randomUUID().toString();
  // for the toString method
  @JsonIgnore
  private static final ObjectMapper mapper =
      new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

  @JsonIgnore private final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());
  // human readable description
  private String description;
  // status of the tas
  private TaskStatus status = WAITING;
  // total size of the task, used for progress monitoring
  private Integer total;
  // position in the total size, if available, used for progress monitoring
  private Integer progress;
  // user who submitted
  private String submitUser;
  // start time to measure run time
  private long submitTimeMilliseconds = System.currentTimeMillis();
  // start time to measure run time
  private long startTimeMilliseconds;
  // end time to calculate run time
  private long endTimeMilliseconds;
  // subtasks/steps in this task
  private List<Task<?>> subTasks = new ArrayList<>();
  // parent task
  @JsonIgnore private Task parentTask;
  // this parameter is used to indicate if steps should fail on unexpected state or should simply
  // try to complete
  @JsonIgnore private boolean strict = false;
  // this handler is used to notify that relevant things happened
  @JsonIgnore private TaskChangedHandler changedHandler;
  private String cronExpression;
  private boolean disabled = false;

  public Task() {}

  public Task(String description) {
    Objects.requireNonNull(description, "description cannot be null");
    this.description = description;
  }

  public Task(String description, boolean strict) {
    this(description);
    this.strict = strict;
  }

  public Task addSubTask(String message) {
    Task step = new Task(message);
    step.setParentTask(this);
    this.subTasks.add(step);
    return step;
  }

  public Task addSubTask(String message, TaskStatus status) {
    Task step = new Task(message);
    this.subTasks.add(step);
    step.setStatus(status);
    return step;
  }

  public void addSubTask(Task task) {
    Objects.requireNonNull(task, "task cannot be null");
    this.subTasks.add(task);
  }

  private void setParentTask(Task parentTask) {
    this.parentTask = parentTask;
  }

  public List<Task<?>> getSubTasks() {
    return Collections.unmodifiableList(subTasks);
  }

  public Integer getProgress() {
    if (RUNNING.equals(status)) return progress;
    return null;
  }

  public T setProgress(int progress) {
    this.progress = progress;
    return (T) this;
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

  public T setDescription(String description) {
    Objects.requireNonNull(description, "description cannot be null");
    this.description = description;
    return (T) this;
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

  public T setTotal(int total) {
    this.total = total;
    return (T) this;
  }

  public T start() {
    this.setStatus(RUNNING);
    this.logger.info(getDescription() + ": started");
    return (T) this;
  }

  public T complete() {
    this.setStatus(COMPLETED);
    this.logger.info(getDescription());
    return (T) this;
  }

  public T complete(String description) {
    this.description = description;
    setStatus(COMPLETED);
    return (T) this;
  }

  public void completeWithError(String message) {
    this.description = message;
    this.setStatus(ERROR);
    logger.error(message);
    throw new MolgenisException(message);
  }

  @JsonIgnore
  public long getDuration() {
    if (startTimeMilliseconds == 0) {
      return 0;
    } else if (endTimeMilliseconds == 0) {
      return System.currentTimeMillis() - startTimeMilliseconds;
    } else {
      return endTimeMilliseconds - startTimeMilliseconds;
    }
  }

  public T setStatus(TaskStatus status) {
    Objects.requireNonNull(status, "status can not be null");
    if (RUNNING.equals(status)) {
      this.startTimeMilliseconds = System.currentTimeMillis();
    } else if (ERROR.equals(status) || COMPLETED.equals(status)) {
      if (startTimeMilliseconds == 0) {
        this.startTimeMilliseconds = System.currentTimeMillis();
      }
      this.endTimeMilliseconds = System.currentTimeMillis();
    }
    this.status = status;
    this.handleChange();
    return (T) this;
  }

  public void setSkipped(String description) {
    this.description = description;
    this.setStatus(SKIPPED);
  }

  public void setSkipped() {
    this.setStatus(SKIPPED);
  }

  public void setError() {
    this.setStatus(ERROR);
  }

  public void setError(String description) {
    this.description = description;
    this.setStatus(ERROR);
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
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
    } catch (Exception e) {
      throw new MolgenisException("internal error", e);
    }
  }

  public String getId() {
    return id;
  }

  public void setChangedHandler(TaskChangedHandler changedHandler) {
    this.changedHandler = changedHandler;
  }

  public long getSubmitTimeMilliseconds() {
    return submitTimeMilliseconds;
  }

  public long getStartTimeMilliseconds() {
    return startTimeMilliseconds;
  }

  public String getSubmitUser() {
    return submitUser;
  }

  public T submitUser(String submitUser) {
    this.submitUser = submitUser;
    return (T) this;
  }

  public T cronExpression(String cronExpression) {
    this.cronExpression = cronExpression;
    return (T) this;
  }

  public String getCronExpression() {
    return this.cronExpression;
  }

  public T disabled(boolean disabled) {
    this.disabled = disabled;
    return (T) this;
  }

  public boolean isDisabled() {
    return this.disabled;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Task)) return false;
    Task task = (Task) o;
    return submitTimeMilliseconds == task.submitTimeMilliseconds
        && startTimeMilliseconds == task.startTimeMilliseconds
        && endTimeMilliseconds == task.endTimeMilliseconds
        && strict == task.strict
        && id.equals(task.id)
        && Objects.equals(description, task.description)
        && status == task.status
        && Objects.equals(total, task.total)
        && Objects.equals(progress, task.progress)
        && Objects.equals(subTasks, task.subTasks);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id,
        description,
        status,
        total,
        progress,
        submitTimeMilliseconds,
        startTimeMilliseconds,
        endTimeMilliseconds,
        subTasks,
        strict);
  }

  public void handleChange() {
    // currently we only log at setStatus changes to not overload database
    if (this.parentTask != null) {
      this.parentTask.handleChange();
    } else if (this.changedHandler != null) {
      this.changedHandler.handleChange(this);
    }
  }

  public void handleOutput(File outputFile) {
    if (this.changedHandler != null) {
      this.changedHandler.handleOutputFile(this, outputFile);
    }
  }

  public void stop() {
    // will stop if implemented
  }

  public long getEndTimeMilliseconds() {
    return this.endTimeMilliseconds;
  }

  @JsonIgnore
  public boolean isRunning() {
    return !status.equals(ERROR) && !status.equals(COMPLETED);
  }
}
