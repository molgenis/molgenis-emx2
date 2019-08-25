package org.molgenis.emx2.io.csv;

import org.molgenis.data.Row;
import org.simpleflatmapper.csv.CsvParser;

import java.io.*;
import java.util.*;

public class RowReaderFlatmapper {

  protected RowReaderFlatmapper() {
    // hide constructor
  }

  public static Iterable<Row> read(File f) throws IOException {
    return read(new FileReader(f));
  }

  public static List<Row> readList(Reader in) throws IOException {
    List<Row> result = new ArrayList<>();
    for (Row r : read(in)) {
      result.add(r);
    }
    return result;
  }

  public static Iterable<Row> read(Reader in) throws IOException {
    // don't used buffered, it is slower
    Iterator<LinkedHashMap> iterator =
        CsvParser.dsl().trimSpaces().mapTo(LinkedHashMap.class).iterator(in);

    return () ->
        new Iterator<Row>() {
          final Iterator<LinkedHashMap> it = iterator;

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
