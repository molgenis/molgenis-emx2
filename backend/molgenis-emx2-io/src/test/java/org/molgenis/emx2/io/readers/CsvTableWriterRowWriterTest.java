package org.molgenis.emx2.io.readers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Row;

class CsvTableWriterRowWriterTest {

  private static final List<String> COLUMNS = List.of("id", "name");

  @Test
  void rowWriterWritesHeaderWhenNoRowsAreAccepted() throws IOException {
    StringWriter writer = new StringWriter();
    CsvTableWriter.rowWriter(COLUMNS, writer, ',');
    writer.flush();

    assertEquals("id,name\n", writer.toString());
  }

  @Test
  void rowWriterWritesHeaderAndRows() throws IOException {
    StringWriter writer = new StringWriter();
    Consumer<Row> rowWriter = CsvTableWriter.rowWriter(COLUMNS, writer, ',');

    Row row = new Row();
    row.set("id", "id0");
    row.set("name", "name0");
    rowWriter.accept(row);
    writer.flush();

    assertEquals("id,name\nid0,name0\n", writer.toString());
  }
}
