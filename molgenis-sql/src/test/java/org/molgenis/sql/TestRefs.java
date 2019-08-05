package org.molgenis.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.*;

import static junit.framework.TestCase.fail;
import static org.molgenis.Type.*;

public class TestRefs {

  static Database db;

  @BeforeClass
  public static void setup() throws MolgenisException {
    db = DatabaseFactory.getDatabase("molgenis", "molgenis");
  }

  @Test
  public void testInt() throws MolgenisException {
    executeTest(INT, 5, 6);
  }

  @Test
  public void testString() throws MolgenisException {
    executeTest(STRING, "test", "DependencyOrderOutsideTransactionFails");
  }

  @Test
  public void testDate() throws MolgenisException {
    executeTest(DATE, "2013-01-01", "2013-01-02");
  }

  @Test
  public void testDateTime() throws MolgenisException {
    executeTest(DATETIME, "2013-01-01T18:00:00", "2013-01-01T18:00:01");
  }

  @Test
  public void testDecimal() throws MolgenisException {
    executeTest(DECIMAL, 5.0, 6.0);
  }

  @Test
  public void testText() throws MolgenisException {
    executeTest(TEXT, "This is a hello world", "This is a hello back to you");
  }

  @Test
  public void testUUID() throws MolgenisException {
    executeTest(
        UUID, "f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce4", "f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce5");
  }

  private void executeTest(Type type, Object insertValue, Object updateValue)
      throws MolgenisException {

    Schema schema = db.createSchema("TestRefs" + type.toString().toUpperCase());

    Table aTable = schema.createTable("A");
    String fieldName = "AKeyOf" + type;
    aTable.addColumn(fieldName, type);
    aTable.addUnique(fieldName);
    Row aRow = new Row().set(fieldName, insertValue);
    aTable.insert(aRow);

    Table bTable = schema.createTable("B");
    String refFromBToA = "RefToAKeyOf" + type;
    bTable.addRef(refFromBToA, "A", fieldName);
    Row bRow = new Row().set(refFromBToA, insertValue);
    bTable.insert(bRow);

    // insert to non-existing value should fail
    Row bErrorRow = new Row().set(refFromBToA, updateValue);
    try {
      bTable.insert(bErrorRow);
      fail("insert should fail because value is missing");
    } catch (Exception e) {
      System.out.println("delete exception correct: \n" + e.getMessage());
    }

    // and update, should be cascading :-)
    aTable.update(aRow.set(fieldName, updateValue));

    // delete of A should fail
    try {
      aTable.delete(aRow);
      fail("delete should fail");
    } catch (Exception e) {
      System.out.println("insert exception correct: \n" + e.getMessage());
    }

    bTable.delete(bRow);
    aTable.delete(aRow);
  }
}
