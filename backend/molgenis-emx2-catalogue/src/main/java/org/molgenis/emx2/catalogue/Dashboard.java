package org.molgenis.emx2.catalogue;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.DateBuilder.evenMinuteDate;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.Date;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

public class Dashboard {
  public Dashboard() {
    JobDetail job = newJob(RefreshJob.class).withIdentity("job1", "group1").build();
    // compute a time that is on the next round minute
    Date runTime = evenMinuteDate(new Date());

    // Trigger the job to run on the next round minute
    Trigger trigger =
        newTrigger()
            .withIdentity("trigger1", "group1")
            //            .startAt(runTime)
            .withSchedule(cronSchedule("0/20 * * * * ?"))
            .build();

    SchedulerFactory schedulerFactory = new StdSchedulerFactory();
    Scheduler scheduler = null;
    try {
      scheduler = schedulerFactory.getScheduler();
      scheduler.scheduleJob(job, trigger);
      scheduler.start();
    } catch (SchedulerException e) {
      e.printStackTrace();
    }
  }
}
