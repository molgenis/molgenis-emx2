package org.molgenis.emx2.io.readers;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.simpleflatmapper.csv.CsvParser;

public class CsvTableReader {

  private CsvTableReader() {
    // to prevent new CsvRowReader()
  }

  public static Iterable<Row> read(File f) throws IOException {
    return read(new InputStreamReader(new FileInputStream(f)));
  }

  public static Iterable<Row> read(Reader in) {
    try {
      BufferedReader bufferedReader = new BufferedReader(in);

      // skip BOM if exists (issue in some windows edited CSV files)
      bufferedReader.mark(1);
      int ch = bufferedReader.read();
      if (ch != 0xFEFF) {
        bufferedReader.reset(); // No BOM, rewind
      }

      // guess separator
      bufferedReader.mark(2000000);
      String firstLine = bufferedReader.readLine();
      String secondLine = bufferedReader.readLine();

      // if file is empty we return empty iterator
      if (firstLine == null || firstLine.trim().equals("") || secondLine == null) {
        return Collections.emptyList();
      }
      char separator = ',';
      // if file is empty we return empty iterator
      if (firstLine.trim().equals("")) {
        return Collections.emptyList();
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
              HashMap<String, Object> next = (HashMap<String, Object>) it.next();
              boolean isEmpty = next.values().stream().allMatch(Objects::isNull);
              while (isEmpty && it.hasNext()) {
                next = (HashMap<String, Object>) it.next();
                isEmpty = next.values().stream().allMatch(Objects::isNull);
              }
              return new Row(next);
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
