package org.molgenis.emx2.io.csv;

import org.molgenis.Row;
import org.simpleflatmapper.csv.CsvParser;

import java.io.*;
import java.util.Iterator;
import java.util.Map;

public class RowReaderFlatmapper {

  protected RowReaderFlatmapper() {
    // hide constructor
  }

  /**
   * Don't use because slower than unbuffered
   *
   * @deprecated
   */
  @Deprecated
  public static Iterable<org.molgenis.Row> readBuffered(File f) throws IOException {
    return read(new BufferedReader(new FileReader(f)));
  }

  public static Iterable<org.molgenis.Row> read(File f) throws IOException {
    return read(new FileReader(f));
  }

  public static Iterable<org.molgenis.Row> read(Reader in) throws IOException {

    Iterator<Map> iterator = CsvParser.mapTo(Map.class).iterator(in);

    return new Iterable<Row>() {
      @Override
      public Iterator<Row> iterator() {
        return new Iterator<org.molgenis.Row>() {
          final Iterator<Map> it = iterator;

          public boolean hasNext() {
            return it.hasNext();
          }

          public org.molgenis.Row next() {
            return new Row(it.next());
          }

          @Override
          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }
    };
  }
}
