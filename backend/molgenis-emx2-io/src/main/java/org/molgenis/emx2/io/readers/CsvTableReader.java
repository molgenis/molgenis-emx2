package org.molgenis.emx2.io.readers;

import java.io.*;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.simpleflatmapper.csv.CsvParser;

public class CsvTableReader {

  private CsvTableReader() {
    // to prevent new CsvRowReader()
  }

  public static Iterable<Row> read(File f) throws IOException {
    return read(new FileReader(f));
  }

  public static Iterable<Row> read(Reader in) {
    try {
      BufferedReader bufferedReader = new BufferedReader(in);
      bufferedReader.mark(2000000);
      String firstLine = bufferedReader.readLine();
      String secondLine = bufferedReader.readLine();

      // if file is empty we return empty iterator
      if (firstLine == null || firstLine.trim().equals("") || secondLine == null) {
        return Collections.EMPTY_LIST;
      }
      char separator = ',';
      // if file is empty we return empty iterator
      if (firstLine == null || firstLine.trim().equals("")) {
        return Collections.EMPTY_LIST;
      }
      // guess the separator
      if (firstLine.contains("\t")) {
        separator = '\t';
      }
      if (firstLine.contains(";")) {
        separator = ';';
      }

      // push back in
      bufferedReader.reset();

      // don't use buffered, it is slower
      Iterator<Map> iterator =
          CsvParser.dsl()
              .separator(separator)
              .trimSpaces()
              .mapTo(Map.class)
              .iterator(bufferedReader);

      //        // configure csv reader
      //        CsvMapper csvMapper =
      //                new CsvMapper()
      //                        .configure(CsvParser.Feature.TRIM_SPACES, true)
      //                        .configure(CsvParser.Feature.ALLOW_TRAILING_COMMA, true)
      //                        .configure(CsvParser.Feature.FAIL_ON_MISSING_COLUMNS, false)
      //                        .configure(CsvParser.Feature.IGNORE_TRAILING_UNMAPPABLE, true)
      //                        .configure(CsvParser.Feature.INSERT_NULLS_FOR_MISSING_COLUMNS, true)
      //                        .configure(CsvParser.Feature.SKIP_EMPTY_LINES, true)
      //                        .configure(CsvParser.Feature.TRIM_SPACES, true)
      //                        .configure(CsvParser.Feature.WRAP_AS_ARRAY, false);
      //        CsvSchema csvSchema =
      //                csvMapper.schemaWithHeader().withColumnSeparator(separator).withComments();
      //        Iterator<Map> iterator =
      //                csvMapper.readerFor(Map.class).with(csvSchema).readValues(bufferedReader);

      return () ->
          new Iterator<>() {
            final Iterator<Map> it = iterator;
            final AtomicInteger line = new AtomicInteger(1);

            public boolean hasNext() {
              try {
                return it.hasNext();
              } catch (Exception e) {
                throw new MolgenisException(
                    "Import failed: "
                        + e.getClass().getName()
                        + ": "
                        + e.getMessage()
                        + ". Error at line "
                        + line.get()
                        + ".",
                    e);
              }
            }

            public Row next() {
              return new Row(it.next());
            }

            @Override
            public void remove() {
              throw new UnsupportedOperationException();
            }
          };
    } catch (IOException ioe) {
      throw new MolgenisException("Import failed", ioe);
    }
  }
}
