package org.molgenis.emx2.database.test;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.EmxException;
import org.molgenis.emx2.EmxTable;
import org.molgenis.emx2.EmxType;
import org.molgenis.emx2.database.EmxDatabaseImpl;
import org.molgenis.emx2.database.EmxRow;
import org.molgenis.sql.SqlRow;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.EmxType.*;

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
    EmxTable t = db.getModel().addTable("Person");
    t.addColumn("First name", STRING);
    t.addColumn("Last name", STRING);
    t.addColumn("Display Name", STRING).setUnique(true);

    SqlRow r =
        new SqlRow()
            .setString("First name", "Donald")
            .setString("Last name", "Duck")
            .setString("Display Name", "Donald Duck");
    db.save("Person", r);

    // todo test query

    db.delete("Person", r);

    System.out.println(db.getModel());

    db.getModel().removeTable(t.getName());
  }

  @Test
  public void test2() throws EmxException {
    EmxTable t = db.getModel().addTable("TypeTest");
    t.addColumn("myid", STRING).setUnique(true);
    for (EmxType type : Arrays.asList(UUID, STRING, INT, BOOL, DECIMAL, TEXT, DATE, DATETIME)) {
      t.addColumn("test_" + type.toString().toLowerCase(), type);
      t.addColumn("test_" + type.toString().toLowerCase() + "_nillable", type).setNillable(true);
    }

    // TODO would like type safety via db.new("TypeTest").....save(); which then checks for
    // non-existing fields to be set.
    EmxRow row = new EmxRow();
    row.setString("myid", "hello world");
    row.setUuid("test_uuid", java.util.UUID.randomUUID());
    row.setString("test_string", "test");
    row.setBool("test_bool", true);
    row.setInt("test_int", 1);
    row.setDecimal("test_decimal", 1.1);
    row.setText("test_text", "testtext");
    row.setDate("test_date", LocalDate.of(2018, 12, 13));
    row.setDateTime("test_datetime", OffsetDateTime.of(2018, 12, 13, 12, 40, 0, 0, ZoneOffset.UTC));

    db.save("TypeTest", row);

    int count = t.getColumns().size();
    t.removeColumn("myid");
    assertEquals(count - 1, t.getColumns().size());
  }
}
