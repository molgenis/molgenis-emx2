package org.molgenis.emx2.jobs;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class JobServiceInMemory implements JobService {
  private ExecutorService executorService;
  private Map<String, Job> jobs = new LinkedHashMap<>();

  public JobServiceInMemory() {
    executorService =
        new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
  }

  @Override
  public String add(Job job) {
    String id = UUID.randomUUID().toString();
    jobs.put(id, job);
    executorService.submit(job);
    return id;
  }

  @Override
  public Set<String> getJobIds() {
    return jobs.keySet();
  }

  @Override
  public Job getJob(String id) {
    return jobs.get(id);
  }

  @Override
  public void removeBeforeTime(LocalDateTime before) {
    Set<String> keys = jobs.keySet();
    for (String key : keys) {
      if (jobs.get(key).getEndTime() != null && jobs.get(key).getEndTime().isBefore(before)) {
        jobs.remove(key);
      }
    }
  }

  @Override
  public void shutdown() {
    // todo, kill all jobs first
    executorService.shutdown();
  }
}
