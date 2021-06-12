package org.molgenis.emx2;

import static org.molgenis.emx2.ColumnType.INT;
import static org.molgenis.emx2.ColumnType.STRING;

import com.zaxxer.hikari.HikariDataSource;
import java.util.regex.Pattern;
import org.molgenis.emx2.examples.PetStoreExample;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.utils.EnvironmentProperty;
import org.molgenis.emx2.web.MolgenisWebservice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunMolgenisEmx2 {

  private static Logger logger = LoggerFactory.getLogger(RunMolgenisEmx2.class);

  public static void main(String[] args) {
    logger.info("Starting MOLGENIS EMX2 Software Version=" + Version.getVersion());

    String url =
        (String)
            EnvironmentProperty.getParameter(
                Constants.MOLGENIS_POSTGRES_URI, "jdbc:postgresql:molgenis", STRING);
    String user =
        (String)
            EnvironmentProperty.getParameter(Constants.MOLGENIS_POSTGRES_USER, "molgenis", STRING);
    String pass =
        (String)
            EnvironmentProperty.getParameter(Constants.MOLGENIS_POSTGRES_PASS, "molgenis", STRING);
    Integer port =
        (Integer) EnvironmentProperty.getParameter(Constants.MOLGENIS_HTTP_PORT, "8080", INT);

    logger.info("with " + org.molgenis.emx2.Constants.MOLGENIS_POSTGRES_URI + "=" + url);
    logger.info("with " + org.molgenis.emx2.Constants.MOLGENIS_POSTGRES_USER + "=" + user);
    logger.info("with " + org.molgenis.emx2.Constants.MOLGENIS_POSTGRES_PASS + "=<HIDDEN>");
    logger.info(
        "with "
            + Constants.MOLGENIS_HTTP_PORT
            + "="
            + port
            + " (change either via java properties or via ENV variables)");

    if (!Pattern.matches("[0-9A-Za-z/:]+", url)) {
      logger.error("Error: invalid " + Constants.MOLGENIS_POSTGRES_URI + " string. Found :" + url);
      return;
    }

    // create data source
    HikariDataSource dataSource = new HikariDataSource();
    dataSource.setJdbcUrl(url);
    dataSource.setUsername(user);
    dataSource.setPassword(pass);

    // setup database
    Database db = new SqlDatabase(dataSource, true);

    if (db.getSchema("pet store") == null) {
      Schema schema = db.createSchema("pet store");
      PetStoreExample.create(schema.getMetadata());
      PetStoreExample.populate(schema);
    }

    // start
    MolgenisWebservice.start(dataSource, port);
  }
}
