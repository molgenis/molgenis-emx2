package org.molgenis.sql.psql.test;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.*;
import org.molgenis.utils.StopWatch;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static junit.framework.TestCase.fail;
import static org.molgenis.Column.Type.*;

public class TestSimpleTypes {

  private static Database db;

  @BeforeClass
  public static void setUp() throws MolgenisException, SQLException {
    db = DatabaseFactory.getDatabase();
  }

  @Test
  public void testTypes() throws MolgenisException {

    StopWatch.print("testTypes started");

    Schema s = db.createSchema("testTypes");

    // generate TypeTest table, with columns for each type
    Table t = s.createTable("TypeTest");
    Column.Type[] types =
        new Column.Type[] {UUID, STRING, ENUM, BOOL, INT, DECIMAL, TEXT, DATE, DATETIME};
    for (Column.Type type : types) {
      if (REF.equals(type)) {
        Column c =
            t.addRef("Test_" + type.toString().toLowerCase() + "_nillable", t.getName())
                .setNullable(true);
        // DatabaseFactory.checkColumnExists(c);
      } else {
        Column c = t.addColumn("Test_" + type.toString().toLowerCase(), type);
        Column c2 =
            t.addColumn("Test_" + type.toString().toLowerCase() + "_nillable", type)
                .setNullable(true);
      }
    }

    // retrieve this table from metadataa
    String TYPE_TEST = "TypeTest";
    Table t2 = s.getTable("TypeTest");

    StopWatch.print("created TypeTest table");

    // check nullable ok
    org.molgenis.Row row = new Row();
    row.setUuid("Test_uuid", java.util.UUID.randomUUID());
    row.setString("Test_string", "test");
    row.setEnum("Test_enum", "test");
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
    row.setEnum("Test_enum_nillable", "test");

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
    List<org.molgenis.Row> result = s.getTable("TypeTest").retrieve();
    for (org.molgenis.Row res : result) {
      res.setMolgenisid(java.util.UUID.randomUUID());
      assert (res.getDate("Test_date") instanceof LocalDate);
      assert (res.getDateTime("Test_datetime") instanceof LocalDateTime);
      assert (res.getString("Test_string") instanceof String);
      assert (res.getInt("Test_int") instanceof Integer);
      assert (res.getDecimal("Test_decimal") instanceof Double);
      assert (res.getText("Test_text") instanceof String);
      assert (res.getBool("Test_bool") instanceof Boolean);
      assert (res.getUuid("Test_uuid") instanceof java.util.UUID);

      t2.insert(res);
    }

    StopWatch.print("checked getters");

    StopWatch.print("complete", s.getTable("TypeTest").retrieve().size());
  }
}
