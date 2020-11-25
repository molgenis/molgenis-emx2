package org.molgenis.emx2;

import static org.molgenis.emx2.ColumnType.INT;
import static org.molgenis.emx2.ColumnType.STRING;

import com.zaxxer.hikari.HikariDataSource;
import java.util.regex.Pattern;
import org.molgenis.emx2.examples.PetStoreExample;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.utils.TypeUtils;
import org.molgenis.emx2.web.MolgenisWebservice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunMolgenisEmx2 {
  public static final String MOLGENIS_POSTGRES_URI = "MOLGENIS_POSTGRES_URI";
  public static final String MOLGENIS_POSTGRES_USER = "MOLGENIS_POSTGRES_USER";
  public static final String MOLGENIS_POSTGRES_PASS = "MOLGENIS_POSTGRES_PASS";
  public static final String MOLGENIS_HTTP_PORT = "MOLGENIS_HTTP_PORT";

  private static Logger logger = LoggerFactory.getLogger(RunMolgenisEmx2.class);

  public static void main(String[] args) {
    String url = (String) getParameter(MOLGENIS_POSTGRES_URI, "jdbc:postgresql:molgenis", STRING);
    String user = (String) getParameter(MOLGENIS_POSTGRES_USER, "molgenis", STRING);
    String pass = (String) getParameter(MOLGENIS_POSTGRES_PASS, "molgenis", STRING);
    Integer port = (Integer) getParameter(MOLGENIS_HTTP_PORT, "8080", INT);

    if (!Pattern.matches("[0-9A-Za-z/:]+", url)) {
      logger.error("Error: invalid " + MOLGENIS_POSTGRES_URI + " string. Found :" + url);
      return;
    }

    logger.info("Starting MOLGENIS EMX2 Version=" + Version.getVersion());
    logger.info("with " + MOLGENIS_POSTGRES_URI + "=" + url);
    logger.info("with " + MOLGENIS_POSTGRES_USER + "=" + user);
    logger.info("with " + MOLGENIS_POSTGRES_PASS + "=<HIDDEN>");
    logger.info(
        "with "
            + MOLGENIS_HTTP_PORT
            + "="
            + port
            + " (change either via java properties or via ENV variables)");

    // create data source
    HikariDataSource dataSource = new HikariDataSource();
    dataSource.setJdbcUrl(url);
    dataSource.setUsername(user);
    dataSource.setPassword(pass);

    // setup database
    Database db = new SqlDatabase(dataSource);
    if (db.getSchema("pet store") == null) {
      Schema schema = db.createSchema("pet store");
      PetStoreExample.create(schema.getMetadata());
      PetStoreExample.populate(schema);
    }

    // start
    MolgenisWebservice.start(dataSource, port);
  }

  private static Object getParameter(String param, Object defaultValue, ColumnType type) {
    try {
      if (System.getProperty(param) != null) {
        return TypeUtils.getTypedValue(System.getProperty(param), type);
      } else if (System.getenv(param) != null) {
        return TypeUtils.getTypedValue(System.getenv(param), type);
      } else {
        return TypeUtils.getTypedValue(defaultValue, type);
      }
    } catch (Exception e) {
      throw new RuntimeException("Startup failed: could not read property/env variable " + param);
    }
  }
}
