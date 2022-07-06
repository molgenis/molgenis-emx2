package org.molgenis.emx2.tasks;

import javax.persistence.Column;

public class TaskInfo {

  @Column(name = "id", nullable = false)
  public String id;

  @Column(name = "description", nullable = false)
  public String description;

  @Column(name = "status", nullable = false)
  public TaskStatus status;

  @Column(name = "total")
  public Integer total;

  @Column(name = "progress")
  public Integer progress;

  @Column(name = "startTimeMilliseconds")
  public long startTimeMilliseconds;

  @Column(name = "endTimeMilliseconds")
  public long endTimeMilliseconds;

  @Column(name = "strict")
  public boolean strict;
}
