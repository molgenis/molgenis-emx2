package org.molgenis.emx2.io.readers.vcf;

import java.io.*;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.simpleflatmapper.csv.CsvParser;

public class VcfReader {

  private VcfReader() {
    // to prevent new VcfReader()
  }

  public static Iterable<Row> read(File f) throws IOException {
    return read(new FileReader(f), f.getName());
  }

  public static Iterable<Row> read(Reader in, String fileName) {
    try {
      BufferedReader bufferedReader = new BufferedReader(in);
      String firstLine = bufferedReader.readLine();
      //      String secondLine = bufferedReader.readLine();

      // if file is empty we return empty iterator
      if (firstLine == null || firstLine.trim().equals("")) {
        return Collections.emptyList();
      }
      // if file is empty we return empty iterator
      if (firstLine.trim().equals("")) {
        return Collections.emptyList();
      }
      // vcf file metadata lines start with ##
      if (!firstLine.trim().startsWith("##")) {
        return Collections.emptyList();
      }

      // vcf file metadata lines start with ##
      boolean header = true;
      String columnHeader = null;
      while (header) {
        String line = bufferedReader.readLine();
        if (line.startsWith("##")) {
          // ignore for now, later store in File table, column Header
          bufferedReader.mark(2000000);
        } else if (line.startsWith("#")) {
          bufferedReader.reset();
          header = false;
        }
      }

      // vcf separator is always tab
      char separator = '\t';

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
              return new VcfRow(it.next(), fileName);
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
