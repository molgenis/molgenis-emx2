package org.molgenis.emx2.io.readers;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import org.molgenis.emx2.Row;
import org.simpleflatmapper.csv.CsvWriter;

public class CsvTableWriter {

  private CsvTableWriter() {
    // hide constructor
  }

  public static void write(
      Iterable<Row> rows, List<String> columnNames, Writer writer, Character seperator)
      throws IOException {
    CsvWriter.CsvWriterDSL<Map> writerDsl =
        CsvWriter.from(Map.class).columns(columnNames.toArray(new String[columnNames.size()]));

    CsvWriter<Map> csvWriter = writerDsl.separator(seperator).endOfLine("\n").to(writer);

    for (Row r : rows) {
      // fromReader all values into strings first
      Map<String, String> values = new LinkedHashMap<>();
      for (String columnName : r.getColumnNames()) {
        // We remove all prefixing _ characters of column names because SFM csv writer has a bug
        // that prevents values with a key that start with a _ are skipped. Because of the ordering,
        // they appear under the correct header column.
        values.put(columnName.replaceFirst("^_+", ""), r.getString(columnName));
      }
      csvWriter.append(values);
    }
  }
}
