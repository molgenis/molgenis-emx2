package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.TableMetadata.table;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.test.SimpleTypeTestExample;
import org.molgenis.emx2.utils.StopWatch;

public class TestCreateBasicDataColumnTypeColumns {

  static Database db;

  @BeforeAll
  public static void setup() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testTypes() {

    StopWatch.print("testTypes started");

    String SCHEMA_NAME = "testTypes";
    Schema schema = db.dropCreateSchema(SCHEMA_NAME);

    // generate TypeTest table, with columns for each type

    SimpleTypeTestExample.createSimpleTypeTest(schema.getMetadata());

    // retrieve this table from metadataa

    db.clearCache();

    Table t2 = db.getSchema(SCHEMA_NAME).getTable(SimpleTypeTestExample.TYPE_TEST);

    StopWatch.print("created TypeTest table");

    // check setNullable ok
    Row row = new Row();
    row.setString("id", "test1");
    row.setUuid("Test_uuid", java.util.UUID.randomUUID());
    row.setString("Test_string", "test");
    row.setBool("Test_bool", true);
    row.setInt("Test_int", 1);
    row.setLong("Test_long", 1l);
    row.setDecimal("Test_decimal", 1.1);
    row.setText("Test_text", "testtext");
    row.setDate("Test_date", LocalDate.of(2018, 12, 13));
    row.setDateTime("Test_datetime", LocalDateTime.of(2018, 12, 13, 12, 40));
    t2.insert(row);

    // check not null expects exception
    row = new Row();
    row.setString("id", "test2");
    row.setUuid("Test_uuid_nillable", java.util.UUID.randomUUID());
    row.setString("Test_string_nillable", "test");
    row.setBool("Test_bool_nillable", true);
    row.setInt("Test_int_nillable", 1);
    row.setDecimal("Test_decimal_nillable", 1.1);
    row.setText("Test_text_nillable", "testtext");
    row.setDate("Test_date_nillable", LocalDate.of(2018, 12, 13));
    row.setDateTime("Test_datetime_nillable", LocalDateTime.of(2018, 12, 13, 12, 40));
    try {
      // should fail on all non  nillable columns
      t2.insert(row);
      fail("Should not be able to insert null in not-null columns"); // should not reach this one
      // because all not null are null
    } catch (MolgenisException e) {

    }

    StopWatch.print("inserted rows");

    // check queryOld and test getters
    List<Row> result = schema.getTable("TypeTest").retrieveRows();
    for (Row res : result) {
      assert (res.getDate("Test_date") instanceof LocalDate);
      assert (res.getDateTime("Test_datetime") instanceof LocalDateTime);
      assert (res.getString("Test_string") instanceof String);
      assert (res.getInteger("Test_int") instanceof Integer);
      assert (res.getDecimal("Test_decimal") instanceof Double);
      assert (res.getText("Test_text") instanceof String);
      assert (res.getBoolean("Test_bool") instanceof Boolean);
      assert (res.getUuid("Test_uuid") instanceof java.util.UUID);
    }

    StopWatch.print("checked getters");

    StopWatch.print("complete", schema.getTable("TypeTest").retrieveRows().size());
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
  public void testEmail() {
    executeTest(EMAIL, new String[] {"aap@some.host", "noot@some.host", "mies@some.host"});
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

  @Test
  public void testJSON() {
    executeTest(
        JSONB,
        new String[] {"{\"key\":\"value1\"}", "{\"key\":\"value2\"}", "{\"key\":\"value3\"}"});
  }

  private void executeTest(ColumnType columnType, Serializable[] values) {

    Schema schema =
        db.dropCreateSchema(
            "TestCreateBasicDataColumnTypeColumns" + columnType.toString().toUpperCase());

    String aKey = columnType + "Key";
    String aColumn = columnType + "Col";
    Table aTable =
        schema.create(
            table("A")
                .add(column(aKey).setType(columnType).setPkey())
                .add(column(aColumn).setType(columnType)));

    Row aRow = new Row().set(aKey, values[0]).set(aColumn, values[0]);
    Row aRow2 = new Row().set(aKey, values[1]).set(aColumn, values[1]);
    aTable.insert(aRow, aRow2);

    // and update
    aRow.set(aColumn, values[2]);
    aTable.update(aRow);

    // check query
    List<Row> result = aTable.query().where(f(aColumn, EQUALS, values[0])).retrieveRows();
    assertEquals(0, result.size());

    result = aTable.query().where(f(aColumn, EQUALS, values[2])).retrieveRows();
    assertEquals(1, result.size());
    for (Row r : result) {
      System.out.println(r);
    }

    // delete
    aTable.delete(aRow, aRow2);
    assertEquals(0, aTable.retrieveRows().size());
  }
}
