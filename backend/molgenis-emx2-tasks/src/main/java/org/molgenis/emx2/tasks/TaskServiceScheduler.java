package org.molgenis.emx2.tasks;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.List;
import org.molgenis.emx2.MolgenisException;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.utils.Key;

public class TaskServiceScheduler {
  protected final Scheduler quartzScheduler;
  private static TaskService taskService;
  private static String EMPTY_PARAMETERSS = "";

  public TaskServiceScheduler(TaskService newTaskService) {
    try {
      taskService = newTaskService;
      this.quartzScheduler = new StdSchedulerFactory().getScheduler();
      this.quartzScheduler.start();
    } catch (Exception e) {
      throw new MolgenisException("Creation of task scheduler failed", e);
    }
  }

  public List<String> scheduledTaskNames() {
    try {
      return quartzScheduler.getJobKeys(GroupMatcher.anyGroup()).stream()
          .map(Key::getName)
          .toList();
    } catch (Exception e) {
      throw new MolgenisException("Listing of scheduled jobs failed", e);
    }
  }

  public void schedule(ScriptTask task) {
    String cronExpression = task.getCronExpression();

    // Validate cron expression
    try {
      CronExpression.validateExpression(cronExpression);
    } catch (Exception e) {
      throw new MolgenisException("Invalid cronexpression '" + cronExpression + "'", e);
    }

    try {
      // If already scheduled, remove it from the quartzScheduler
      if (quartzScheduler.checkExists(new JobKey(task.getName(), Key.DEFAULT_GROUP))) {
        unschedule(task.getName());
      }

      // create the job
      JobDetail job =
          newJob(SubmitJob.class).withIdentity(task.getName(), Key.DEFAULT_GROUP).build();

      // Schedule with 'cron' trigger
      CronTrigger trigger =
          newTrigger()
              .withIdentity(task.getName(), Key.DEFAULT_GROUP)
              .forJob(task.getName(), Key.DEFAULT_GROUP)
              .withSchedule(cronSchedule(cronExpression))
              .build();

      // schedule
      quartzScheduler.scheduleJob(job, trigger);
    } catch (Exception e) {
      throw new MolgenisException("Job scheduling failed", e);
    }
  }

  public void unschedule(String name) {
    try {
      JobKey jobKey = new JobKey(name, Key.DEFAULT_GROUP);
      quartzScheduler.unscheduleJob(TriggerKey.triggerKey(name, Key.DEFAULT_GROUP));
      quartzScheduler.deleteJob(jobKey);
      quartzScheduler
          .getCurrentlyExecutingJobs()
          .forEach(
              jobExecutionContext -> {
                // stop jobs spawned from this jobkey
                if (jobExecutionContext.getJobInstance() instanceof SubmitJob submitJob
                    && jobExecutionContext.getJobDetail().getKey().getName().equals(name)) {
                  submitJob.stop();
                }
              });
    } catch (Exception e) {
      throw new MolgenisException(String.format("Error deleting ScheduledJob %s", name), e);
    }
  }

  public void update(ScriptTask scriptTask) {
    // if no cron we remove
    if (scriptTask.getCronExpression() == null) {
      unschedule(scriptTask.getName());
    }
    // otherwise we schedule
    else {
      schedule(scriptTask);
    }
  }

  public static class SubmitJob implements Job {
    private String molgenisTaskId; // from molgenis
    TaskStatus status;

    public SubmitJob() {
      // no arg constructor
    }

    @Override
    public void execute(JobExecutionContext context) {
      String name = context.getJobDetail().getKey().getName();
      try {
        // id of submitted task
        molgenisTaskId = taskService.submitTaskFromName(name, EMPTY_PARAMETERSS);
        // need to keep it in running state until complete so we can interupt if needed
        status = taskService.getTask(molgenisTaskId).getStatus();
        while (status.equals(TaskStatus.WAITING)
            || status.equals(TaskStatus.RUNNING)
            || status.equals(TaskStatus.UNKNOWN)) {
          status = taskService.getTask(molgenisTaskId).getStatus();
          Thread.sleep(1000);
        }
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
      }
    }

    public void stop() {
      status = TaskStatus.ERROR;
      if (molgenisTaskId != null) {
        Task<?> task = taskService.getTask(molgenisTaskId);
        task.stop();
        task.setError("Job interupted, probably being unscheduled");
      }
    }
  }
}
