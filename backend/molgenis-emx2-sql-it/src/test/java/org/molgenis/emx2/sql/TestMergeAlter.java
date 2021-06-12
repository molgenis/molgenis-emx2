package org.molgenis.emx2.sql;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertArrayEquals;
import static org.molgenis.emx2.ColumnType.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import junit.framework.TestCase;
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
    executeRelationshipTest(REF_TARGET, REF_TABLE, ColumnType.REF, "target1");
  }

  @Test
  public void testRefArray() {
    executeRelationshipTest(REF_ARRAY_TARGET, REF_ARRAY_TABLE, ColumnType.REF_ARRAY, "{target1}");
  }

  private void executeRelationshipTest(
      String targetTableName, String refTableName, ColumnType refColumnType, String stringValue) {
    SchemaMetadata newSchema = new SchemaMetadata();
    newSchema
        .create(TableMetadata.table(targetTableName).add(Column.column(ID_COLUMN).setPkey()))
        .add(
            Column.column(REFBACK_COLUMN)
                .setType(ColumnType.REFBACK)
                .setRefTable(refTableName)
                .setRefBack(REF_COLUMN));
    newSchema.create(
        TableMetadata.table(refTableName)
            .add(Column.column(ID_COLUMN).setPkey())
            .add(Column.column(REF_COLUMN).setType(refColumnType).setRefTable(targetTableName)));

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
          .alterColumn(new Column(REF_COLUMN).setType(ColumnType.STRING));
      fail("should not be possible to alter ref that has refBack");
    } catch (Exception e) {
      // correct
    }

    try {
      schema
          .getTable(refTableName)
          .getMetadata()
          .alterColumn(new Column(REF_COLUMN).setType(ColumnType.STRING));
      fail("should not be possible to drop ref that has refBack");
    } catch (Exception e) {
      // correct
    }

    // delete refBack, than it should work
    schema.getTable(targetTableName).getMetadata().dropColumn("refBack");
    schema
        .getTable(refTableName)
        .getMetadata()
        .alterColumn(new Column(REF_COLUMN).setType(ColumnType.STRING));

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
                TableMetadata.table(
                    "testRenameTable", Column.column("ID").setKey(1), Column.column("Name")),
                TableMetadata.table(
                    "testRenameTableRefOld",
                    Column.column("ID").setKey(1),
                    Column.column("Name"),
                    Column.column("Ref")
                        .setType(ColumnType.REF_ARRAY)
                        .setRefTable("testRenameTable")));

    // test via migrate so we have full stack
    TestCase.assertNull(schema.getTable("testRenameTableRefOld"));
    schema.migrate(s);
    TestCase.assertNotNull(schema.getTable("testRenameTableRefOld"));

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
            .create(
                TableMetadata.table("testRenameTableRefNew").setOldName("testRenameTableRefOld"))
            .getSchema();
    schema.migrate(migration);
    TestCase.assertNull(schema.getTable("testRenameTableRefOld"));
    TestCase.assertNotNull(schema.getTable("testRenameTableRefNew"));
    db.clearCache();
    schema = db.getSchema(schema.getName());
    TestCase.assertNull(schema.getTable("testRenameTableRefOld"));
    TestCase.assertNotNull(schema.getTable("testRenameTableRefNew"));
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
    Table t =
        schema.create(
            TableMetadata.table(
                "TestAlterKeys", Column.column("ID").setKey(1), Column.column("Name")));

    t.getMetadata().alterColumn("Name", Column.column("Name").setKey(2));

    TestCase.assertEquals(2, t.getMetadata().getColumn("Name").getKey());
  }

  @Test
  public void testSimpleTypes() {

    // simple
    executeAlterType(ColumnType.STRING, "true", ColumnType.BOOL, true);
    executeAlterType(ColumnType.STRING, "1", ColumnType.INT, 1);
    executeAlterType(ColumnType.STRING, "1.0", ColumnType.DECIMAL, 1.0);
    executeAlterType(ColumnType.INT, 1, ColumnType.DECIMAL, 1.0);

    LocalDate date = LocalDate.now();
    LocalDateTime time = LocalDateTime.now();
    // todo: I would actually like always to get rid to 'T' is that systemwide settable?
    executeAlterType(ColumnType.STRING, date.toString(), ColumnType.DATE, date);
    // rounding error executeAlterType(STRING, time.toString().replace("T", " "), DATETIME, time);
    executeAlterType(
        ColumnType.DATE,
        date,
        ColumnType.DATETIME,
        LocalDateTime.of(date.getYear(), date.getMonth(), date.getDayOfMonth(), 0, 0));

    // array
    executeAlterType(
        ColumnType.STRING_ARRAY, new String[] {"1"}, ColumnType.INT_ARRAY, new Integer[] {1});
    executeAlterType(
        ColumnType.INT_ARRAY,
        new Integer[] {1, 2},
        ColumnType.DECIMAL_ARRAY,
        new Double[] {1.0, 2.0});

    // mixed
    executeAlterType(ColumnType.INT, 1, ColumnType.INT_ARRAY, new Integer[] {1}, false);
    executeAlterType(
        ColumnType.STRING_ARRAY,
        new String[] {"aap,noot"},
        ColumnType.STRING,
        "{\"aap,noot\"}",
        false);
    executeAlterType(
        ColumnType.STRING, "aap", ColumnType.STRING_ARRAY, new String[] {"aap"}, false);
  }

  private void executeAlterType(
      ColumnType fromType, Object fromVal, ColumnType toType, Object toVal) {
    executeAlterType(fromType, fromVal, toType, toVal, true);
  }

  private void executeAlterType(
      ColumnType fromType, Object fromVal, ColumnType toType, Object toVal, boolean roundtrip) {
    String tableName = "TEST_ALTER_" + fromType.toString() + "_TO_" + toType.toString();
    schema.create(
        new TableMetadata(tableName)
            .add(Column.column("id").setPkey(), Column.column("col1").setType(fromType)));
    schema.getTable(tableName).insert(new Row().set("id", "test1").set("col1", fromVal));
    schema.getTable(tableName).getMetadata().alterColumn(new Column("col1").setType(toType));

    if (toVal instanceof Object[]) {
      assertArrayEquals(
          (Object[]) toVal,
          (Object[])
              schema.getTable(tableName).retrieveRows().get(0).get("col1", toVal.getClass()));
    } else {
      TestCase.assertEquals(
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
        TestCase.assertEquals(
            fromVal,
            schema.getTable(tableName).retrieveRows().get(0).get("col1", fromVal.getClass()));
      }
    }
  }
}
