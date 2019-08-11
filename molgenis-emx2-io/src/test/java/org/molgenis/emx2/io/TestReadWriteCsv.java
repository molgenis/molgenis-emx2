package org.molgenis.emx2.io;

import org.junit.Test;
import org.molgenis.MolgenisException;
import org.molgenis.Row;
import org.molgenis.emx2.examples.CompareTools;
import org.molgenis.emx2.io.csv.CsvRowReader;
import org.molgenis.emx2.io.csv.CsvRowWriter;
import org.molgenis.utils.StopWatch;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TestReadWriteCsv {
  @Test
  public void test() throws IOException, MolgenisException {

    List<Row> rows = new ArrayList<>();
    for (int i = 1; i <= 10; i++) {
      rows.add(
          new Row()
              .setString("stringCol", "test" + i)
              .setInt("intCol", i)
              .setDecimal("decimalCol", new Double(new Double(i) / 2))
              .setUuid("uuidCol", UUID.randomUUID())
              .setDate("dateCol", LocalDate.of(2019, 12, i))
              .setDateTime("datetimeCol", LocalDateTime.now())
              .setBool("boolCol", true)
              .setStringArray("stringarrayCol", new String[] {"a", "b"})
              .setIntArray("intarrayCol", new Integer[] {1, 2})
              .setDecimalArray("doubleArrayCol", new Double[] {1.0, 2.0})
              .setDecimalArray("doubleArrayCol", new Double[] {1.0, 2.0})
              .setDateArray(
                  "dateArray",
                  new LocalDate[] {LocalDate.of(2019, 12, i), LocalDate.of(2019, 12, i)})
              .setDateTimeArray(
                  "datetimeArrayCol",
                  new LocalDateTime[] {LocalDateTime.now(), LocalDateTime.now()})
              .setBoolArray("booleanArrayCol", new Boolean[] {true, false}));
    }
    StopWatch.start("created some rows");

    // write them
    StringWriter writer = new StringWriter();
    CsvRowWriter.writeCsv(rows, writer);

    System.out.println(writer.toString());
    StopWatch.print("wrote them to CSV");

    // read them
    List<Row> rows2 = CsvRowReader.readList(new StringReader(writer.toString()));
    for (Row r : rows2) System.out.println(r);
    StopWatch.print("read them back from CSV");

    // compare
    CompareTools.assertEquals(rows, rows2);

    StopWatch.print("compared succesfully");
  }
}
