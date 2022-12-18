package org.molgenis.emx2.catalogue;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.DateBuilder.evenMinuteDate;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.util.Date;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.SqlDatabase;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dashboard {
  private static final Logger logger = LoggerFactory.getLogger(Dashboard.class);
  static final String DASHBOARD_SCHEMA = "_catalogue_dashboard";

  public Dashboard() {

    Database db = new SqlDatabase(false);

    // elevate privileges for init
    try {
      db.becomeAdmin();
      if (db.getSchema(DASHBOARD_SCHEMA) == null) {
        logger.info("Create new DASHBOARD_SCHEMA: " + DASHBOARD_SCHEMA);
        Schema schema = db.createSchema(DASHBOARD_SCHEMA);
        new DashboardLoader().load(schema, true);
      }
    } finally {
      // ensure to remove admin
      db.clearActiveUser();
    }

    JobDetail job = newJob(RefreshJob.class).withIdentity("job1", "group1").build();
    // compute a time that is on the next round minute
    Date runTime = evenMinuteDate(new Date());

    // Trigger the job to run on the next round minute
    Trigger trigger =
        newTrigger()
            .withIdentity("trigger1", "group1")
            // .startAt(runTime)
            .withSchedule(cronSchedule("0/20 * * * * ?"))
            .build();

    SchedulerFactory schedulerFactory = new StdSchedulerFactory();
    try {
      Scheduler scheduler = schedulerFactory.getScheduler();
      scheduler.scheduleJob(job, trigger);
      scheduler.start();
    } catch (SchedulerException e) {
      logger.error(e.toString());
    }
  }
}
