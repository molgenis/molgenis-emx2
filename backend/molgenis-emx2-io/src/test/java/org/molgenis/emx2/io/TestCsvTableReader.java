package org.molgenis.emx2.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.apache.commons.collections4.IteratorUtils;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.readers.CsvTableReader;

public class TestCsvTableReader {

  @Test
  public void testCsvTableReader() {
    // test it works with empty file
    Iterator<Row> iterator = CsvTableReader.read(new StringReader("")).iterator();
    while (iterator.hasNext()) {
      iterator.next();
      fail("should not happen");
    }

    // test it works with only header
    iterator = CsvTableReader.read(new StringReader("test,test")).iterator();
    while (iterator.hasNext()) {
      iterator.next();
      fail("should not happen");
    }

    // test it works with only header
    iterator = CsvTableReader.read(new StringReader("test,test\n")).iterator();
    while (iterator.hasNext()) {
      iterator.next();
      fail("should not happen");
    }
  }

  @Test
  void readCSVWithEmptyLinesViaInputStream() {
    ClassLoader classLoader = getClass().getClassLoader();
    File file =
        new File(
            Objects.requireNonNull(classLoader.getResource("csv-with-empty-lines.csv")).getFile());

    try {
      InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(file));
      Iterable<Row> rows = CsvTableReader.read(inputStreamReader);
      List<Row> list = IteratorUtils.toList(rows.iterator());
      assertEquals(2, list.size());
    } catch (IOException e) {
      fail(e.getMessage());
    }
  }

  @Test
  void readCSVWithEmptyLinesViaFileMethod() {
    ClassLoader classLoader = getClass().getClassLoader();
    File file =
        new File(
            Objects.requireNonNull(classLoader.getResource("csv-with-empty-lines.csv")).getFile());

    try {
      Iterable<Row> rows = CsvTableReader.read(file);
      List<Row> list = IteratorUtils.toList(rows.iterator());
      assertEquals(2, list.size());
    } catch (IOException e) {
      fail(e.getMessage());
    }
  }
}
