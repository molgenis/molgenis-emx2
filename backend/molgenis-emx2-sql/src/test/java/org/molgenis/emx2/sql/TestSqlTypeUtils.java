package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.generator.IdGenerator;

class TestSqlTypeUtils {

  static IdGenerator idGenerator = () -> "123-abc";

  @BeforeAll
  public static void beforeAll() {
    SqlTypeUtils.idGenerator = idGenerator;
  }

  @Test
  void autoIdGetsGenerated() {
    TableMetadata tableMetadata = table("Test", new Column("myCol").setType(ColumnType.AUTO_ID));
    final Row row = new Row("myCol", null);
    final Map<String, Object> values =
        SqlTypeUtils.validateAndGetVisibleValuesAsMap(
            row, tableMetadata, tableMetadata.getColumns());
    assertEquals("123-abc", values.get("myCol"));
  }

  @Test
  void autoIdGetsGeneratedWithPreFix() {
    TableMetadata tableMetadata =
        table(
            "Test",
            new Column("myCol")
                .setType(ColumnType.AUTO_ID)
                .setComputed("foo-" + Constants.COMPUTED_AUTOID_TOKEN));
    final Row row = new Row("myCol", null);
    final Map<String, Object> values =
        SqlTypeUtils.validateAndGetVisibleValuesAsMap(
            row, tableMetadata, tableMetadata.getColumns());
    assertEquals("foo-123-abc", values.get("myCol"));
  }
}
