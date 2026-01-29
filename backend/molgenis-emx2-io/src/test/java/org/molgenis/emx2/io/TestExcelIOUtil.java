package org.molgenis.emx2.io;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Row;

class TestExcelIOUtil {

  @Test
  void testGetExportStringValue() {
    Row row = new Row();
    String key = "keyOfRow";
    row.setString(key, "cell value");
    String result = ExcelIOUtil.getExportStringValue(row, key);
    assertEquals("cell value", result);
  }

  @Test
  void testGetExportStringValueWithEquals() {
    Row row = new Row();
    String key = "keyOfRow";
    row.setString(key, "=cell value");
    String result = ExcelIOUtil.getExportStringValue(row, key);
    assertEquals("'=cell value", result);
  }

  @Test
  void testGetImportStringValue() {
    String rawValue = "  cellValue";
    String result = ExcelIOUtil.getImportStringValue(rawValue);
    assertEquals("cellValue", result);
  }

  @Test
  void testGetImportStringValueWithEquals() {
    String rawValue = "'=cellValue";
    String result = ExcelIOUtil.getImportStringValue(rawValue);
    assertEquals("=cellValue", result);
  }
}
