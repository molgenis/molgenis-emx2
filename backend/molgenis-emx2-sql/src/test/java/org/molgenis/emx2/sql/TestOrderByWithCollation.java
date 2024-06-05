package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

public class TestOrderByWithCollation {
  static Database database;
  static Schema schema;
  static final String VARS = "VARS";

  @BeforeAll
  public static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();

    // createColumn a schema to test with
    schema = database.dropCreateSchema("TestOrderByWithCollation");

    // createColumn some tables with contents
    Table vars = schema.create(table(VARS).add(column("NAME").setType(STRING).setPkey()));

    Row var13 = new Row().setString("NAME", "var_13");
    Row var1 = new Row().setString("NAME", "var_1");
    Row var10 = new Row().setString("NAME", "var_10");
    Row var3 = new Row().setString("NAME", "var_3");

    vars.insert(var13, var1, var10, var3);
  }

  @Test
  void orderByCollatedColumn() {

    final String result =
        schema.getTable(VARS).select(s("NAME")).orderBy("NAME", Order.ASC).retrieveRows().stream()
            .map(r -> r.getString("NAME"))
            .collect(Collectors.joining(", "));

    assertEquals("var_1, var_3, var_10, var_13", result);
  }
}
