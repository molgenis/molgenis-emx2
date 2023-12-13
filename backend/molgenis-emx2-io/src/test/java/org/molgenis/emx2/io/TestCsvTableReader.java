package org.molgenis.emx2.io;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.StringReader;
import java.util.Iterator;
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
}
