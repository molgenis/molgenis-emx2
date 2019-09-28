package org.molgenis.emx2.io.stores;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.molgenis.emx2.Row;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RowStoreForXlsxFile implements RowStore {
  private Path excelFilePath;

  public RowStoreForXlsxFile(Path excelFilePath) {
    this.excelFilePath = excelFilePath;
  }

  @Override
  public void write(String name, List<Row> rows) throws IOException {
    if (rows.isEmpty()) return;

    if (name.length() > 30)
      throw new IOException("Excel sheet name '" + name + "' is too long. Maximum 30 characters");

    if (!Files.exists(excelFilePath)) {
      try (FileOutputStream out = new FileOutputStream(excelFilePath.toFile());
          Workbook wb = new XSSFWorkbook()) {
        writeRowsToSheet(name, rows, wb);
        wb.write(out);
      }
    } else {
      Workbook wb;
      try (FileInputStream inputStream = new FileInputStream(excelFilePath.toFile())) {
        wb = WorkbookFactory.create(inputStream);
        writeRowsToSheet(name, rows, wb);
      }
      try (FileOutputStream outputStream = new FileOutputStream(excelFilePath.toFile())) {
        wb.write(outputStream);
      } finally {
        wb.close();
      }
    }
  }

  private void writeRowsToSheet(String name, List<Row> rows, Workbook wb) {

    Sheet sheet = wb.createSheet(name);
    Map<String, Integer> columNames = new LinkedHashMap<>();
    int rowNum = 0;
    for (Row row : rows) {
      // create header row
      if (rowNum == 0) {
        int columnIndex = 0;
        // define the column indexes
        for (String columnName : row.getColumnNames()) {
          columNames.put(columnName, columnIndex++);
        }
        // write a header row
        org.apache.poi.ss.usermodel.Row excelRow = sheet.createRow(rowNum);
        for (Map.Entry<String, Integer> entry : columNames.entrySet()) {
          excelRow.createCell(entry.getValue()).setCellValue(entry.getKey());
        }
        rowNum++;
      }
      // write a contents row
      org.apache.poi.ss.usermodel.Row excelRow = sheet.createRow(rowNum);
      for (Map.Entry<String, Integer> entry : columNames.entrySet()) {
        excelRow.createCell(entry.getValue()).setCellValue(row.getString(entry.getKey()));
      }
      rowNum++;
    }
  }

  @Override
  public List<Row> read(String name) throws IOException {
    List<Row> result = new ArrayList<>();
    try (InputStream inp = new FileInputStream(excelFilePath.toFile());
        Workbook wb = WorkbookFactory.create(inp)) {
      Sheet sheet = wb.getSheet(name);
      if (sheet == null) throw new IOException("Sheet '" + name + "' not found in Excel file");

      Map<Integer, String> columnNames = null;
      for (org.apache.poi.ss.usermodel.Row excelRow : sheet) {

        // first non-empty row is column names
        if (columnNames == null) {
          columnNames = new LinkedHashMap<>();
          for (Cell cell : excelRow) {
            if (!CellType.BLANK.equals(cell.getCellType())) {
              columnNames.put(cell.getColumnIndex(), cell.getStringCellValue());
            }
          }
        }
        // otherwise it is a normal row to be added to result
        else {
          Row row = convertRow(name, columnNames, excelRow);
          result.add(row);
        }
      }
      return result;
    }
  }

  private Row convertRow(
      String name, Map<Integer, String> columnNames, org.apache.poi.ss.usermodel.Row excelRow)
      throws IOException {
    Row row = new Row();
    for (Cell cell : excelRow) {
      String colName = columnNames.get(cell.getColumnIndex());
      if (colName == null)
        throw new IOException(
            "Read of "
                + name
                + " failed: column index "
                + cell.getColumnIndex()
                + " has no column name");

      switch (cell.getCellType()) {
        case STRING:
          row.set(colName, cell.getRichStringCellValue().getString());
          break;
        case NUMERIC:
          if (DateUtil.isCellDateFormatted(cell)) {
            row.setDate(
                colName,
                cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
          } else {
            row.setDecimal(colName, cell.getNumericCellValue());
          }
          break;
        case BOOLEAN:
          row.setBool(colName, cell.getBooleanCellValue());
          break;
        case FORMULA:
          throw new IOException("formula's not supported");
        case BLANK:
          break;
        default:
      }
    }
    return row;
  }

  @Override
  public boolean containsTable(String name) throws IOException {
    try (InputStream inp = new FileInputStream(excelFilePath.toFile());
        Workbook wb = WorkbookFactory.create(inp)) {
      if (wb.getSheet(name) != null) return true;
    }
    return false;
  }
}
