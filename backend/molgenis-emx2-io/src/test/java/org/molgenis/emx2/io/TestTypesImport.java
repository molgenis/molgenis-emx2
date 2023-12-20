package org.molgenis.emx2.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvFile;
import org.molgenis.emx2.io.tablestore.TableStoreForXlsxFile;

public class TestTypesImport {

  @Test
  public void testExcelTypesCast() {
    ClassLoader classLoader = getClass().getClassLoader();
    Path path = new File(classLoader.getResource("TypeTest.xlsx").getFile()).toPath();
    TableStore store = new TableStoreForXlsxFile(path);

    for (Row r : store.readTable("Sheet1")) {

      assertEquals("1.0", r.getString("decimal"));
      assertEquals(Double.valueOf(1.0), r.getDecimal("decimal"));

      assertEquals("1", r.getString("int"));
      assertEquals(Integer.valueOf(1), r.getInteger("int"));
    }
  }

  @Test
  public void testCsvTypesCast() {
    ClassLoader classLoader = getClass().getClassLoader();
    Path path = new File(classLoader.getResource("TypeTest.csv").getFile()).toPath();
    TableStore store = new TableStoreForCsvFile(path);

    for (Row r : store.readTable("Sheet1")) {
      assertEquals(
          "Respiratory, allergy and skin characteristics", r.getStringArray("string_array")[0]);

      assertEquals(
          "Respiratory, allergy and skin characteristics", r.getStringArray("string_array2")[0]);

      assertEquals(
          "Respiratory, allergy and skin characteristics2", r.getStringArray("string_array2")[1]);
    }
  }

  @Test
  public void testTsvTypeCast() {
    ClassLoader classLoader = getClass().getClassLoader();
    Path path = new File(classLoader.getResource("TypeTest.tsv").getFile()).toPath();
    TableStore store = new TableStoreForCsvFile(path);

    for (Row r : store.readTable("Sheet1")) {
      assertEquals("1", r.getString("column1"));
      assertEquals("2", r.getString("column2"));
    }
  }

  @Test
  public void testSemiColonTypeCast() {
    ClassLoader classLoader = getClass().getClassLoader();
    Path path = new File(classLoader.getResource("TypeTestSemiColon.csv").getFile()).toPath();
    TableStore store = new TableStoreForCsvFile(path);

    for (Row r : store.readTable("Sheet1")) {
      assertEquals("a", r.getString("string"));
      assertEquals("b", r.getStringArray("string_array")[1]);
      assertEquals("a,b", r.getStringArray("string_array2")[0]);
    }
  }
}
