package org.molgenis.emx2.sql;

import static junit.framework.TestCase.fail;
import static org.molgenis.emx2.ColumnType.*;

import junit.framework.TestCase;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;

public class TestCreateForeignKeysCascadeDelete {

  static Database db;

  @BeforeClass
  public static void setup() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testInt() {
    executeTest(ColumnType.INT, 5, 6);
  }

  @Test
  public void testString() {
    executeTest(ColumnType.STRING, "test", "DependencyOrderOutsideTransactionFails");
  }

  @Test
  public void testDate() {
    executeTest(ColumnType.DATE, "2013-01-01", "2013-01-02");
  }

  @Test
  public void testDateTime() {
    executeTest(ColumnType.DATETIME, "2013-01-01T18:00:00", "2013-01-01T18:00:01");
  }

  @Test
  public void testDecimal() {
    executeTest(ColumnType.DECIMAL, 5.0, 6.0);
  }

  @Test
  public void testText() {
    executeTest(ColumnType.TEXT, "This is a hello world", "This is a hello back to you");
  }

  @Test
  public void testUUID() {
    executeTest(
        ColumnType.UUID,
        "f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce4",
        "f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce5");
  }

  private void executeTest(ColumnType columnType, Object insertValue, Object updateValue) {

    Schema schema =
        db.dropCreateSchema("TestCreateForeignKeysCascade" + columnType.toString().toUpperCase());

    String fieldName = "AKeyOf" + columnType;
    Table aTable =
        schema.create(
            TableMetadata.table("A").add(Column.column(fieldName).setType(columnType).setPkey()));
    Row aRow = new Row().set(fieldName, insertValue);
    aTable.insert(aRow);

    String refFromBToA = "RefToAKeyOf" + columnType;
    Table bTable =
        schema.create(
            TableMetadata.table("B")
                .add(Column.column("ID").setType(ColumnType.INT).setPkey())
                // only difference with other test
                .add(
                    Column.column(refFromBToA)
                        .setType(ColumnType.REF)
                        .setRefTable("A")
                        .setCascadeDelete(true)
                        .setPkey()));
    Row bRow = new Row().setInt("ID", 2).set(refFromBToA, insertValue);
    bTable.insert(bRow);

    // insert to non-existing value should fail
    Row bErrorRow = new Row().setInt("ID", 3).set(refFromBToA, updateValue);
    try {
      bTable.insert(bErrorRow);
      fail("insert should fail because value is missing");
    } catch (Exception e) {
      System.out.println("insert exception correct: \n" + e);
    }

    // and update, should be cascading :-)
    // aTable.update(aRow.set(fieldName, updateValue));

    // delete of A should cascade
    try {
      aTable.delete(aRow);
      TestCase.assertEquals(0, bTable.query().retrieveRows().size());
    } catch (Exception e) {
      fail("delete should cascade because cascadeDelete was set");
    }
  }
}
