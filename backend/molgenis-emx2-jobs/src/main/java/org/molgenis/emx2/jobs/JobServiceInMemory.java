package org.molgenis.emx2.jobs;

import org.molgenis.emx2.MolgenisException;

import java.util.*;
import java.util.concurrent.*;

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
  public Set<String> getJobs() {
    return jobs.keySet();
  }

  @Override
  public JobStatus getStatus(String jobId) {
    check(jobId);
    return jobs.get(jobId).getStatus();
  }

  @Override
  public List<JobProgress> getProgress(String jobId) {
    check(jobId);
    return jobs.get(jobId).getProgress();
  }

  @Override
  public List<JobProgress> getCompleted(String jobId) {
    check(jobId);
    return jobs.get(jobId).getCompleted();
  }

  @Override
  public void purge() {
    Set<String> keys = jobs.keySet();
    for (String key : keys) {
      if (JobStatus.COMPLETED.equals(jobs.get(key).getStatus())) {
        jobs.remove(key);
      }
    }
  }

  @Override
  public void shutdown() {
    // todo, kill all jobs first
    executorService.shutdown();
  }

  private void check(String jobId) {
    if (jobs.get(jobId) == null)
      throw new MolgenisException("Job not found", "Job with id " + jobId + " doesn't exist");
  }
}
