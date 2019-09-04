package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.Type;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.utils.MolgenisException;

import static junit.framework.TestCase.fail;
import static org.molgenis.emx2.Type.*;

public class TestCreateCompositeRef {

  static Database db;

  @BeforeClass
  public static void setup() throws MolgenisException {
    db = DatabaseFactory.getTestDatabase("molgenis", "molgenis");
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

    Schema schema = db.createSchema("TestCreateCompositeRef" + type.toString().toUpperCase());

    Table aTable = schema.createTableIfNotExists("A");
    String uniqueColumn1 = "AUnique" + type;
    String uniqueColumn2 = "AUnique" + type + "2";

    aTable
        .getMetadata()
        .addColumn("ID", Type.INT)
        .primaryKey()
        .addColumn(uniqueColumn1, type)
        .addColumn(uniqueColumn2, type)
        .addUnique(uniqueColumn1, uniqueColumn2);

    Row aRow =
        new Row().setInt("ID", 1).set(uniqueColumn1, insertValue).set(uniqueColumn2, insertValue);
    aTable.insert(aRow);

    Table bTable = schema.createTableIfNotExists("B");
    String refFromBToA1 = "RefToAKeyOf" + type;
    String refFromBToA2 = "RefToAKeyOf" + type + "2";

    bTable
        .getMetadata()
        .addColumn("ID", Type.INT)
        .primaryKey()
        .addRefMultiple(refFromBToA1, refFromBToA2)
        .to("A", uniqueColumn1, uniqueColumn2);
    Row bRow =
        new Row().setInt("ID", 1).set(refFromBToA1, insertValue).set(refFromBToA2, insertValue);
    bTable.insert(bRow);

    // insert to non-existing value should fail
    Row bErrorRow =
        new Row().setInt("ID", 2).set(refFromBToA1, updateValue).set(refFromBToA2, updateValue);
    try {
      bTable.insert(bErrorRow);
      fail("insert should fail because value is missing");
    } catch (Exception e) {
      System.out.println("delete exception correct: \n" + e.getMessage());
    }

    // and update, should be cascading :-)
    aTable.update(aRow.set(uniqueColumn1, updateValue));

    // delete of A should fail
    try {
      aTable.delete(aRow);
      fail("delete should fail because a bRow is still referencing to it");
    } catch (Exception e) {
      System.out.println("delete exception correct: \n" + e.getMessage());
    }

    bTable.delete(bRow);
    aTable.delete(aRow);
  }
}
