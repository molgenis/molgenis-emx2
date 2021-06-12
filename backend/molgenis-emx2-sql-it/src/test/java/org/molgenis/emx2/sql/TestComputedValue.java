package org.molgenis.emx2.sql;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;

public class TestComputedValue {
  static Database db;
  static Schema schema;

  @BeforeClass
  public static void setup() {
    db = TestDatabaseFactory.getTestDatabase();

    schema = db.dropCreateSchema(TestComputedValue.class.getSimpleName());
  }

  @Test
  public void test1() {
    Table t =
        schema.create(
            TableMetadata.table(
                "Test1",
                Column.column("id").setPkey(),
                Column.column("computed").setComputed("1;")));
    t.insert(new Row().set("id", 1));
    assertEquals(1, (int) t.query().retrieveRows().get(0).getInteger("computed"));

    t =
        schema.create(
            TableMetadata.table(
                "Test2",
                Column.column("id").setPkey(),
                Column.column("computed").setComputed("id")));
    t.insert(new Row().set("id", 1));
    assertEquals(1, (int) t.query().retrieveRows().get(0).getInteger("computed"));

    //    t = schema.create(table("Test3", column("id").pkey(), column("computed").computed("id")));
    //    List<Row> rows = new ArrayList<>();
    //    for (int i = 0; i < 10000; i++) {
    //      rows.add(new Row().set("id", i));
    //    }
    //
    //    // perf test;
    //    StopWatch.start("start");
    //    t.insert(rows);
    //    StopWatch.print("complete", 10000);
  }
}
