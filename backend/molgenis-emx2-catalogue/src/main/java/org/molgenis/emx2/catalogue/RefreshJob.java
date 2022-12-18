package org.molgenis.emx2.catalogue;

import static org.molgenis.emx2.catalogue.Dashboard.DASHBOARD_SCHEMA;
import static org.molgenis.emx2.catalogue.DashboardLoader.COUNTS;
import static org.molgenis.emx2.catalogue.DashboardLoader.DATA;
import static org.molgenis.emx2.catalogue.DashboardLoader.MOMENT;
import static org.molgenis.emx2.catalogue.DashboardLoader.NAME_COLUMN;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.SqlDatabase;
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

      Database db = new SqlDatabase(false);
      try {
        db.becomeAdmin();
        Schema schema = db.getSchema(DASHBOARD_SCHEMA);
        schema
            .getTable(COUNTS)
            .insert(
                new Row()
                    .set(NAME_COLUMN, "data catalogue")
                    .set(
                        MOMENT,
                        LocalDateTime.ofInstant(
                            jobExecutionContext.getFireTime().toInstant(), ZoneId.systemDefault()))
                    .set(DATA, jsonNode.findValue("count").intValue()));
      } finally {
        // ensure to remove admin
        db.clearActiveUser();
      }

    } catch (URISyntaxException | IOException | InterruptedException e) {
      logger.error("error on count CountRequest");
    }
  }
}
