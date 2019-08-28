package org.molgenis.emx2.io;

import org.junit.Test;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.examples.CompareTools;
import org.molgenis.emx2.io.stores.RowStore;
import org.molgenis.emx2.io.stores.RowStoreForCsvFilesDirectory;
import org.molgenis.emx2.io.stores.RowStoreForCsvInMemory;
import org.molgenis.emx2.io.stores.RowStoreForCsvInZipFile;
import org.molgenis.emx2.utils.MolgenisException;
import org.molgenis.emx2.utils.StopWatch;

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

public class CanReadWriteCSVfilesInDirectoryAndZip {
  @Test
  public void testCsvDirectoryStore() throws IOException {
    Path tmp = Files.createTempDirectory(null);
    try {
      Path folderInTmpDir = tmp.resolve("test");
      Files.createDirectories(folderInTmpDir);
      System.out.println("created tmp dir " + folderInTmpDir);
      RowStoreForCsvFilesDirectory store = new RowStoreForCsvFilesDirectory(folderInTmpDir);
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
      RowStoreForCsvInZipFile store = new RowStoreForCsvInZipFile(zipFile);
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
    executeTest(new RowStoreForCsvInMemory());
  }

  public static void executeTest(RowStore store) throws IOException, MolgenisException {

    List<Row> rows = new ArrayList<>();
    int count = 10;
    for (int i = 1; i <= count; i++) {
      rows.add(
          new Row()
              .setString("stringCol", "test" + i)
              .setInt("intCol", i)
              .setDecimal("decimalCol", new Double(new Double(i) / 2))
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
    store.write("test", rows);
    StopWatch.print("wrote them to " + store.getClass().getSimpleName(), count);

    List<Row> rows2 = store.read("test");
    // for (Row r : rows2) System.out.println(r);
    StopWatch.print("fromReader them back from " + store.getClass().getSimpleName(), count);

    // compare
    CompareTools.assertEquals(rows, rows2);

    // write another one
    store.write("CanQueryExpandIntoArrayForeignKeys", rows);
    StopWatch.print("wrote them to " + store.getClass().getSimpleName(), count);

    rows2 = store.read("CanQueryExpandIntoArrayForeignKeys");
    // for (Row r : rows2) System.out.println(r);
    StopWatch.print("fromReader them back from " + store.getClass().getSimpleName(), count);

    // compare
    CompareTools.assertEquals(rows, rows2);
    StopWatch.print("compared succesfully");
  }
}
