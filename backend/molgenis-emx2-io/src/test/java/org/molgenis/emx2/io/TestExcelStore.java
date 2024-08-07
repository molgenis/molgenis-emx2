package org.molgenis.emx2.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.tablestore.TableStoreForXlsxFile;

@Tag("slow")
public class TestExcelStore {

  @Test
  public void testSimple() throws IOException {
    List<Row> rows = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      Row r = new Row();
      r.set("name", "test" + i);
      r.set("id", i);
      rows.add(r);
    }

    Path tmp = Files.createTempDirectory("TestExcelStore");
    tmp.toFile().deleteOnExit();

    Path excelFile = tmp.resolve("test.xlsx");
    TableStoreForXlsxFile store = new TableStoreForXlsxFile(excelFile);
    store.writeTable("test", List.of("id", "name"), rows);
    store.writeTable("test2", List.of("id", "name"), rows);

    // and read
    store = new TableStoreForXlsxFile(excelFile);
    Iterable<Row> rows2 = store.readTable("test2");
    Iterable<Row> rows3 = store.readTable("test");

    assertEquals(10, StreamSupport.stream(rows2.spliterator(), false).count());
    assertEquals(10, StreamSupport.stream(rows3.spliterator(), false).count());

    excelFile = tmp.resolve("error.xlsx");
    final TableStoreForXlsxFile errorStore = new TableStoreForXlsxFile(excelFile);
    assertThrows(MolgenisException.class, () -> errorStore.getTableNames());
    assertThrows(MolgenisException.class, () -> errorStore.readTable("test"));
  }

  @Test
  void testLargeNumeric() {
    ClassLoader classLoader = getClass().getClassLoader();
    File excelFile = new File(classLoader.getResource("TestLargeNumeric.xlsx").getFile());
    TableStoreForXlsxFile store = new TableStoreForXlsxFile(excelFile.toPath());
    for (Row r : store.readTable("Sheet1")) {
      assertEquals(10000070587L, r.getLong("TestLongNumeric"));
      assertThrows(
          MolgenisException.class,
          () ->
              r.getInteger(
                  "TestLongNumeric")); // is this desired that we give an error? I suppose so?
    }
  }
}
