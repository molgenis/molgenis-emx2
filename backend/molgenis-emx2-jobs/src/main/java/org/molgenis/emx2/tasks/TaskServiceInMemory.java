package org.molgenis.emx2.tasks;

import static org.molgenis.emx2.tasks.StepStatus.RUNNING;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.molgenis.emx2.MolgenisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskServiceInMemory implements TaskService {
  Logger logger = LoggerFactory.getLogger(TaskServiceInMemory.class.getSimpleName());
  private ExecutorService executorService;
  private Map<String, Task> tasks = new LinkedHashMap<>();

  public TaskServiceInMemory() {
    executorService =
        new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
  }

  @Override
  public String submit(Task task) {
    String id = UUID.randomUUID().toString();
    tasks.put(id, task);
    executorService.submit(task);
    return id;
  }

  @Override
  public Set<String> getJobIds() {
    return tasks.keySet();
  }

  @Override
  public Task getTask(String id) {
    // delete older than a day
    removeOlderThan(24L * 60 * 60 * 1000);
    return tasks.get(id);
  }

  @Override
  public void removeOlderThan(long milliseconds) {
    Set<String> keys = tasks.keySet();
    for (String key : keys) {
      if (tasks.get(key).end != 0
          && tasks.get(key).end <= System.currentTimeMillis() - milliseconds) {
        try {
          tasks.remove(key);
        } catch (Exception e) {
          // no problem, we only delete what we can
        }
      }
    }
  }

  @Override
  public void shutdown() {
    // todo, kill all jobs first
    executorService.shutdown();
  }

  @Override
  public void removeTask(String id) {
    if (id == null) return;
    id = id.replaceAll("[\n|\r|\t]", "_"); // sanitize

    Task task = getTask(id);

    if (task == null) {
      logger.info("skipped delete task " + id + "because not found");
      throw new MolgenisException("Task with id '" + id + "' not found");
    }
    if (task.getStatus().equals(StepStatus.RUNNING)) {
      logger.info("skipped delete task " + id + "because still running");
      throw new MolgenisException("Cannot yet cancel running tasks");
    }
    logger.info("deleted task " + id);

    this.tasks.remove(id);
  }

  @Override
  public void clear() {
    for (String id : getJobIds()) {
      if (!getTask(id).getStatus().equals(RUNNING)) {
        removeTask(id);
      }
    }
  }
}
