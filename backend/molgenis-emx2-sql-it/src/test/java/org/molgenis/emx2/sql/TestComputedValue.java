package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;

@Tag("slow")
public class TestComputedValue {
  static Database db;
  static Schema schema;

  @BeforeAll
  public static void setup() {
    db = TestDatabaseFactory.getTestDatabase();

    schema = db.dropCreateSchema(TestComputedValue.class.getSimpleName());
  }

  @Test
  public void test1() {
    Table t =
        schema.create(table("Test1", column("id").setPkey(), column("computed").setComputed("5;")));

    // reload to make sure 'computed' is really in backend
    db.clearCache();
    schema = db.getSchema(TestComputedValue.class.getSimpleName());

    t.insert(new Row().set("id", 1));
    assertEquals(5, (int) t.query().retrieveRows().get(0).getInteger("computed"));

    t = schema.create(table("Test2", column("id").setPkey(), column("computed").setComputed("id")));
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
