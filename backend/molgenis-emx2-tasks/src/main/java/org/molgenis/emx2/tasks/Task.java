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

  protected final TaskInfo info;

  public Task(String description) {
    info = TaskInfo.create(description);
  }

  public Task(String description, TaskStatus status) {
    this(description);
    this.info.setStatus(status);
  }

  public Task(String description, boolean strict) {
    this(description);
    this.info.setStrict(strict);
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

  public TaskInfo getInfo() {
    return info;
  }

  public List<Task> getSubTasks() {
    return Collections.unmodifiableList(subTasks);
  }

  public Task start() {
    info.setStartTimeMilliseconds(System.currentTimeMillis());
    info.setStatus(RUNNING);
    logger.info(info.getDescription() + ": started");
    return this;
  }

  public Task complete() {
    info.setStatus(COMPLETED);
    info.setEndTimeMilliseconds(System.currentTimeMillis());
    logger.info(info.getDescription());
    return this;
  }

  public Task complete(String description) {
    info.setDescription(description);
    complete();
    return this;
  }

  public void completeWithError(String message) {
    info.setDescription(message);
    complete();
    info.setStatus(ERROR);
    logger.error(message);
    throw new MolgenisException(message);
  }

  public void setSkipped(String description) {
    setSkipped();
    info.setDescription(description);
  }

  public void setSkipped() {
    complete();
    info.setStatus(SKIPPED);
  }

  public void setError() {
    complete();
    info.setStatus(ERROR);
  }

  public void setError(String description) {
    setError();
    info.setDescription(description);
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

  public String toString() {
    try {
      return mapper.writeValueAsString(this);
    } catch (Exception e) {
      throw new MolgenisException("internal error", e);
    }
  }

  // TODO equals & hashcode
}
