package org.molgenis.emx2.database.test;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.EmxException;
import org.molgenis.emx2.EmxModel;
import org.molgenis.emx2.EmxTable;
import org.molgenis.emx2.EmxType;
import org.molgenis.emx2.database.EmxDatabase;
import org.molgenis.emx2.database.EmxDatabaseImpl;
import org.molgenis.emx2.database.EmxRow;
import org.molgenis.sql.SqlRow;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.molgenis.emx2.EmxType.*;

public class EmxDatabaseTest {

  private static EmxDatabase db = null;
  private static HikariDataSource ds = null;

  @BeforeClass
  public static void setUp() {
    ch.qos.logback.classic.Logger rootLogger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    rootLogger.setLevel(ch.qos.logback.classic.Level.INFO);

    String userName = "molgenis";
    String password = "molgenis";
    String url = "jdbc:postgresql:molgenis";

    try {
      ds = new HikariDataSource();
      ds.setJdbcUrl(url);
      ds.setUsername(userName);
      ds.setPassword(password);

      // delete all tables first
      Connection conn = ds.getConnection();
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
      db = new EmxDatabaseImpl(ds);
    }

    // For the sake of this test, let's keep exception handling simple
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void mrefTypeTest() throws EmxException {

    // create model
    EmxModel m = db.getModel();

    EmxTable parts = m.addTable("Parts");
    parts.addColumn("PartName", STRING);

    EmxTable products = m.addTable("Products");
    assertEquals(0, products.getColumns().size()); // molgenisid is hidden
    products.addColumn("ProductName", STRING);
    assertEquals(1, products.getColumns().size());
    assertNotNull(products.getColumn("ProductName"));

    products.addMref("Parts", parts, "ProductParts");

    // create data

    List<SqlRow> partsList = new ArrayList<>();
    partsList.add(new EmxRow().setString("PartName", "part1"));
    partsList.add(new EmxRow().setString("PartName", "part2"));
    db.save("Parts", partsList);

    SqlRow prod1 = new EmxRow();
    prod1.setString("ProductName", "prod1").setMref("Parts", partsList);
    db.save("Products", prod1);

    List<SqlRow> partsList2 = new ArrayList<>();
    partsList2.add(new EmxRow().setString("PartName", "part3"));
    partsList2.add(new EmxRow().setString("PartName", "part4"));
    db.save("Parts", partsList2);

    prod1.setMref("Parts", partsList2);
    db.save("Products", prod1); // updated

    // todo test that state is correct

    db.delete("Products", prod1);
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

    // create new database instance and see if Person is there
    EmxDatabase db2 = new EmxDatabaseImpl(ds);
    // db.getModel().diff(db2.getModel()); // should give no difference
    assertNotNull(db2.getModel().getTable("Person"));
    assertNotNull(db2.getModel().getTable("Person").getColumn("First name"));

    // todo test query

    db.delete("Person", r);
    System.out.println(db.getModel());
    db.getModel().removeTable(t.getName());
    assertNull(db.getModel().getTable("Person"));
  }

  @Test
  public void simpleTypeTest() throws EmxException {
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

    db.getModel().removeTable(t.getName());
  }
}
