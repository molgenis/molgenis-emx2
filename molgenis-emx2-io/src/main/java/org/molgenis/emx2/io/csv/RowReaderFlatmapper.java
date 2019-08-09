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

  public static Iterable<org.molgenis.Row> read(File f) throws IOException {
    return read(new FileReader(f));
  }

  public static Iterable<org.molgenis.Row> read(Reader in) throws IOException {
    // don't used buffered, it is slower
    Iterator<Map> iterator = CsvParser.mapTo(Map.class).iterator(in);

    return () ->
        new Iterator<Row>() {
          final Iterator<Map> it = iterator;

          public boolean hasNext() {
            return it.hasNext();
          }

          public Row next() {
            return new Row(it.next());
          }

          @Override
          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
  }
}
