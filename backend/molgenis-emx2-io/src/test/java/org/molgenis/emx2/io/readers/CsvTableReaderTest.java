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
    Iterable<Row> rowsIterable = CsvTableReader.read(file);
    List<Row> rows = Lists.newArrayList(rowsIterable);
    assertEquals(2, rows.size());
    assertEquals("User", rows.get(0).getString("tableName"));
    assertEquals("username", rows.get(0).getString("columnName"));
    assertEquals("ImageTest", rows.get(0).getString("profiles"));
    assertEquals("User", rows.get(1).getString("tableName"));
    assertEquals("picture", rows.get(1).getString("columnName"));
    assertEquals("ImageTest", rows.get(1).getString("profiles"));
  }
}
