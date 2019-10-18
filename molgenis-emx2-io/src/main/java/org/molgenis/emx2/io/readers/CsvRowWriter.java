package org.molgenis.emx2.io.readers;

import org.molgenis.emx2.Row;
import org.simpleflatmapper.csv.CsvWriter;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CsvRowWriter {

  private CsvRowWriter() {
    // hide constructor
  }

  public static void writeCsv(List<Row> rows, Writer writer, Character seperator)
      throws IOException {

    if (rows.isEmpty()) return;

    CsvWriter.CsvWriterDSL<Map> writerDsl =
        CsvWriter.from(Map.class)
            .columns(
                rows.get(0)
                    .getColumnNames()
                    .toArray(new String[rows.get(0).getColumnNames().size()]));

    CsvWriter<Map> csvWriter = writerDsl.separator(seperator).to(writer);
    for (Row r : rows) {
      // fromReader all values into strings first
      Map<String, String> values = new LinkedHashMap<>();
      for (String columnName : r.getColumnNames()) {
        values.put(columnName, r.getString(columnName));
      }
      csvWriter.append(values);
    }
  }
}
