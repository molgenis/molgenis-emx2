package org.molgenis.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.*;
import org.molgenis.data.Database;
import org.molgenis.data.Row;
import org.molgenis.data.Table;
import org.molgenis.metadata.Type;
import org.molgenis.data.Schema;

import java.io.Serializable;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.molgenis.metadata.Type.*;

public class TestSimpleDatatypes {

  static Database db;

  @BeforeClass
  public static void setup() throws MolgenisException {
    db = DatabaseFactory.getTestDatabase("molgenis", "molgenis");
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

  private void executeTest(Type type, Serializable[] values) throws MolgenisException {

    Schema schema = db.createSchema("TestSimpleDatatypes" + type.toString().toUpperCase());

    Table aTable = schema.createTableIfNotExists("A");
    String aKey = type + "Key";
    String aColumn = type + "Col";
    aTable.getMetadata().addColumn(aKey, type).addColumn(aColumn, type).setPrimaryKey(aKey);

    Row aRow = new Row().set(aKey, values[0]).set(aColumn, values[0]);
    Row aRow2 = new Row().set(aKey, values[1]).set(aColumn, values[1]);
    aTable.insert(aRow, aRow2);

    // and update
    aRow.set(aColumn, values[2]);
    aTable.update(aRow);

    // check query
    List<Row> result = aTable.query().where(aColumn).eq(values[0]).retrieve();
    assertEquals(0, result.size());

    result = aTable.query().where(aColumn).eq(values[2]).retrieve();
    assertEquals(1, result.size());
    for (Row r : result) {
      System.out.println(r);
    }

    // deletel
    aTable.delete(aRow, aRow2);
    assertEquals(0, aTable.retrieve().size());
  }
}
