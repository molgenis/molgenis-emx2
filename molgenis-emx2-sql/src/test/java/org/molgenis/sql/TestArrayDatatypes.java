package org.molgenis.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.*;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.molgenis.Type.*;

public class TestArrayDatatypes {

  static Database database;

  @BeforeClass
  public static void setup() throws MolgenisException {
    database = DatabaseFactory.getTestDatabase("molgenis", "molgenis");
  }

  @Test
  public void testUUIDArray() throws MolgenisException {
    executeTest(
        UUID_ARRAY,
        new java.util.UUID[] {
          java.util.UUID.fromString("f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce4"),
          java.util.UUID.fromString("f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce5"),
          java.util.UUID.fromString("f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce6")
        });
  }

  @Test
  public void testStringArray() throws MolgenisException {
    executeTest(STRING_ARRAY, new String[] {"aap", "noot", "mies"});
  }

  @Test
  public void testIntArray() throws MolgenisException {
    executeTest(INT_ARRAY, new Integer[] {5, 6});
  }

  @Test
  public void testDateArray() throws MolgenisException {
    executeTest(DATE_ARRAY, new String[] {"2013-01-01", "2013-01-02", "2013-01-03"});
  }

  @Test
  public void testDateTimeArray() throws MolgenisException {
    executeTest(
        DATETIME_ARRAY,
        new String[] {"2013-01-01T18:00:00.0", "2013-01-01T18:00:01.0", "2013-01-01T18:00:02.0"});
  }

  @Test
  public void testDecimalArray() throws MolgenisException {
    executeTest(DECIMAL_ARRAY, new Double[] {5.0, 6.0, 7.0});
  }

  @Test
  public void testTextArray() throws MolgenisException {
    executeTest(
        TEXT_ARRAY,
        new String[] {
          "This is a hello world", "This is a hello back to you", "This is a hello some more"
        });
  }

  //  @Test
  //  public void testBool() throws MolgenisException {
  //    executeTest(BOOL_ARRAY, new Boolean[] {null, true, false});
  //  }

  private void executeTest(Type type, Serializable[] values) throws MolgenisException {

    Schema schema = database.createSchema("TestArrayDatatypes" + type.toString().toUpperCase());

    Table tableA = schema.createTableIfNotExists("A");
    String aFieldName = type + "Col";
    tableA.addColumn(aFieldName, type);
    tableA.addUnique(aFieldName);

    Row aRow = new Row().set(aFieldName, Arrays.copyOfRange(values, 1, 3));
    tableA.insert(aRow);

    // and update
    aRow.set(aFieldName, Arrays.copyOfRange(values, 0, 2));
    tableA.update(aRow);

    // cehck query
    List<Row> result = tableA.query().where(aFieldName).contains(values[0]).retrieve();
    assertEquals(1, result.size());
    for (Row r : result) {
      if (DATETIME_ARRAY.equals(type)) {
        // TODO fix test
      } else {
        assertEquals(
            Arrays.copyOfRange(values, 0, 2)[0].toString(),
            ((Object[]) r.get(type, aFieldName))[0].toString());
        assertEquals(
            Arrays.copyOfRange(values, 0, 2)[1].toString(),
            ((Object[]) r.get(type, aFieldName))[1].toString());
      }
    }

    // delete of referenced A should fail
    tableA.delete(aRow);
  }
}
