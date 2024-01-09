package org.molgenis.emx2.io;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.datamodels.util.CompareTools;
import org.molgenis.emx2.io.tablestore.*;
import org.molgenis.emx2.utils.StopWatch;

@Tag("slow")
public class TestReadWriteStores {
  @Test
  public void testCsvDirectoryStore() throws IOException {
    Path tmp = Files.createTempDirectory(null);
    try {
      Path folderInTmpDir = tmp.resolve("test");
      Files.createDirectories(folderInTmpDir);
      System.out.println("created tmp dir " + folderInTmpDir);
      TableStoreForCsvFilesDirectory store = new TableStoreForCsvFilesDirectory(folderInTmpDir);
      executeTest(store);
    } catch (MolgenisException e) {
      e.printStackTrace();
    } finally {
      Files.walk(tmp).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }
    if (Files.exists(tmp))
      throw new RuntimeException(
          "TMP directory " + tmp + " not deleted. This should never happen.");
  }

  @Test
  public void testCsvZipStore() throws IOException {
    Path tmp = Files.createTempDirectory(null);
    try {
      Path zipFile = tmp.resolve("test.zip");
      System.out.println("defined zip file " + zipFile);
      TableStoreForCsvInZipFile store = new TableStoreForCsvInZipFile(zipFile);
      executeTest(store);
    } catch (MolgenisException e) {
      e.printStackTrace();
    } finally {
      Files.walk(tmp).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }
    if (Files.exists(tmp))
      throw new RuntimeException(
          "TMP directory " + tmp + " not deleted. This should never happen.");
  }

  @Test
  public void testExcelStore() throws IOException {
    Path tmp = Files.createTempDirectory(null);
    try {
      Path excelFile = tmp.resolve("test.xlsx");
      System.out.println("defined excel file " + excelFile);
      TableStoreForXlsxFile store = new TableStoreForXlsxFile(excelFile);
      executeTest(store);
    } catch (MolgenisException e) {
      e.printStackTrace();
    } finally {
      Files.walk(tmp).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }
    if (Files.exists(tmp))
      throw new RuntimeException(
          "TMP directory " + tmp + " not deleted. This should never happen.");
  }

  @Test
  public void testCsvStringStore() throws IOException, MolgenisException {
    executeTest(new TableStoreForCsvInMemory());
  }

  public static void executeTest(TableStore store) throws IOException, MolgenisException {

    List<Row> rows = new ArrayList<>();
    int count = 10;
    for (int i = 1; i <= count; i++) {
      rows.add(
          new Row()
              .setString("stringCol", "test" + i)
              .setInt("intCol", i)
              .setDecimal("decimalCol", Double.valueOf(i / 2))
              .setUuid("uuidCol", UUID.randomUUID())
              .setDate("dateCol", LocalDate.of(2019, 12, 12))
              .setDateTime("datetimeCol", LocalDateTime.now())
              .setBool("boolCol", true)
              .setStringArray("stringarrayCol", new String[] {"a", "b,including comma,"})
              .setIntArray("intarrayCol", new Integer[] {1, 2})
              .setDecimalArray("doubleArrayCol", new Double[] {1.0, 2.0})
              .setDecimalArray("doubleArrayCol", new Double[] {1.0, 2.0})
              .setDateArray(
                  "dateArray",
                  new LocalDate[] {LocalDate.of(2019, 12, 12), LocalDate.of(2019, 12, 12)})
              .setDateTimeArray(
                  "datetimeArrayCol",
                  new LocalDateTime[] {LocalDateTime.now(), LocalDateTime.now()})
              .setBoolArray("booleanArrayCol", new Boolean[] {true, false}));
    }
    StopWatch.start("created some rows");

    // write them
    store.writeTable("test", new ArrayList<>(rows.get(0).getColumnNames()), rows);
    store.writeTable("test2", new ArrayList<>(rows.get(0).getColumnNames()), rows);

    StopWatch.print("wrote them to " + store.getClass().getSimpleName(), count);

    List<Row> rows2 =
        StreamSupport.stream(store.readTable("test2").spliterator(), false)
            .collect(Collectors.toList());
    // for (Row r : rows2) System.out.println(r);
    StopWatch.print("fromReader them back from " + store.getClass().getSimpleName(), count);

    // compare
    CompareTools.assertEquals(rows, rows2);

    // write another one
    store.writeTable("test3", new ArrayList<>(rows.get(0).getColumnNames()), rows);
    StopWatch.print("wrote them to " + store.getClass().getSimpleName(), count);

    rows2 =
        StreamSupport.stream(store.readTable("test3").spliterator(), false)
            .collect(Collectors.toList());
    // for (Row r : rows2) System.out.println(r);
    StopWatch.print("fromReader them back from " + store.getClass().getSimpleName(), count);

    // compare
    CompareTools.assertEquals(rows, rows2);
    StopWatch.print("compared succesfully");

    // write empty
    store.writeTable("test4", List.of("empty"), new ArrayList<>());

    // test that reading store that doesn't exist errors properly
    try {
      store.readTable("fake");
      fail("should have failed");
    } catch (MolgenisException me) {
      System.out.println("errored correctly:" + me);
    }
  }
}
