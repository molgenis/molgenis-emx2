package org.molgenis.emx2.database.test;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.EmxTable;
import org.molgenis.emx2.database.EmxDatabase;
import org.slf4j.LoggerFactory;

public class EmxDatabaseTest {

  private static EmxDatabase db = null;

  @BeforeClass
  public static void setUp() {
    ch.qos.logback.classic.Logger rootLogger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    rootLogger.setLevel(ch.qos.logback.classic.Level.INFO);

    String userName = "molgenis";
    String password = "molgenis";
    String url = "jdbc:postgresql:molgenis";

    try {
      HikariDataSource source = new HikariDataSource();
      source.setJdbcUrl(url);
      source.setUsername(userName);
      source.setPassword(password);

      db = new EmxDatabase(source);
    }

    // For the sake of this test, let's keep exception handling simple
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void test1() {

    EmxTable t = db.addTable();
  }
}
