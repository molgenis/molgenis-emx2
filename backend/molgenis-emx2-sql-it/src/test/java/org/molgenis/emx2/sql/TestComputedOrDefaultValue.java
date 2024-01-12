package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

@Tag("slow")
public class TestComputedOrDefaultValue {
  static Database db;
  static Schema schema;

  @BeforeAll
  public static void setup() {
    db = TestDatabaseFactory.getTestDatabase();

    schema = db.dropCreateSchema(TestComputedOrDefaultValue.class.getSimpleName());
  }

  @Test
  public void testComputed() {
    Table t =
        schema.create(table("Test1", column("id").setPkey(), column("computed").setComputed("5;")));

    // reload to make sure 'computed' is really in backend
    db.clearCache();
    schema = db.getSchema(TestComputedOrDefaultValue.class.getSimpleName());

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

  @Test
  public void testDefault() {
    final Table t =
        schema.create(
            table("Test3", column("id").setPkey(), column("hasDefault").setDefaultValue("blaat")));

    t.insert(new Row().set("id", 1));
    assertEquals("blaat", t.query().retrieveRows().get(0).getString("hasDefault"));

    final Table t2 =
        schema.create(
            table(
                "Test4",
                column("id").setPkey(),
                column("autoDate")
                    .setType(ColumnType.DATE)
                    .setDefaultValue("=new Date().toISOString().substring(0,10)"),
                column("autoDateTime")
                    .setType(ColumnType.DATETIME)
                    .setDefaultValue("=new Date().toISOString()")));
    t2.insert(new Row().set("id", 1));
    final Row result = t2.query().retrieveRows().get(0);
    assertDoesNotThrow(() -> result.getDate("autoDate"));
    assertDoesNotThrow(() -> result.getDate("autoDateTime"));

    final Table t3 =
        schema.create(
            table(
                "Test5",
                column("id").setPkey(),
                column("ontologyArray")
                    .setType(ColumnType.ONTOLOGY_ARRAY)
                    .setRefTable("Colors")
                    .setDefaultValue("=[{name:\"green\"}]"),
                column("ontology")
                    .setType(ColumnType.ONTOLOGY)
                    .setRefTable("Colors")
                    .setDefaultValue("={name:\"green\"}")));
    schema.getTable("Colors").insert(row("name", "green"));

    t3.insert(new Row().set("id", 1));
    final Row result2 = t3.query().retrieveRows().get(0);
    assertEquals("green", result2.getString("ontology"));
    assertEquals("green", result2.getStringArray("ontologyArray")[0]);
  }
}
