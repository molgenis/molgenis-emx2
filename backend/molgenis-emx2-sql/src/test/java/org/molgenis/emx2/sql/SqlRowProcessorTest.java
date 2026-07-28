package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.util.CompareTools;

class SqlRowProcessorTest {

  @Test
  void autoIdGetsSkipped() {
    TableMetadata tableMetadata = table("Test", new Column("myCol").setType(ColumnType.AUTO_ID));

    final Row row = new Row("myCol", null);
    SqlRowProcessor computer = new SqlRowProcessor(tableMetadata.getColumns());
    computer.validateAndCompute(row);
    assertNull(row.getString("myCol"));
  }

  @Test
  void testWorkingValidationForEmailArray() {
    List<Column> columns = List.of(column("SPAM blocklist", ColumnType.EMAIL_ARRAY));
    Row row = row("SPAM blocklist", "bob@example.com,ros@example.com");

    SqlRowProcessor computer = new SqlRowProcessor(columns);
    assertDoesNotThrow(() -> computer.validateAndCompute(row));
  }

  @Test
  void shouldHandleComputedDependencies() {
    List<Column> columns =
        List.of(
            // Depends on C and B
            column("a").setComputed("c + b"),
            // Depends on C
            column("b").setComputed("c"),
            // Depends on nothing
            column("c"));

    Row row = row("c", "1");
    SqlRowProcessor computer = new SqlRowProcessor(columns);
    computer.validateAndCompute(row);
    CompareTools.assertEquals(row, row("c", "1", "b", "1", "a", "11"));
  }

  @Test
  void shouldThrowOnCircularDependency() {
    List<Column> columns =
        List.of(
            // Depends on C and B
            column("a").setComputed("b"),
            // Depends on C
            column("b").setComputed("a"));

    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> new SqlRowProcessor(columns));
    assertEquals("Circular dependency between b and a", exception.getMessage());
  }
}
