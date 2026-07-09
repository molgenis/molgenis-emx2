package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

@Tag("slow")
class TestComputedOrDefaultValue {
  static Database db;
  static Schema schema;

  @BeforeAll
  static void setup() {
    db = TestDatabaseFactory.getTestDatabase();

    schema = db.dropCreateSchema(TestComputedOrDefaultValue.class.getSimpleName());
  }

  @Test
  void testComputed() {
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
  void testDefault() {
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

  @Test
  void testVisibleBasedOnComputedOntologyArray() {
    schema.create(table("Colors6321", column("name").setPkey()));
    schema.getTable("Colors6321").insert(row("name", "green"));

    schema.create(
        table(
            "Test6321",
            column("name").setPkey(),
            column("favoriteColors")
                .setType(ColumnType.ONTOLOGY_ARRAY)
                .setRefTable("Colors6321")
                .setComputed("[{name:'green'}]"),
            column("description")
                .setRequired(true)
                .setVisible("favoriteColors && favoriteColors.length > 0")));

    // reload to make sure expressions are really fetched from the backend
    db.clearCache();
    schema = db.getSchema(TestComputedOrDefaultValue.class.getSimpleName());
    Table t = schema.getTable("Test6321");

    t.insert(row("name", "Piet", "description", "Hello"));

    // favoriteColors computes to a non-empty array, so 'description' is visible and must be stored
    assertEquals("Hello", t.query().retrieveRows().get(0).getString("description"));
  }

  /**
   * A (required) column with a visible expression that depends on a computed INT column. When the
   * computed value makes the column invisible, upload should NOT raise a required error.
   */
  @Test
  void testVisibleBasedOnComputedInt() {
    schema.create(
        table(
            "Test6372",
            column("id").setPkey().setType(ColumnType.INT),
            column("ageComputed").setType(ColumnType.INT).setComputed("23"),
            column("schoolName").setRequired(true).setVisible("ageComputed < 18")));

    db.clearCache();
    schema = db.getSchema(TestComputedOrDefaultValue.class.getSimpleName());
    final Table table = schema.getTable("Test6372");

    // ageComputed = 23 (>= 18) so schoolName is invisible: empty value is allowed, no error
    assertDoesNotThrow(() -> table.insert(row("id", 1)));
    assertNull(table.query().retrieveRows().get(0).getString("schoolName"));
  }

  @Test
  void testVisibleBasedOnDefaultValue() {
    schema.create(
        table(
            "Test6373",
            column("id").setPkey().setType(ColumnType.INT),
            column("nationalBool").setType(ColumnType.BOOL).setDefaultValue("false"),
            column("firstName").setRequired(true).setVisible("nationalBool == false"),
            column("lastName").setRequired(true).setVisible("nationalBool == false")));

    db.clearCache();
    schema = db.getSchema(TestComputedOrDefaultValue.class.getSimpleName());
    final Table table = schema.getTable("Test6373");

    // nationalBool defaults to false, so firstName/lastName are visible and must be stored
    table.insert(row("id", 1, "firstName", "John", "lastName", "Doe"));

    final Row result = table.query().retrieveRows().get(0);
    assertArrayEquals(
        new String[] {"John", "Doe"},
        new String[] {result.getString("firstName"), result.getString("lastName")});
  }
}
