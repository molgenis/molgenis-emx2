package org.molgenis.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.*;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.molgenis.Type.*;

public class TestArrayDatatypes {

  static Database db;

  @BeforeClass
  public static void setup() throws MolgenisException {
    db = DatabaseFactory.getDatabase("molgenis", "molgenis");
  }

  @Test
  public void testUUID() throws MolgenisException {
    executeTest(
        UUID_ARRAY,
        new java.util.UUID[] {
          java.util.UUID.fromString("f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce4"),
          java.util.UUID.fromString("f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce5"),
          java.util.UUID.fromString("f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce6")
        });
  }

  @Test
  public void testString() throws MolgenisException {
    executeTest(STRING_ARRAY, new String[] {"aap", "noot", "mies"});
  }

  @Test
  public void testInt() throws MolgenisException {
    executeTest(INT_ARRAY, new Integer[] {5, 6});
  }

  @Test
  public void testDate() throws MolgenisException {
    executeTest(DATE_ARRAY, new String[] {"2013-01-01", "2013-01-02", "2013-01-03"});
  }

  @Test
  public void testDateTime() throws MolgenisException {
    executeTest(
        DATETIME_ARRAY,
        new String[] {"2013-01-01T18:00:00", "2013-01-01T18:00:01", "2013-01-01T18:00:02"});
  }

  @Test
  public void testDecimal() throws MolgenisException {
    executeTest(DECIMAL_ARRAY, new Double[] {5.0, 6.0, 7.0});
  }

  @Test
  public void testText() throws MolgenisException {
    executeTest(
        TEXT_ARRAY,
        new String[] {
          "This is a hello world", "This is a hello back to you", "This is a hello some more"
        });
  }

  private void executeTest(Type type, Object[] values) throws MolgenisException {

    Schema s = db.createSchema("TestArrayDatatypes" + type.toString().toUpperCase());

    Table a = s.createTable("A");
    String aFieldName = type + "Col";
    a.addColumn(aFieldName, type);
    a.addUnique(aFieldName);

    Row aRow = new Row().set(aFieldName, Arrays.copyOfRange(values, 1, 3));
    a.insert(aRow);

    // and update
    aRow.set(aFieldName, Arrays.copyOfRange(values, 0, 2));
    a.update(aRow);

    // cehck query
    List<Row> result = a.query().where(aFieldName).any(values[0]).retrieve();
    assertEquals(1, result.size());
    for (Row r : result) {
      System.out.println(r);
    }

    // delete of referenced A should fail
    a.delete(aRow);
  }
}
