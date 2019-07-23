package org.molgenis.emx2.io.csv;

import org.molgenis.Row;
import org.molgenis.beans.RowBean;
import org.simpleflatmapper.csv.CsvParser;

import java.io.*;
import java.util.Iterator;
import java.util.Map;

public class RowReaderFlatmapper {

  /** Don't use because slower than unbuffered */
  @Deprecated
  public static Iterable<Row> readBuffered(File f) throws IOException {
    return read(new BufferedReader(new FileReader(f)));
  }

  public static Iterable<Row> read(File f) throws IOException {
    return read(new FileReader(f));
  }

  public static Iterable<Row> read(Reader in) throws IOException {

    Iterator<Map> iterator = CsvParser.mapTo(Map.class).iterator(in);

    return new Iterable<Row>() {
      // ... some reference to data
      public Iterator<Row> iterator() {
        return new Iterator<Row>() {
          final Iterator<Map> it = iterator;

          public boolean hasNext() {
            return it.hasNext();
          }

          public Row next() {
            return new RowBean(it.next());
          }

          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }
    };
  }
}
