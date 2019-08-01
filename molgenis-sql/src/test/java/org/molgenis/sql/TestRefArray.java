package org.molgenis.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.*;

import java.util.Arrays;
import java.util.UUID;

import static junit.framework.TestCase.fail;
import static org.molgenis.Row.MOLGENISID;
import static org.molgenis.Type.*;

public class TestRefArray {
  private static Database db;

  @BeforeClass
  public static void setup() throws MolgenisException {
    db = DatabaseFactory.getDatabase("molgenis", "molgenis");
  }

  @Test
  public void testUUID() throws MolgenisException {
    executeTest(
        UUID,
        new java.util.UUID[] {
          java.util.UUID.fromString("f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce4"),
          java.util.UUID.fromString("f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce5"),
          java.util.UUID.fromString("f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce6")
        });
  }

  @Test
  public void testString() throws MolgenisException {
    executeTest(STRING, new String[] {"aap", "noot", "mies"});
  }

  @Test
  public void testInt() throws MolgenisException {
    executeTest(INT, new Integer[] {5, 6});
  }

  @Test
  public void testDate() throws MolgenisException {
    executeTest(DATE, new String[] {"2013-01-01", "2013-01-02", "2013-01-03"});
  }

  @Test
  public void testDateTime() throws MolgenisException {
    executeTest(
        DATETIME,
        new String[] {"2013-01-01T18:00:00", "2013-01-01T18:00:01", "2013-01-01T18:00:02"});
  }

  @Test
  public void testDecimal() throws MolgenisException {
    executeTest(DECIMAL, new Double[] {5.0, 6.0, 7.0});
  }

  @Test
  public void testText() throws MolgenisException {
    executeTest(
        TEXT,
        new String[] {
          "This is a hello world", "This is a hello back to you", "This is a hello some more"
        });
  }

  private void executeTest(Type type, Object[] values) throws MolgenisException {

    Schema s = db.createSchema("TestRefArray" + type.toString().toUpperCase());

    Table a = s.createTable("A");
    String fieldName = type + "Col";
    a.addColumn(fieldName, type);
    a.addUnique(fieldName);

    Row aRow = new Row().set(fieldName, values[0]);
    Row aRow2 = new Row().set(fieldName, values[1]);
    a.insert(aRow, aRow2);

    Table b = s.createTable("B");
    String refName = type + "RefArray";
    b.addRefArray(refName, "A", fieldName);

    // error on insert of faulty fkey
    Row bErrorRow = new Row().set(refName, Arrays.copyOfRange(values, 1, 3));
    try {
      b.insert(bErrorRow);
      fail("insert should fail because value is missing");
    } catch (Exception e) {
      System.out.println("insert exception correct: \n" + e.getMessage());
    }

    // okay
    Row bRow = new Row().set(refName, Arrays.copyOfRange(values, 0, 2));
    b.insert(bRow);

    // delete of A should fail
    try {
      a.delete(aRow);
      fail("delete should fail");
    } catch (Exception e) {
      System.out.println("delete exception correct: \n" + e.getMessage());
    }

    // should be okay
    b.delete(bRow);
    a.delete(aRow);
  }

  @Test
  public void test1() throws MolgenisException {

    Schema s = db.createSchema("TestRefArray");

    Table aTable = s.createTable("A");

    Table bTable = s.createTable("B");
    bTable.addRefArray("refToA", "A", MOLGENISID);

    Row aRow = new Row();
    Row aRow2 = new Row();
    Row aRow3 = new Row();
    aTable.insert(aRow, aRow2, aRow3);

    // okay
    Row bRow = new Row().set("refToA", new UUID[] {aRow.getMolgenisid(), aRow3.getMolgenisid()});
    bTable.insert(bRow);

    // should fail
    try {
      Row notInA = new Row();
      Row bRow2 =
          new Row()
              .set(
                  "refToA",
                  new UUID[] {aRow.getMolgenisid(), aRow3.getMolgenisid(), notInA.getMolgenisid()});
      bTable.insert(bRow2);
      fail("should fail because of missing foreign key");
    } catch (Exception e) {
      System.out.println("error correctly with message:\n " + e.getMessage());
    }

    // should fail
    try {
      aTable.delete(aRow);
      fail("should fail because aRow still has foreign key references to it");
    } catch (Exception e) {
      System.out.println("error correctly with message:\n " + e.getMessage());
    }
  }
}
