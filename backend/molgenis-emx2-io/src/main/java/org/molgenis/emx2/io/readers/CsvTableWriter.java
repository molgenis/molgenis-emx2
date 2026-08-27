package org.molgenis.emx2.io.readers;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.function.Consumer;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.simpleflatmapper.csv.CsvWriter;

public class CsvTableWriter {

  private CsvTableWriter() {
    // hide constructor
  }

  public static void write(
      Iterable<Row> rows, List<String> columnNames, Writer writer, Character seperator)
      throws IOException {
    CsvWriter<Map> csvWriter = createCsvWriter(columnNames, writer, seperator);
    for (Row r : rows) {
      csvWriter.append(toValueMap(r));
    }
  }

  public static Consumer<Row> rowWriter(
      List<String> columnNames, Writer writer, Character seperator) throws IOException {
    CsvWriter<Map> csvWriter = createCsvWriter(columnNames, writer, seperator);
    return row -> {
      try {
        csvWriter.append(toValueMap(row));
      } catch (IOException ioe) {
        throw new MolgenisException(ioe.getMessage());
      }
    };
  }

  private static CsvWriter<Map> createCsvWriter(
      List<String> columnNames, Writer writer, Character seperator) throws IOException {
    CsvWriter.CsvWriterDSL<Map> writerDsl =
        CsvWriter.from(Map.class).columns(columnNames.toArray(new String[0]));
    return writerDsl.separator(seperator).endOfLine("\n").to(writer);
  }

  private static Map<String, String> toValueMap(Row r) {
    // fromReader all values into strings first
    Map<String, String> values = new LinkedHashMap<>();
    for (String columnName : r.getColumnNames()) {
      // We remove all prefixing _ characters of column names because SFM csv writer has a bug
      // that prevents values with a key that start with a _ are skipped. Because of the ordering,
      // they appear under the correct header column.
      values.put(columnName.replaceFirst("^_+", ""), r.getString(columnName));
    }
    return values;
  }
}
