package org.molgenis.emx2.jobs;

import java.time.LocalDateTime;
import java.util.*;

public abstract class Job implements Runnable {
  private Map<String, JobTask> tasks = new LinkedHashMap<>();
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private JobStatus status;

  public Collection<JobTask> getTasks() {
    return Collections.unmodifiableCollection(tasks.values());
  }

  public void addTask(JobTask task) {
    tasks.put(task.getId(), task);
  }

  public JobTask getTask(String taskId) {
    return tasks.get(taskId);
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public LocalDateTime getEndTime() {
    return endTime;
  }

  public JobStatus getStatus() {
    return status;
  }

  protected void start() {
    this.startTime = LocalDateTime.now();
  }

  protected void complete() {
    this.endTime = LocalDateTime.now();
    this.status = JobStatus.COMPLETED;
  }
}
