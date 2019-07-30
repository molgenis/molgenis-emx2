package org.molgenis.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.*;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.molgenis.Type.*;

public class TestSimpleDatatypes2 {

  static Database db;

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

  private void executeTest(Type type, Object[] values) throws MolgenisException {

    Schema s = db.createSchema("TestSimpleDatatypes" + type.toString().toUpperCase());

    Table a = s.createTable("A");
    String aFieldName = type + "Col";
    a.addColumn(aFieldName, type);

    Row aRow = new Row().set(aFieldName, values[0]);
    Row bRow = new Row().set(aFieldName, values[1]);
    a.insert(aRow, bRow);

    // and update
    aRow.set(aFieldName, values[2]);
    a.update(aRow);

    // check query
    List<Row> result = a.query().where(aFieldName).eq(values[0]).retrieve();
    assertEquals(0, result.size());

    result = a.query().where(aFieldName).eq(values[2]).retrieve();
    assertEquals(1, result.size());
    for (Row r : result) {
      System.out.println(r);
    }

    // deletel
    a.delete(aRow, bRow);
    assertEquals(0, a.retrieve().size());
  }
}
