package org.molgenis.emx2.catalogue;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URISyntaxException;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RefreshJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(RefreshJob.class);

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    logger.info("Do count request");

    try {
      JsonNode jsonNode = new CountRequest().send();
      logger.info("response - " + jsonNode.toString());
    } catch (URISyntaxException | IOException | InterruptedException e) {
      logger.error("error on count CountRequest");
    }
  }
}
