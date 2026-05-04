package org.molgenis.emx2.io;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Row;

class TestExcelIOUtil {

  @Test
  void testToExcelFormat() {
    Row row = new Row();
    String key = "keyOfRow";
    row.setString(key, "cell value");
    String result = ExcelIOUtil.toExcelFormat(row, key);
    assertEquals("cell value", result);
  }

  @Test
  void testToExcelFormatWithEquals() {
    Row row = new Row();
    String key = "keyOfRow";
    row.setString(key, "=cell value");
    String result = ExcelIOUtil.toExcelFormat(row, key);
    assertEquals("'=cell value", result);
  }

  @Test
  void testFromExcelFormat() {
    String rawValue = "  cellValue";
    String result = ExcelIOUtil.fromExcelFormat(rawValue);
    assertEquals("cellValue", result);
  }

  @Test
  void testFromExcelFormatWithEquals() {
    String rawValue = "'=cellValue";
    String result = ExcelIOUtil.fromExcelFormat(rawValue);
    assertEquals("=cellValue", result);
  }
}
