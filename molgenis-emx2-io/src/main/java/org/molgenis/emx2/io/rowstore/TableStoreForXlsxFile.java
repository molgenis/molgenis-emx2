package org.molgenis.emx2.io.rowstore;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.io.ErrorCodes;
import org.molgenis.emx2.utils.MolgenisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.*;

/** Now caches all data. Might want to change to SAX parser for XLSX. */
public class TableStoreForXlsxFile implements TableStore {
  private Path excelFilePath;
  private Map<String, List<Row>> cache;
  private static Logger logger = LoggerFactory.getLogger(TableStoreForXlsxFile.class);

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
          this.cache = null;
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

  private void cache() {
    long start = System.currentTimeMillis();
    try (Workbook wb = WorkbookFactory.create(excelFilePath.toFile())) {
      this.cache = new LinkedHashMap<>();
      for (Sheet sheet : wb) {
        String sheetName = sheet.getSheetName();
        List<Row> result = new ArrayList<>();
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
            Row row = convertRow(sheetName, columnNames, excelRow);
            result.add(row);
          }
        }
        this.cache.put(sheetName, result);
      }
    } catch (IOException ioe) {
      throw new MolgenisException(
          ErrorCodes.IO_EXCEPTION, ErrorCodes.IO_EXCEPTION_MESSAGE, ioe.getMessage(), ioe);
    }
    if (logger.isInfoEnabled()) {
      logger.info("Excel file loaded into memory in {}ms", (System.currentTimeMillis() - start));
    }
  }

  @Override
  public List<Row> readTable(String name) {
    if (this.cache == null) {
      this.cache();
    }
    if (!this.cache.containsKey(name)) {
      throw new MolgenisException(
          ErrorCodes.NOT_FOUND,
          ErrorCodes.NOT_FOUND_MESSAGE,
          "Table with name " + name + " not found in Excel file");
    }
    return this.cache.get(name);
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
    if (this.cache == null) {
      this.cache();
    }
    return this.cache.containsKey(name);
  }
}
