package org.molgenis.emx2.catalogue;

import java.net.http.HttpResponse;
import java.util.Date;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RefreshJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(RefreshJob.class);

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    logger.info("Hello World! - " + new Date());

    HttpResponse response = new CountRequest().send();
    logger.info("response - " + response.body());
  }
}
