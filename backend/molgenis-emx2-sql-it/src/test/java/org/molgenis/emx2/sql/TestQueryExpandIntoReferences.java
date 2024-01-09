package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.INT;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.TableMetadata.table;

import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.test.ProductComponentPartsExample;
import org.molgenis.emx2.utils.StopWatch;

public class TestQueryExpandIntoReferences {
  static Database db;

  @BeforeAll
  public static void setUp() throws SQLException {
    db = TestDatabaseFactory.getTestDatabase();

    // createColumn a schema to test with
    Schema schema = db.dropCreateSchema("TestQueryWithRefArray");

    // createColumn some tables with contents
    String PERSON = "Person";
    schema
        .getMetadata()
        .create(
            table(PERSON)
                .add(column("ID").setType(INT).setPkey())
                .add(column("First_Name").setKey(2).setRequired(true))
                .add(column("Father").setType(REF).setRefTable(PERSON))
                .add(column("Last_Name").setKey(2).setRequired(true)));

    Row father =
        new Row().setInt("ID", 1).setString("First_Name", "Donald").setString("Last_Name", "Duck");
    Row child =
        new Row()
            .setInt("ID", 2)
            .setString("First_Name", "Kwik")
            .setString("Last_Name", "Duck")
            .setInt("Father", father.getInteger("ID"));

    Table personTable = schema.getTable("Person");
    personTable.insert(father);
    personTable.insert(child);
  }

  @Test
  public void canExpandQueryInReferences() {

    StopWatch.start("canExpandQueryInReferences");

    Schema schema = db.getSchema("TestQueryWithRefArray");

    StopWatch.print("got schema");

    Query query1 =
        schema
            .query("Person")
            .select(s("First_Name"), s("Last_Name"), s("Father", s("First_Name"), s("Last_Name")))
            .where(f("Last_Name", EQUALS, "Duck"), f("Father", f("Last_Name", EQUALS, "Duck")));

    StopWatch.print("created query");

    List<Row> rows = query1.retrieveRows();
    for (Row r : rows) {
      System.out.println(r);
    }

    StopWatch.print("query complete");

    query1 =
        schema
            .query("Person")
            .select(
                s("First_Name"),
                s("Last_Name"),
                s("Father").select("Last_Name").select("First_Name"))
            .where(f("Last_Name", EQUALS, "Duck"))
            .where(f("Father", f("Last_Name", EQUALS, "Duck")));

    rows = query1.retrieveRows();
    for (Row r : rows) System.out.println(r);
    assertEquals(1, rows.size());

    StopWatch.print("second time");
  }

  @Test
  public void CanQueryExpandIntoArrayForeignKeys() {
    Schema schema = db.getSchema("TestQueryWithRefArray");

    StopWatch.start("CanQueryExpandIntoArrayForeignKeys");

    ProductComponentPartsExample.create(schema.getMetadata());
    ProductComponentPartsExample.populate(schema);

    StopWatch.print("tables created");

    Query q = schema.query("Product");
    q.select(s("name"), s("components", s("name"), s("parts", s("name"))));

    List<Row> rows = q.retrieveRows();
    assertEquals(3, rows.size());
    for (Row r : rows) {
      System.out.println(r);
    }

    StopWatch.print("query completed");

    // restart database and see if it is still there

    db.clearCache();
    schema = db.getSchema("TestQueryWithRefArray");

    StopWatch.print("cleared cache");

    Query q2 = schema.query("Product");
    q2.select(s("name"), s("components", s("name"), s("parts", s("name"))));

    // todo query expansion! q2.where("components", "parts",
    // "weight").eq(50).and("name").eq("explorer", "navigator");

    StopWatch.print("created query (needed to get metadata from disk)");

    List<Row> rows2 = q2.retrieveRows();
    assertEquals(3, rows2.size());
    for (Row r : rows2) {
      System.out.println(r);
    }

    StopWatch.print("queried again, cached so for free");
  }
}
