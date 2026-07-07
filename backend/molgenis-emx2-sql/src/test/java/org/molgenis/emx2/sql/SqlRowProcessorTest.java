package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.TableMetadata;

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
}
