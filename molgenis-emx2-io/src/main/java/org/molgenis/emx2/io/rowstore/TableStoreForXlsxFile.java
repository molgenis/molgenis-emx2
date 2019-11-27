package org.molgenis.emx2.io.rowstore;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.ErrorCodes;
import org.molgenis.emx2.utils.MolgenisException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.*;

public class TableStoreForXlsxFile implements TableStore {
  private Path excelFilePath;

  public TableStoreForXlsxFile(Path excelFilePath) {
    this.excelFilePath = excelFilePath;
  }

  @Override
  public void writeTable(String name, List<Row> rows) {
    if (rows.isEmpty()) return;
    try {
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
    } catch (IOException ioe) {
      throw new MolgenisException(
          ErrorCodes.IO_EXCEPTION, ErrorCodes.IO_EXCEPTION_MESSAGE, ioe.getMessage(), ioe);
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
  public List<Row> readTable(String name) {
    List<Row> result = new ArrayList<>();
    try (Workbook wb = WorkbookFactory.create(excelFilePath.toFile())) {
      Sheet sheet = wb.getSheet(name);
      if (sheet == null) throw new IOException("Sheet '" + name + "' not found in Excel file");

      Map<Integer, String> columnNames = null;
      Iterator<org.apache.poi.ss.usermodel.Row> rowIterator = sheet.rowIterator();
      while (rowIterator.hasNext()) {
        org.apache.poi.ss.usermodel.Row excelRow = rowIterator.next();
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
    } catch (IOException ioe) {
      throw new MolgenisException(
          ErrorCodes.NOT_FOUND,
          ErrorCodes.NOT_FOUND_MESSAGE,
          "CsvStringStore with name '" + name + "' doesn't exist." + ioe.getMessage(),
          ioe);
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
  public boolean containsTable(String name) {
    try (Workbook wb = WorkbookFactory.create(excelFilePath.toFile())) {
      if (wb.getSheet(name) != null) return true;
    } catch (IOException ioe) {
      throw new MolgenisException(
          ErrorCodes.IO_EXCEPTION, ErrorCodes.IO_EXCEPTION_MESSAGE, ioe.getMessage(), ioe);
    }
    return false;
  }
}
