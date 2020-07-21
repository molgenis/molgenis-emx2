package org.molgenis.emx2.jobs;

import java.time.Duration;
import java.time.LocalDateTime;

/** Immutable */
public class JobTask {
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private String label;
  private int count = 0;
  private int total = 0;
  private JobStatus status = JobStatus.SCHEDULED;

  JobTask(String label) {
    this.label = label;
  }

  JobTask(String label, int total) {
    this.label = label;
    this.total = total;
  }

  public String getId() {
    return label;
  }

  void start() {
    this.startTime = LocalDateTime.now();
    this.status = JobStatus.RUNNING;
  }

  void updateCount(int count) {
    this.count = count;
  }

  public void complete() {
    this.endTime = LocalDateTime.now();
    this.status = JobStatus.COMPLETED;
  }

  public void complete(int count) {
    this.complete();
    this.updateCount(count);
    this.total = count;
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

  public int getTotal() {
    return total;
  }

  public String getLabel() {
    return label;
  }

  public int getCount() {
    return count;
  }

  public Duration getRunTime() {
    if (getEndTime() == null) {
      return Duration.between(getStartTime(), LocalDateTime.now());
    } else {
      return Duration.between(getStartTime(), getEndTime());
    }
  }

  public String getDurationString() {
    if (getStartTime() != null) {
      return getRunTime()
          .toString()
          .substring(2)
          .replaceAll("(\\d[HMS])(?!$)", "$1 ")
          .toLowerCase();
    } else {
      return "";
    }
  }

  public String toString() {
    if (getStartTime() != null) {
      if (getTotal() > 0 && getEndTime() == null) {
        return getLabel()
            + " "
            + getStatus()
            + ": "
            + (100 * getCount() / getTotal())
            + "% in "
            + getDurationString();
      } else {
        return getLabel()
            + " "
            + getStatus()
            + ": "
            + getCount()
            + " items in "
            + getDurationString();
      }
    } else {
      return getLabel() + " " + getStatus();
    }
  }
}
