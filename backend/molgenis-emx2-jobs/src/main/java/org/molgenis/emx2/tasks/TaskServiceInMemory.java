package org.molgenis.emx2.tasks;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TaskServiceInMemory implements TaskService {
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
    return tasks.get(id);
  }

  @Override
  public void removeOlderThan(long milliseconds) {
    Set<String> keys = tasks.keySet();
    for (String key : keys) {
      if (tasks.get(key).end != 0
          && tasks.get(key).end < System.currentTimeMillis() - milliseconds) {
        tasks.remove(key);
      }
    }
  }

  @Override
  public void shutdown() {
    // todo, kill all jobs first
    executorService.shutdown();
  }
}
