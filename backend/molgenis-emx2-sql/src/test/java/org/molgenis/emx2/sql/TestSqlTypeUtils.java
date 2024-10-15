package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.sql.SqlTypeUtils.applyValidationAndComputed;
import static org.molgenis.emx2.sql.SqlTypeUtils.convertRowToMap;

import java.util.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.generator.SnowflakeIdGenerator;

class TestSqlTypeUtils {

  @BeforeAll
  static void before() {
    if (!SnowflakeIdGenerator.hasInstance()) {
      SnowflakeIdGenerator.init("123");
    }
  }

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

  @Test
  void testArrayConversionToMap() {
    List<Column> columns = List.of(column("STRING array", ColumnType.STRING_ARRAY));
    Row row = row("STRING array", "aa,bb");

    Map<String, Object> output = convertRowToMap(columns, row);

    assertAll(
        () -> assertEquals(Set.of("sTRINGArray"), output.keySet()),
        () ->
            assertEquals(List.of("aa", "bb"), Arrays.asList((String[]) output.get("sTRINGArray"))));
  }

  @Test
  void testWorkingValidationForEmailArray() {
    List<Column> columns = List.of(column("SPAM blocklist", ColumnType.EMAIL_ARRAY));
    Row row = row("SPAM blocklist", "bob@example.com,ros@example.com");

    assertDoesNotThrow(() -> applyValidationAndComputed(columns, row));
  }
}
