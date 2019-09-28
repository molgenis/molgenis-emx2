package org.molgenis.emx2.io;

import org.junit.Test;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.stores.RowStoreForXlsxFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

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
    RowStoreForXlsxFile store = new RowStoreForXlsxFile(excelFile);
    store.write("test", rows);

    // and read
    store = new RowStoreForXlsxFile(excelFile);
    List<Row> rows2 = store.read("test");

    assertEquals(10, rows2.size());
  }
}
