package org.molgenis.emx2;

import com.zaxxer.hikari.HikariDataSource;
import java.util.regex.Pattern;
import org.molgenis.emx2.examples.PetStoreExample;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.web.MolgenisWebservice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunMolgenisEmx2BackendOnly {
  private static Logger logger = LoggerFactory.getLogger(RunMolgenisEmx2BackendOnly.class);

  public static void main(String[] args) {
    String url = "jdbc:postgresql:molgenis";
    if (args.length == 1) {
      if (Pattern.matches("[0-9A-Za-z/:]+", args[0])) {
        url = args[0];
      } else {
        logger.error("Error: invalid jdbc string. Found '{0}'", args[0]);
        return;
      }
    } else {
      logger.info("You can pass custom postgresql jdbc url as first parameter as commandline");
    }
    logger.info("Starting EMX2 Version=" + Version.getVersion() + " with postgresql url: " + url);

    // create data source
    HikariDataSource dataSource = new HikariDataSource();
    dataSource.setJdbcUrl(url);
    dataSource.setUsername("molgenis");
    dataSource.setPassword("molgenis");

    // setup database
    Database db = new SqlDatabase(dataSource);
    if (db.getSchema("pet store") == null) {
      Schema schema = db.createSchema("pet store");
      PetStoreExample.create(schema.getMetadata());
      PetStoreExample.populate(schema);
    }

    // start
    MolgenisWebservice.start(dataSource);
  }
}
