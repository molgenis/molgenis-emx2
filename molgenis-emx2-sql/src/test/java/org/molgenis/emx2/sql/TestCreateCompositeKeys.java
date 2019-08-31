package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.Type;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.utils.MolgenisException;

import java.io.Serializable;

import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.Type.*;

public class TestCreateCompositeKeys {
  static Database database;

  @BeforeClass
  public static void setup() throws MolgenisException {
    database = DatabaseFactory.getTestDatabase("molgenis", "molgenis");
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
    executeTest(INT, new Integer[] {5, 6, 7});
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

  public void executeTest(Type type, Serializable[] data) throws MolgenisException {
    Schema schema = database.createSchema("TestCreateCompositeKeys" + type.toString());

    Table aTable = schema.createTableIfNotExists("CompositeKeyTable");
    aTable
        .getMetadata()
        .addColumn("col1", type)
        .addColumn("col2", type)
        .addColumn("col3", type)
        .setPrimaryKey("col1", "col2");

    Row aRow = new Row().set("col1", data[0]).set("col2", data[0]).set("col3", data[0]);
    Row aRow2 = new Row().set("col1", data[0]).set("col2", data[1]).set("col3", data[0]);
    Row aRow3 = new Row().set("col1", data[1]).set("col2", data[1]).set("col3", data[0]);

    aTable.insert(aRow, aRow2, aRow3);

    aRow2.set("col3", data[1]);
    aTable.update(aRow2);

    assertEquals(1, aTable.query().where("col3").eq(data[1]).retrieve().size());

    aTable.delete(aRow);

    assertEquals(2, aTable.retrieve().size());

    aTable.delete(aRow2, aRow3);

    assertEquals(0, aTable.retrieve().size());
  }
}
