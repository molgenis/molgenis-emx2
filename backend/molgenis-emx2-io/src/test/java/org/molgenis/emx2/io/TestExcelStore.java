package org.molgenis.emx2.io;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.tablestore.TableStoreForXlsxFile;

public class TestExcelStore {

  @Test
  public void testSimple() throws IOException {
    List<Row> rows = new ArrayList<Row>();

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
    store.writeTable("test", List.of(), rows);

    // and read
    store = new TableStoreForXlsxFile(excelFile);
    List<Row> rows2 = store.readTable("test");

    assertEquals(10, rows2.size());

    // empty rows
    excelFile = tmp.resolve("test.xlsx");
    store = new TableStoreForXlsxFile(excelFile);
    store.writeTable("test2", List.of("blaat"), new ArrayList<>());
  }
}
