package org.molgenis.emx2.io.tablestore;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Row;

class TableStoreForXlsxFileTest {

  @Test
  void testGetCellValue() {
    Row row = mock(Row.class);
    Map.Entry<String, Integer> entry = mock();
    String key = "keyOfRow";
    when(entry.getKey()).thenReturn(key);
    when(row.getString(key)).thenReturn("cell value");
    String result = TableStoreForXlsxFile.getCellValue(row, entry);
    assertEquals("cell value", result);
  }

  @Test
  void testGetCellValueWithEquals() {
    Row row = mock(Row.class);
    Map.Entry<String, Integer> entry = mock();
    String key = "keyOfRow";
    when(entry.getKey()).thenReturn(key);
    when(row.getString(key)).thenReturn("=cell value");
    String result = TableStoreForXlsxFile.getCellValue(row, entry);
    assertEquals("'=cell value", result);
  }

  @Test
  void testGetStringValue() {
    Cell cell = mock(Cell.class);
    when(cell.getStringCellValue()).thenReturn("  cellValue");
    String result = TableStoreForXlsxFile.getStringValue(cell);
    assertEquals("cellValue", result);
  }

  @Test
  void testGetStringValueWithEquals() {
    Cell cell = mock(Cell.class);
    when(cell.getStringCellValue()).thenReturn("'=cellValue");
    String result = TableStoreForXlsxFile.getStringValue(cell);
    assertEquals("=cellValue", result);
  }
}
