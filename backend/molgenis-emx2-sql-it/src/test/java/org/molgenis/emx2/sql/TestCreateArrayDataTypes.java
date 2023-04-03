package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.TableMetadata.table;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

public class TestCreateArrayDataTypes {

  static Database database;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testUUIDArray() {
    executeTest(
        UUID_ARRAY,
        new java.util.UUID[] {
          java.util.UUID.fromString("f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce4"),
          java.util.UUID.fromString("f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce5"),
          java.util.UUID.fromString("f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce6")
        });
  }

  @Test
  public void testStringArray() {
    executeTest(STRING_ARRAY, new String[] {"aap", "noot", "mies"});
  }

  @Test
  public void testIntArray() {
    executeTest(INT_ARRAY, new Integer[] {5, 6});
  }

  @Test
  public void testLongArray() {
    executeTest(LONG_ARRAY, new Long[] {5l, 6l});
  }

  @Test
  public void testDateArray() {
    executeTest(DATE_ARRAY, new String[] {"2013-01-01", "2013-01-02", "2013-01-03"});
  }

  @Test
  public void xtestDateTimeArray() {
    executeTest(
        DATETIME_ARRAY,
        new String[] {"2013-01-01T18:00:00.0", "2013-01-01T18:00:01.0", "2013-01-01T18:00:02.0"});
  }

  @Test
  public void testDecimalArray() {
    executeTest(DECIMAL_ARRAY, new Double[] {5.0, 6.0, 7.0});
  }

  @Test
  public void testTextArray() {
    executeTest(
        TEXT_ARRAY,
        new String[] {
          "This is a hello world", "This is a hello back to you", "This is a hello some more"
        });
  }

  @Test
  public void testJSON() {
    executeTest(
        JSONB_ARRAY,
        new String[] {"{\"key\":\"value1\"}", "{\"key\":\"value2\"}", "{\"key\":\"value3\"}"});
  }

  //  @Test
  //  public void testBool()  {
  //    executeTest(BOOL_ARRAY, new Boolean[] {null, true, false});
  //  }

  private void executeTest(ColumnType columnType, Serializable[] values) {

    Schema schema =
        database.dropCreateSchema("TestCreateArrayDataTypes" + columnType.toString().toUpperCase());

    String aFieldName = columnType + " Col";
    Table tableA =
        schema.create(table("A", column("id").setPkey(), column(aFieldName).setType(columnType)));

    Row aRow = new Row().set("id", "one").set(aFieldName, Arrays.copyOfRange(values, 1, 3));
    tableA.insert(aRow);

    // and update
    aRow.set(aFieldName, Arrays.copyOfRange(values, 0, 2));
    tableA.update(aRow);

    // check query
    List<Row> result = tableA.query().where(f(aFieldName, EQUALS, values[0])).retrieveRows();
    assertEquals(1, result.size());
    for (Row r : result) {
      if (DATETIME_ARRAY.equals(columnType)) {
        // TODO fix test
      } else {
        assertEquals(
            Arrays.copyOfRange(values, 0, 2)[0].toString(),
            ((Object[]) r.get(aFieldName, columnType))[0].toString());
        assertEquals(
            Arrays.copyOfRange(values, 0, 2)[1].toString(),
            ((Object[]) r.get(aFieldName, columnType))[1].toString());
      }
    }

    // delete of referenced A should fail
    tableA.delete(aRow);
  }
}
