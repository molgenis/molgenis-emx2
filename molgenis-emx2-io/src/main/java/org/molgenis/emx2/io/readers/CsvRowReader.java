package org.molgenis.emx2.io.readers;

import org.molgenis.emx2.Row;
import org.molgenis.emx2.utils.MolgenisException;
import org.simpleflatmapper.csv.CsvParser;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CsvRowReader {

  private CsvRowReader() {
    // to prevent new CsvRowReader()
  }

  public static Iterable<Row> read(File f) throws IOException {
    return read(new FileReader(f), ',');
  }

  public static List<Row> readList(Reader in, Character separator) {
    List<Row> result = new ArrayList<>();
    for (Row r : read(in, separator)) {
      result.add(r);
    }
    return result;
  }

  public static Iterable<Row> read(Reader in, Character separator) {
    try {
      // don't use buffered, it is slower
      Iterator<LinkedHashMap> iterator =
          CsvParser.dsl().separator(separator).trimSpaces().mapTo(LinkedHashMap.class).iterator(in);

      return () ->
          new Iterator<Row>() {
            final Iterator<LinkedHashMap> it = iterator;
            final AtomicInteger line = new AtomicInteger(1);

            public boolean hasNext() {
              try {
                return it.hasNext();
              } catch (Exception e) {
                throw new MolgenisException(
                    "io_exception",
                    "IO exception",
                    e.getClass().getName()
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
      throw new MolgenisException("io_exception", "IO exception", ioe.getMessage(), ioe);
    }
  }
}
