package org.molgenis.emx2.database.test;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.EmxException;
import org.molgenis.emx2.EmxTable;
import org.molgenis.emx2.database.EmxDatabaseImpl;
import org.molgenis.sql.SqlRow;
import org.slf4j.LoggerFactory;

import java.sql.Connection;

import static org.molgenis.emx2.EmxType.STRING;

public class EmxDatabaseTest {

  private static EmxDatabaseImpl db = null;

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

      // delete all tables first
      Connection conn = source.getConnection();
      conn.prepareCall(
              "DO $$ DECLARE\n"
                  + "    r RECORD;\n"
                  + "BEGIN\n"
                  + "    FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = current_schema()) LOOP\n"
                  + "        EXECUTE 'DROP TABLE IF EXISTS ' || quote_ident(r.tablename) || ' CASCADE';\n"
                  + "    END LOOP;\n"
                  + "END $$;")
          .execute();
      conn.close();
      db = new EmxDatabaseImpl(source);
    }

    // For the sake of this test, let's keep exception handling simple
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void test1() throws EmxException {
    EmxTable t = db.addTable("Person");
    t.addColumn("First name", STRING);
    t.addColumn("Last name", STRING);
    t.addColumn("Display Name", STRING).setUnique(true);

    SqlRow r =
        new SqlRow()
            .setString("First name", "Donald")
            .setString("Last name", "Duck")
            .setString("Display Name", "Donald Duck");
    db.save("Person", r);

    db.delete("Person", r);

    db.removeTable(t.getName());
  }
}
