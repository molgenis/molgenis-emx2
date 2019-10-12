package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.ColumnType;

import java.io.Serializable;

import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.Operator.EQUALS;

public class TestCreateCompositeKeys {
  static Database database;

  @BeforeClass
  public static void setup() {
    database = DatabaseFactory.getTestDatabase("molgenis", "molgenis");
  }

  @Test
  public void testUUID() {
    executeTest(
        UUID,
        new java.util.UUID[] {
          java.util.UUID.fromString("f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce4"),
          java.util.UUID.fromString("f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce5"),
          java.util.UUID.fromString("f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce6")
        });
  }

  @Test
  public void testString() {
    executeTest(STRING, new String[] {"aap", "noot", "mies"});
  }

  @Test
  public void testInt() {
    executeTest(INT, new Integer[] {5, 6, 7});
  }

  @Test
  public void testDate() {
    executeTest(DATE, new String[] {"2013-01-01", "2013-01-02", "2013-01-03"});
  }

  @Test
  public void testDateTime() {
    executeTest(
        DATETIME,
        new String[] {"2013-01-01T18:00:00", "2013-01-01T18:00:01", "2013-01-01T18:00:02"});
  }

  @Test
  public void testDecimal() {
    executeTest(DECIMAL, new Double[] {5.0, 6.0, 7.0});
  }

  @Test
  public void testText() {
    executeTest(
        TEXT,
        new String[] {
          "This is a hello world", "This is a hello back to you", "This is a hello some more"
        });
  }

  public void executeTest(ColumnType columnType, Serializable[] data) {
    Schema schema = database.createSchema("TestCreateCompositeKeys" + columnType.toString());

    Table aTable = schema.createTableIfNotExists("CompositeKeyTable");
    aTable
        .getMetadata()
        .addColumn("col1", columnType)
        .addColumn("col2", columnType)
        .addColumn("col3", columnType)
        .setPrimaryKey("col1", "col2");

    Row aRow = new Row().set("col1", data[0]).set("col2", data[0]).set("col3", data[0]);
    Row aRow2 = new Row().set("col1", data[0]).set("col2", data[1]).set("col3", data[0]);
    Row aRow3 = new Row().set("col1", data[1]).set("col2", data[1]).set("col3", data[0]);

    aTable.insert(aRow, aRow2, aRow3);

    aRow2.set("col3", data[1]);
    aTable.update(aRow2);

    assertEquals(1, aTable.query().where("col3", EQUALS, data[1]).retrieve().size());

    aTable.delete(aRow);

    assertEquals(2, aTable.retrieve().size());

    aTable.delete(aRow2, aRow3);

    assertEquals(0, aTable.retrieve().size());
  }
}
