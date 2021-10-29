package org.molgenis.emx2.sql;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertArrayEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.jooq.DSLContext;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;

public class TestMergeAlter {
  private static final String REF_TARGET = "RefTarget";
  private static final String REF_TABLE = "RefTable";
  private static final String ID_COLUMN = "id";
  private static final String REF_COLUMN = "ref";
  private static final String REF_ARRAY_TARGET = "RefArrayTarget";
  private static final String REF_ARRAY_TABLE = "RefArrayTable";
  private static final String REFBACK_COLUMN = "refBack";

  static Database db;
  static Schema schema;

  @BeforeClass
  public static void setup() {
    db = TestDatabaseFactory.getTestDatabase();
    schema = db.dropCreateSchema(TestMergeAlter.class.getSimpleName());
  }

  @Test
  public void testRef() {
    executeRelationshipTest(REF_TARGET, REF_TABLE, REF, "target1");
  }

  @Test
  public void testRefArray() {
    executeRelationshipTest(REF_ARRAY_TARGET, REF_ARRAY_TABLE, REF_ARRAY, "{target1}");
  }

  private void executeRelationshipTest(
      String targetTableName, String refTableName, ColumnType refColumnType, String stringValue) {
    SchemaMetadata newSchema = new SchemaMetadata();
    newSchema
        .create(table(targetTableName).add(column(ID_COLUMN).setPkey()))
        .add(
            column(REFBACK_COLUMN)
                .setType(ColumnType.REFBACK)
                .setRefTable(refTableName)
                .setRefBack(REF_COLUMN));
    newSchema.create(
        table(refTableName)
            .add(column(ID_COLUMN).setPkey())
            .add(column(REF_COLUMN).setType(refColumnType).setRefTable(targetTableName)));

    schema.migrate(newSchema);

    schema.getTable(targetTableName).insert(new Row().set(ID_COLUMN, "target1"));
    schema
        .getTable(refTableName)
        .insert(new Row().set(ID_COLUMN, "ref1").set(REF_COLUMN, "target1"));

    // this should fail
    try {
      schema
          .getTable(refTableName)
          .update(new Row().set(ID_COLUMN, "ref1").set(REF_COLUMN, "target_fail"));
      fail("should have failed");
    } catch (Exception e) {
      // correct
    }

    // should fail because refBack still exists
    try {
      schema
          .getTable(refTableName)
          .getMetadata()
          .alterColumn(new Column(REF_COLUMN).setType(STRING));
      fail("should not be possible to alter ref that has refBack");
    } catch (Exception e) {
      // correct
    }

    try {
      schema
          .getTable(refTableName)
          .getMetadata()
          .alterColumn(new Column(REF_COLUMN).setType(STRING));
      fail("should not be possible to drop ref that has refBack");
    } catch (Exception e) {
      // correct
    }

    // delete refBack, than it should work
    schema.getTable(targetTableName).getMetadata().dropColumn("refBack");
    schema.getTable(refTableName).getMetadata().alterColumn(new Column(REF_COLUMN).setType(STRING));

    // this should work
    schema
        .getTable(refTableName)
        .update(
            new Row().set(ID_COLUMN, "ref1").set(REF_COLUMN, "target_fail")); // it is a string now

    // this should fail
    try {
      schema
          .getTable(REF_TABLE)
          .getMetadata()
          .alterColumn(new Column(REF_COLUMN).setType(refColumnType));
      fail("cast to column with faulty xref values should fail");
    } catch (Exception e) {
      // correct
    }
    schema
        .getTable(refTableName)
        .update(new Row().set(ID_COLUMN, "ref1").set(REF_COLUMN, stringValue));

    // restore the reference, including refback
    schema
        .getTable(refTableName)
        .getMetadata()
        .alterColumn(new Column(REF_COLUMN).setType(refColumnType).setRefTable(targetTableName));
    schema
        .getTable(targetTableName)
        .getMetadata()
        .add(
            new Column(REFBACK_COLUMN)
                .setType(ColumnType.REFBACK)
                .setRefTable(refTableName)
                .setRefBack(REF_COLUMN));

    // finally check change from ref to ref_array should keep refback
    //    if (REF.equals(refColumnType)) {
    //      schema
    //          .getTable(refTableName)
    //          .getMetadata()
    //          .alterColumn(
    //              new Column(REF_COLUMN)
    //                  .setType(REF_ARRAY)
    //                  .setRefTable(targetTableName)
    //                  .setNullable(true));
    //
    //      // check refback did not dissapear
    //      assertNotNull(schema.getTable(targetTableName).getMetadata().getColumn("refback"));
    //
    //      // use refback for an update to 'null'
    //      //      schema
    //      //          .getTable(targetTableName)
    //      //          .update(new Row().set(ID_COLUMN, "target1").setStringArray(REFBACK_COLUMN));
    //      //
    // assertNull(schema.getTable(refTableName).retrieve().get(0).getString(REF_COLUMN));
    //
    //      // should fail
    //      try {
    //        schema
    //            .getTable(targetTableName)
    //            .update(
    //                new Row().set(ID_COLUMN, "target1").setStringArray(REFBACK_COLUMN, "should
    // fail"));
    //        fail("refback should check foreign key validity");
    //      } catch (Exception e) {
    //        // correct
    //      }
    //      schema
    //          .getTable(targetTableName)
    //          .update(new Row().set(ID_COLUMN, "target1").setStringArray(REFBACK_COLUMN, "ref1"));
    //      assertEquals(
    //          "target1",
    // schema.getTable(refTableName).retrieveRows().get(0).getString(REF_COLUMN));
    //    }
  }

  @Test
  public void testRenameTable() {

    SchemaMetadata s =
        new SchemaMetadata("temp")
            .create(
                table("testRenameTable", column("ID").setKey(1), column("Name")),
                table(
                    "testRenameTableRefOld",
                    column("ID").setKey(1),
                    column("Name"),
                    column("Ref").setType(REF_ARRAY).setRefTable("testRenameTable")));

    // test via migrate so we have full stack
    assertNull(schema.getTable("testRenameTableRefOld"));
    schema.migrate(s);
    assertNotNull(schema.getTable("testRenameTableRefOld"));

    // now, we check trigger name
    DSLContext jooq = ((SqlSchemaMetadata) schema.getMetadata()).getJooq();
    jooq.resultQuery(
        "SELECT trigger_name from information_schema.triggers  WHERE trigger_name LIKE '%testRenameTableRefOld%'");
    assertEquals(
        6,
        jooq
            .resultQuery(
                "SELECT trigger_name from information_schema.triggers  WHERE trigger_name LIKE '%testRenameTableRefOld%'")
            .stream()
            .count());

    // now create migration
    SchemaMetadata migration =
        new SchemaMetadata()
            .create(table("testRenameTableRefNew").setOldName("testRenameTableRefOld"))
            .getSchema();
    schema.migrate(migration);
    assertNull(schema.getTable("testRenameTableRefOld"));
    assertNotNull(schema.getTable("testRenameTableRefNew"));
    db.clearCache();
    schema = db.getSchema(schema.getName());
    assertNull(schema.getTable("testRenameTableRefOld"));
    assertNotNull(schema.getTable("testRenameTableRefNew"));
    assertEquals(
        0,
        jooq
            .resultQuery(
                "SELECT trigger_name from information_schema.triggers  WHERE trigger_name LIKE '%testRenameTableRefOld%'")
            .stream()
            .count());
    assertEquals(
        6,
        jooq
            .resultQuery(
                "SELECT trigger_name from information_schema.triggers  WHERE trigger_name LIKE '%testRenameTableRefNew%'")
            .stream()
            .count());
  }

  @Test
  public void testAlterKeys() {
    Table t = schema.create(table("TestAlterKeys", column("ID").setKey(1), column("Name")));

    t.getMetadata().alterColumn("Name", column("Name").setKey(2));

    assertEquals(2, t.getMetadata().getColumn("Name").getKey());
  }

  @Test
  public void testFromAndToHeading() {
    Table t = schema.create(table("TestHeading", column("ID").setKey(1), column("Name")));
    t.insert(row("ID", "1", "Name", "bla"));
    assertEquals("bla", t.retrieveRows().get(0).getString("Name"));

    // make to a heading
    t.getMetadata().alterColumn("Name", t.getMetadata().getColumn("Name").setType(HEADING));
    assertNull(null, t.retrieveRows().get(0).getString("Name"));

    // and back again
    t.getMetadata().alterColumn("Name", t.getMetadata().getColumn("Name").setType(STRING));
    t.update(row("ID", "1", "Name", "bla2"));
    assertEquals("bla2", t.retrieveRows().get(0).getString("Name"));
  }

  @Test
  public void testSimpleTypes() {

    // simple
    executeAlterType(STRING, "true", BOOL, true);
    executeAlterType(STRING, "1", INT, 1);
    executeAlterType(STRING, "1.0", DECIMAL, 1.0);
    executeAlterType(INT, 1, DECIMAL, 1.0);

    LocalDate date = LocalDate.now();
    LocalDateTime time = LocalDateTime.now();
    // todo: I would actually like always to get rid to 'T' is that systemwide settable?
    executeAlterType(STRING, date.toString(), DATE, date);
    // rounding error executeAlterType(STRING, time.toString().replace("T", " "), DATETIME, time);
    executeAlterType(
        DATE,
        date,
        DATETIME,
        LocalDateTime.of(date.getYear(), date.getMonth(), date.getDayOfMonth(), 0, 0));

    // array
    executeAlterType(STRING_ARRAY, new String[] {"1"}, INT_ARRAY, new Integer[] {1});
    executeAlterType(INT_ARRAY, new Integer[] {1, 2}, DECIMAL_ARRAY, new Double[] {1.0, 2.0});

    // mixed
    executeAlterType(INT, 1, INT_ARRAY, new Integer[] {1}, false);
    executeAlterType(STRING_ARRAY, new String[] {"aap,noot"}, STRING, "{\"aap,noot\"}", false);
    executeAlterType(STRING, "aap", STRING_ARRAY, new String[] {"aap"}, false);
  }

  private void executeAlterType(
      ColumnType fromType, Object fromVal, ColumnType toType, Object toVal) {
    executeAlterType(fromType, fromVal, toType, toVal, true);
  }

  private void executeAlterType(
      ColumnType fromType, Object fromVal, ColumnType toType, Object toVal, boolean roundtrip) {
    String tableName = "TEST_ALTER_" + fromType.toString() + "_TO_" + toType.toString();
    schema.create(
        new TableMetadata(tableName).add(column("id").setPkey(), column("col1").setType(fromType)));
    schema.getTable(tableName).insert(new Row().set("id", "test1").set("col1", fromVal));
    schema.getTable(tableName).getMetadata().alterColumn(new Column("col1").setType(toType));

    if (toVal instanceof Object[]) {
      assertArrayEquals(
          (Object[]) toVal,
          (Object[])
              schema.getTable(tableName).retrieveRows().get(0).get("col1", toVal.getClass()));
    } else {
      assertEquals(
          toVal, schema.getTable(tableName).retrieveRows().get(0).get("col1", toVal.getClass()));
    }
    // also when converted back?
    if (roundtrip) {
      schema.getTable(tableName).getMetadata().alterColumn(new Column("col1").setType(fromType));

      if (fromVal instanceof Object[]) {
        assertArrayEquals(
            (Object[]) fromVal,
            (Object[])
                schema.getTable(tableName).retrieveRows().get(0).get("col1", fromVal.getClass()));
      } else {
        assertEquals(
            fromVal,
            schema.getTable(tableName).retrieveRows().get(0).get("col1", fromVal.getClass()));
      }
    }
  }
}
