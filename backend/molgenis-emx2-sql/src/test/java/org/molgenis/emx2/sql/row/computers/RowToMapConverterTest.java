package org.molgenis.emx2.sql.row.computers;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Row;

class RowToMapConverterTest {

  @Test
  void testArrayConversionToMap() {
    List<Column> columns = List.of(column("STRING array", ColumnType.STRING_ARRAY));
    Row row = row("STRING array", "aa,bb");

    Map<String, Object> output = new RowToMapConverter().convertRowToMap(columns, row);

    assertAll(
        () -> assertEquals(Set.of("sTRINGArray"), output.keySet()),
        () ->
            assertEquals(List.of("aa", "bb"), Arrays.asList((String[]) output.get("sTRINGArray"))));
  }
}
