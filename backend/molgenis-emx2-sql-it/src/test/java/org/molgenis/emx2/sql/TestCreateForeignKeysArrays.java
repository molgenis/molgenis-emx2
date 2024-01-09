package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.INT;
import static org.molgenis.emx2.ColumnType.REF_ARRAY;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

public class TestCreateForeignKeysArrays {
  private static Database db;

  @BeforeAll
  public static void setup() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testUUID() {
    executeTest(
        ColumnType.UUID,
        new java.util.UUID[] {
          java.util.UUID.fromString("f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce4"),
          java.util.UUID.fromString("f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce5"),
          java.util.UUID.fromString("f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce6")
        });
  }

  @Test
  public void testStringRef() {
    executeTest(ColumnType.STRING, new String[] {"aap", "noot", "mies"});
  }

  @Test
  public void testIntRef() {
    executeTest(INT, new Integer[] {5, 6, 7});
  }

  @Test
  public void testDateRef() {
    executeTest(ColumnType.DATE, new String[] {"2013-01-01", "2013-01-02", "2013-01-03"});
  }

  @Test
  public void testDateTimeRef() {
    executeTest(
        ColumnType.DATETIME,
        new String[] {"2013-01-01T18:00:00", "2013-01-01T18:00:01", "2013-01-01T18:00:02"});
  }

  @Test
  public void testDecimalRef() {
    executeTest(ColumnType.DECIMAL, new Double[] {5.0, 6.0, 7.0});
  }

  @Test
  public void testTextRef() {
    executeTest(
        ColumnType.TEXT,
        new String[] {
          "This is a hello world", "This is a hello back to you", "This is a hello some more"
        });
  }

  @Test
  public void testJSONRef() {
    executeTest(
        ColumnType.JSONB,
        new String[] {"{\"key\": \"value1\"}", "{\"key\": \"value2\"}", "{\"key\": \"value3\"}"});
  }

  private void executeTest(ColumnType columnType, Object[] testValues) {

    Schema schema = db.dropCreateSchema("TestRefArray" + columnType.toString().toUpperCase());

    String aKey = "A" + columnType + "Key";
    Table aTable = schema.create(table("A").add(column(aKey).setType(columnType).setPkey()));

    Row aRow = new Row().set(aKey, testValues[0]);
    Row aRow2 = new Row().set(aKey, testValues[1]);
    aTable.insert(aRow, aRow2);

    String refToA = columnType + "RefToA";
    Table bTable =
        schema.create(
            table("B")
                .add(column("id").setPkey())
                .add(column(refToA).setType(REF_ARRAY).setRefTable("A").setRequired(true))
                .add(column(refToA + "Nullable").setType(REF_ARRAY).setRefTable("A")));

    // error on insert of faulty fkey
    Row bErrorRow = new Row().set("id", 1).set(refToA, Arrays.copyOfRange(testValues, 1, 3));
    try {
      bTable.insert(bErrorRow);
      fail("insert should fail because value is missing");
    } catch (Exception e) {
      System.out.println(
          "insert exception correct because value " + testValues[2] + " is missing: \n" + e);
    }

    // error on insert of faulty null fkey
    bErrorRow = new Row().set("id", 1).set(refToA, null);
    try {
      bTable.insert(bErrorRow);
      fail("insert should fail because required value is null");
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("required"));
      System.out.println(
          "insert exception correct because value " + testValues[2] + " is missing: \n" + e);
    }

    // error on insert of faulty empty array fkey
    bErrorRow = new Row().set("id", 1).set(refToA, new Object[0]);
    try {
      bTable.insert(bErrorRow);
      fail("insert should fail because required value is empty array");
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("required"));
      System.out.println(
          "insert exception correct because value " + testValues[2] + " is missing: \n" + e);
    }

    // okay
    Row bRow = new Row().set("id", 1).set(refToA, Arrays.copyOfRange(testValues, 0, 2));
    bTable.insert(bRow);

    // delete of A should fail
    try {
      aTable.delete(aRow);
      fail("delete should fail");
    } catch (Exception e) {
      System.out.println("delete exception correct because of " + testValues[0] + ": \n" + e);
    }

    // should be okay
    bTable.delete(bRow);
    aTable.delete(aRow);
  }
}
