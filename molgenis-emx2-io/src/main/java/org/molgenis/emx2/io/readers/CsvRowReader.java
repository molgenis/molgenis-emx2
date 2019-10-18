package org.molgenis.emx2.io.readers;

import org.molgenis.emx2.Row;
import org.simpleflatmapper.csv.CsvParser;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

public class CsvRowReader {

  private CsvRowReader() {
    // to prevent new CsvRowReader()
  }

  public static Iterable<Row> read(File f) throws IOException {
    return read(new FileReader(f), ',');
  }

  public static List<Row> readList(Reader in, Character separator) throws IOException {
    List<Row> result = new ArrayList<>();
    for (Row r : read(in, separator)) {
      result.add(r);
    }
    return result;
  }

  public static Iterable<Row> read(Reader in, Character separator) throws IOException {
    // don't use buffered, it is slower
    Iterator<LinkedHashMap> iterator =
        CsvParser.dsl().separator(separator).trimSpaces().mapTo(LinkedHashMap.class).iterator(in);

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
