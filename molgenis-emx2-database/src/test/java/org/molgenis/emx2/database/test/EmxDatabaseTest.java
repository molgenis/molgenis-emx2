package org.molgenis.emx2.database.test;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.database.EmxDatabase;
import org.molgenis.emx2.database.EmxDatabaseImpl;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.*;
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

    EmxTable parts = m.createTable("Parts");
    parts.addColumn("PartName", STRING);

    EmxTable products = m.createTable("Products");
    assertEquals(0, products.getColumns().size()); // molgenisid is hidden
    products.addColumn("ProductName", STRING);
    assertEquals(1, products.getColumns().size());
    assertNotNull(products.getColumn("ProductName"));

    products.addMref("Parts", parts, "ProductParts");

    // create data

    List<EmxRow> partsList = new ArrayList<>();
    partsList.add(new EmxRow().setString("PartName", "part1"));
    partsList.add(new EmxRow().setString("PartName", "part2"));
    db.save("Parts", partsList);

    EmxRow prod1 = new EmxRow();
    prod1.setString("ProductName", "prod1").setMref("Parts", partsList);
    db.save("Products", prod1);

    List<EmxRow> partsList2 = new ArrayList<>();
    partsList2.add(new EmxRow().setString("PartName", "part3"));
    partsList2.add(new EmxRow().setString("PartName", "part4"));
    db.save("Parts", partsList2);

    prod1.setMref("Parts", partsList2);
    db.save("Products", prod1); // updated

    // todo test that state is correct
    assertEquals(2, db.query("ProductParts").fetch().size());
    db.delete("Products", prod1);
    assertEquals(0, db.query("ProductParts").fetch().size());

    db.getModel().removeTable("Products"); // should also remove mref table
    db.getModel().removeTable("Parts");
    assertNull(db.getModel().getTable("ProductParts"));
  }

  @Test
  public void test1() throws EmxException {
    EmxTable t = db.getModel().createTable("Person");
    t.addColumn("First name", STRING);
    t.addColumn("Last name", STRING);
    t.addColumn("Display Name", STRING).setUnique(true);

    EmxRow r =
        new EmxRow()
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
    EmxTable t = db.getModel().createTable("TypeTest");
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

  @Test
  public void testQuery() throws EmxException {
    long startTime = System.currentTimeMillis();

    EmxTable part = db.getModel().createTable("Part");
    part.addColumn("name", STRING);
    part.addColumn("weight", INT);
    part.addUnique("name");

    EmxRow part1 = new EmxRow().setString("name", "forms").setInt("weight", 100);
    EmxRow part2 = new EmxRow().setString("name", "login").setInt("weight", 50);
    db.save("Part", part1);
    db.save("Part", part2);

    EmxTable component = db.getModel().createTable("Component");
    component.addColumn("name", STRING);
    component.addMref("parts", part, "ComponentParts");
    component.addUnique("name");

    EmxRow component1 = new EmxRow().setString("name", "explorer").setMref("parts", part1, part2);
    EmxRow component2 = new EmxRow().setString("name", "navigator").setMref("parts", part2);
    db.save("Component", component1);
    db.save("Component", component2);

    EmxTable product = db.getModel().createTable("Product");
    product.addColumn("name", STRING);
    product.addMref("components", component, "ProductComponents");
    product.addUnique("name");

    EmxRow product1 =
        new EmxRow().setString("name", "molgenis").setMref("components", component1, component2);
    db.save("Product", product1);

    long endTime = System.currentTimeMillis();
    System.out.println("Creation took " + (endTime - startTime) + " milliseconds");

    // now getQuery to show product.name and parts.name linked by path Assembly.product,part

    // needed:
    // join+columns paths, potentially multiple paths. We only support outer join over relationships
    // if names are not unique, require explicit select naming
    // complex nested where clauses
    // sortby clauses
    // later: group by.

    //        SqlQueryImpl q1 = new PsqlQueryBack(db);
    // db        q1.select("Product").columns("name").as("productName");
    //        q1.mref("ProductComponent").columns("name").as("componentName");
    //        q1.mref("ComponentPart").columns("name").as("partName");
    //        //q1.where("productName").eq("molgenis");
    //
    //        System.out.println(q1);

    //    try {
    //      db.query("pietje");
    //      fail("exception handling from(pietje) failed");
    //    } catch (Exception e) {
    //      // good stuff
    //    }
    //
    //    try {
    //      db.query("Product").as("p").join("ProductComponent", "p2", "product");
    //      fail("should fail because faulty toTabel");
    //    } catch (Exception e) {
    //      // good stuff
    //    }
    //
    //    try {
    //      db.query("Product").as("p").join("ProductComponent", "p", "component");
    //      fail("should fail because faulty 'on'");
    //    } catch (Exception e) {
    //      // good stuff
    //    }
    //    try {
    //      db.query("Product").as("p").select("wrongname").as("productName");
    //      fail("should fail because faulty 'select'");
    //    } catch (Exception e) {
    //      // good stuff
    //    }
    //
    //    startTime = System.currentTimeMillis();
    //    SqlQuery q2 = db.query("Product").as("p").select("name").as("productName");
    //    q2.join("ProductComponent", "p", "product").as("pc");
    //    q2.join("Component", "pc", "component").as("c").select("name").as("componentName");
    //    q2.join("ComponentPart", "c", "component").as("cp");
    //    q2.join("Part", "cp", "part").as("p2").select("name").as("partName").select("weight");
    //    q2.eq("p2", "weight", 50).eq("c", "name", "explorer", "navigator");
    //    for (SqlRow row : q2.retrieve()) {
    //      System.out.println(row);
    //    }
    //    endTime = System.currentTimeMillis();
    //    System.out.println("Query took " + (endTime - startTime) + " milliseconds");
    //    System.out.println("Query contents: " + q2);
  }
}
