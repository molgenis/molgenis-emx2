package org.molgenis.emx2.io.readers;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import org.molgenis.emx2.NameMapper;
import org.molgenis.emx2.Row;
import org.simpleflatmapper.csv.CsvWriter;

public class CsvTableWriter {

  private CsvTableWriter() {
    // hide constructor
  }

  public static void write(
      Iterable<Row> rows,
      List<String> columnNames,
      NameMapper mapper,
      Writer writer,
      Character seperator)
      throws IOException {

    List<String> columnLabels = columnNames.stream().map(name -> mapper.map(name)).toList();

    CsvWriter.CsvWriterDSL<Map> writerDsl =
        CsvWriter.from(Map.class).columns(columnLabels.toArray(new String[columnNames.size()]));

    CsvWriter<Map> csvWriter = writerDsl.separator(seperator).to(writer);
    for (Row r : rows) {
      // fromReader all values into strings first
      Map<String, String> values = new LinkedHashMap<>();
      for (String columnName : r.getColumnNames()) {
        values.put(mapper.map((columnName)), r.getString(columnName));
      }
      csvWriter.append(values);
    }
  }
}
