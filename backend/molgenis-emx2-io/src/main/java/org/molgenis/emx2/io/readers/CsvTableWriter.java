package org.molgenis.emx2.io.readers;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;
import org.molgenis.emx2.Row;
import org.simpleflatmapper.csv.CsvWriter;

public class CsvTableWriter {

  private CsvTableWriter() {
    // hide constructor
  }

  public static void write(Iterable<Row> rows, Writer writer, Character seperator)
      throws IOException {

    // get most extensive headers
    Set<String> columnNames = new LinkedHashSet<>();
    Row firstRow = rows.iterator().next();
    columnNames.addAll(firstRow.getColumnNames());

    // we filter mg_ columns. TODO make option to choose
    columnNames =
        columnNames.stream()
            .filter(name -> !name.startsWith("mg_"))
            .collect(Collectors.toCollection(LinkedHashSet::new));

    CsvWriter.CsvWriterDSL<Map> writerDsl =
        CsvWriter.from(Map.class).columns(columnNames.toArray(new String[columnNames.size()]));

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
