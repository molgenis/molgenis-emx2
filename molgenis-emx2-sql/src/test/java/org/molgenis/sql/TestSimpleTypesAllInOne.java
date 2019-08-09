package org.molgenis.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.*;
import org.molgenis.emx2.examples.synthetic.SimpleTypeTestExample;
import org.molgenis.utils.StopWatch;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static junit.framework.TestCase.fail;
import static org.molgenis.emx2.examples.synthetic.SimpleTypeTestExample.TYPE_TEST;

public class TestSimpleTypesAllInOne {

  private static Database db;

  @BeforeClass
  public static void setUp() throws MolgenisException, SQLException {
    db = DatabaseFactory.getTestDatabase("molgenis", "molgenis");
  }

  @Test
  public void testTypes() throws MolgenisException {

    StopWatch.print("testTypes started");

    String SCHEMA_NAME = "testTypes";
    Schema schema = db.createSchema(SCHEMA_NAME);

    // generate TypeTest table, with columns for each type

    SimpleTypeTestExample.createSimpleTypeTest(schema);

    // retrieve this table from metadataa

    db.clearCache();

    Table t2 = db.getSchema(SCHEMA_NAME).getTable(TYPE_TEST);

    StopWatch.print("created TypeTest table");

    // check nullable ok
    Row row = new Row();
    row.setUuid("Test_uuid", java.util.UUID.randomUUID());
    row.setString("Test_string", "test");
    row.setBool("Test_bool", true);
    row.setInt("Test_int", 1);
    row.setDecimal("Test_decimal", 1.1);
    row.setText("Test_text", "testtext");
    row.setDate("Test_date", LocalDate.of(2018, 12, 13));
    row.setDateTime("Test_datetime", LocalDateTime.of(2018, 12, 13, 12, 40));
    t2.insert(row);

    // check not null expects exception
    row = new Row();
    row.setUuid("Test_uuid_nillable", java.util.UUID.randomUUID());
    row.setString("Test_string_nillable", "test");
    // row.setEnum("Test_enum_nillable", "test");

    row.setBool("Test_bool_nillable", true);
    row.setInt("Test_int_nillable", 1);
    row.setDecimal("Test_decimal_nillable", 1.1);
    row.setText("Test_text_nillable", "testtext");
    row.setDate("Test_date_nillable", LocalDate.of(2018, 12, 13));
    row.setDateTime("Test_datetime_nillable", LocalDateTime.of(2018, 12, 13, 12, 40));
    try {
      t2.insert(row);
      fail(); // should not reach this one
    } catch (MolgenisException e) {

    }

    StopWatch.print("inserted rows");

    // check queryOld and test getters
    List<Row> result = schema.getTable("TypeTest").retrieve();
    for (Row res : result) {
      res.setMolgenisid(java.util.UUID.randomUUID());
      assert (res.getDate("Test_date") instanceof LocalDate);
      assert (res.getDateTime("Test_datetime") instanceof LocalDateTime);
      assert (res.getString("Test_string") instanceof String);
      assert (res.getInteger("Test_int") instanceof Integer);
      assert (res.getDecimal("Test_decimal") instanceof Double);
      assert (res.getText("Test_text") instanceof String);
      assert (res.getBoolean("Test_bool") instanceof Boolean);
      assert (res.getUuid("Test_uuid") instanceof java.util.UUID);

      t2.insert(res);
    }

    StopWatch.print("checked getters");

    StopWatch.print("complete", schema.getTable("TypeTest").retrieve().size());
  }
}
