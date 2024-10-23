package org.molgenis.emx2.io.readers;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Row;

class CsvTableReaderTest {

  @Test
  void readImageMeta() throws IOException {
    File file =
        new File(getClass().getClassLoader().getResource("profile/imagetest.csv").getFile());
    //    file =
    //        new File(
    //
    // "/Users/connor/Code/emx2/molgenis-emx2/backend/molgenis-emx2-io/src/test/resources/profile/imagetest.csv");

    Iterable<Row> rowsIterable = CsvTableReader.read(file);
    List<Row> rows = Lists.newArrayList(rowsIterable);
    assertEquals(4, rows.size());
    assertEquals("User", rows.get(0).getString("tableName"));
    assertEquals("username", rows.get(0).getString("columnName"));
    assertEquals("ImageTest", rows.get(0).getString("profiles"));
    assertEquals("User", rows.get(3).getString("tableName"));
    assertEquals("picture", rows.get(3).getString("columnName"));
    assertEquals("ImageTest", rows.get(3).getString("profiles"));
  }

  @Test
  void readProfileMeta() throws IOException {
    File file = new File(getClass().getClassLoader().getResource("profile/RD3.csv").getFile());
    Iterable<Row> rowsIterable = CsvTableReader.read(file);
    List<Row> rows = Lists.newArrayList(rowsIterable);
    assertEquals(21, rows.size());
    assertEquals("overview", rows.get(0).getString("tableName"));
    assertEquals("RD3", rows.get(0).getString("profiles"));
  }
}
