package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.utils.StopWatch;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

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
    Table t = schema.create(table("Test1", column("id").pkey(), column("computed").computed("1;")));
    t.insert(new Row().set("id", 1));
    assertEquals(1, (int) t.query().retrieveRows().get(0).getInteger("computed"));

    t = schema.create(table("Test2", column("id").pkey(), column("computed").computed("id")));
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
