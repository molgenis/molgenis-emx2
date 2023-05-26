package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

class TestSqlTypeUtils {

  @Test
  void autoIdGetsGenerated() {
    TableMetadata tableMetadata = table("Test", new Column("myCol").setType(ColumnType.AUTO_ID));
    final Row row = new Row("myCol", null);
    final Map<String, Object> values1 =
        SqlTypeUtils.validateAndGetVisibleValuesAsMap(
            row, tableMetadata, tableMetadata.getColumns());
    assertNotNull(values1.get("myCol"));

    // and now it should change on update
    final Row copy = new Row(values1);
    final Map<String, Object> values2 =
        SqlTypeUtils.validateAndGetVisibleValuesAsMap(
            copy, tableMetadata, tableMetadata.getColumns());
    assertEquals(values1.get("myCol"), values2.get("myCol"));
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
    final Map<String, Object> values1 =
        SqlTypeUtils.validateAndGetVisibleValuesAsMap(
            row, tableMetadata, tableMetadata.getColumns());
    assertTrue(((String) values1.get("myCol")).startsWith("foo"));
    assertTrue(((String) values1.get("myCol")).endsWith("bar"));

    // and now it should change on update
    final Row copy = new Row(values1);
    final Map<String, Object> values2 =
        SqlTypeUtils.validateAndGetVisibleValuesAsMap(
            copy, tableMetadata, tableMetadata.getColumns());
    assertEquals(values1.get("myCol"), values2.get("myCol"));
  }
}
