package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.Type;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.utils.MolgenisException;

import java.util.Arrays;

import static junit.framework.TestCase.fail;

public class CanCreateForeignKeysArrays {
  private static Database db;

  @BeforeClass
  public static void setup() throws MolgenisException {
    db = DatabaseFactory.getTestDatabase("molgenis", "molgenis");
  }

  @Test
  public void testUUID() throws MolgenisException {
    executeTest(
        Type.UUID,
        new java.util.UUID[] {
          java.util.UUID.fromString("f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce4"),
          java.util.UUID.fromString("f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce5"),
          java.util.UUID.fromString("f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce6")
        });
  }

  @Test
  public void testStringRef() throws MolgenisException {
    executeTest(Type.STRING, new String[] {"aap", "noot", "mies"});
  }

  @Test
  public void testIntRef() throws MolgenisException {
    executeTest(Type.INT, new Integer[] {5, 6});
  }

  @Test
  public void testDateRef() throws MolgenisException {
    executeTest(Type.DATE, new String[] {"2013-01-01", "2013-01-02", "2013-01-03"});
  }

  @Test
  public void testDateTimeRef() throws MolgenisException {
    executeTest(
        Type.DATETIME,
        new String[] {"2013-01-01T18:00:00", "2013-01-01T18:00:01", "2013-01-01T18:00:02"});
  }

  @Test
  public void testDecimalRef() throws MolgenisException {
    executeTest(Type.DECIMAL, new Double[] {5.0, 6.0, 7.0});
  }

  @Test
  public void testTextRef() throws MolgenisException {
    executeTest(
        Type.TEXT,
        new String[] {
          "This is a hello world", "This is a hello back to you", "This is a hello some more"
        });
  }

  private void executeTest(Type type, Object[] testValues) throws MolgenisException {

    Schema schema = db.createSchema("TestRefArray" + type.toString().toUpperCase());

    Table aTable = schema.createTableIfNotExists("A");
    String aKey = "A" + type + "Key";
    aTable.getMetadata().addColumn(aKey, type).addUnique(aKey);

    Row aRow = new Row().set(aKey, testValues[0]);
    Row aRow2 = new Row().set(aKey, testValues[1]);
    aTable.insert(aRow, aRow2);

    Table bTable = schema.createTableIfNotExists("B");
    String refToA = type + "RefToA";
    bTable.getMetadata().addRefArray(refToA, "A", aKey);

    // error on insert of faulty fkey
    Row bErrorRow = new Row().set(refToA, Arrays.copyOfRange(testValues, 1, 3));
    try {
      bTable.insert(bErrorRow);
      fail("insert should fail because value is missing");
    } catch (Exception e) {
      System.out.println("insert exception correct: \n" + e.getMessage());
    }

    // okay
    Row bRow = new Row().set(refToA, Arrays.copyOfRange(testValues, 0, 2));
    bTable.insert(bRow);

    // delete of A should fail
    try {
      aTable.delete(aRow);
      fail("delete should fail");
    } catch (Exception e) {
      System.out.println("delete exception correct: \n" + e.getMessage());
    }

    // should be okay
    bTable.delete(bRow);
    aTable.delete(aRow);
  }
}
