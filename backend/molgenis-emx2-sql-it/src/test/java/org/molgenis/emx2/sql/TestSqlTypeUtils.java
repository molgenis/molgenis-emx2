package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.sql.SqlTypeUtils.applyValidationAndComputed;

import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

class TestSqlTypeUtils {

  @Test
  void autoIdGetsGenerated() {
    TableMetadata tableMetadata = table("Test", new Column("myCol").setType(ColumnType.AUTO_ID));
    final Row row = new Row("myCol", null);
    applyValidationAndComputed(tableMetadata.getColumns(), row);
    assertNotNull(row.getString("myCol"));

    // and now it should change on update
    final Row copy = new Row(row);
    applyValidationAndComputed(tableMetadata.getColumns(), copy);
    assertEquals(row.getString("myCol"), copy.getString("myCol"));
  }

  @Test
  void autoIdGetsGeneratedWithPreFix() {
    TableMetadata tableMetadata =
        table(
            "Test",
            new Column("myCol")
                .setType(ColumnType.AUTO_ID)
                .setComputed("foo-" + Constants.COMPUTED_AUTOID_TOKEN + "-bar"));
    final Row row = new Row("myCol", null);
    applyValidationAndComputed(tableMetadata.getColumns(), row);
    assertTrue(row.getString("myCol").startsWith("foo"));
    assertTrue(row.getString("myCol").endsWith("bar"));

    // and now it should change on update
    final Row copy = new Row(row);

    applyValidationAndComputed(tableMetadata.getColumns(), copy);
    assertEquals(row.getString("myCol"), row.getString("myCol"));
  }
}
