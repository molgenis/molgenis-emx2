package org.molgenis.emx2.io.rowstore;

import static org.apache.poi.ss.usermodel.CellType.BLANK;
import static org.apache.poi.ss.usermodel.CellType.FORMULA;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Now caches all data. Might want to change to SAX parser for XLSX. */
public class TableStoreForXlsxFile implements TableStore {
  private Path excelFilePath;
  private Map<String, List<Row>> cache;
  private static Logger logger = LoggerFactory.getLogger(TableStoreForXlsxFile.class);

  public TableStoreForXlsxFile(Path excelFilePath) {
    this.excelFilePath = excelFilePath;
  }

  @Override
  public void writeTable(String name, Iterable<Row> rows) {
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
      throw new MolgenisException("Import failed", ioe);
    }
  }

  private void writeRowsToSheet(String name, Iterable<Row> rows, Workbook wb) {

    // get the row columns
    Set<String> columnNames = new LinkedHashSet<>();
    for (Row row : rows) {
      columnNames.addAll(row.getColumnNames());
    }

    // create the sheet
    Sheet sheet = wb.createSheet(name);
    Map<String, Integer> columnNameIndexMap = new LinkedHashMap<>();
    int rowNum = 0;

    // write the data
    for (Row row : rows) {
      // create header row
      if (rowNum == 0) {
        int columnIndex = 0;
        // define the column indexes
        for (String columnName : columnNames) {
          columnNameIndexMap.put(columnName, columnIndex++);
        }
        // write a header row
        org.apache.poi.ss.usermodel.Row excelRow = sheet.createRow(rowNum);
        for (Map.Entry<String, Integer> entry : columnNameIndexMap.entrySet()) {
          excelRow.createCell(entry.getValue()).setCellValue(entry.getKey());
        }
        rowNum++;
      }
      // write a contents row
      org.apache.poi.ss.usermodel.Row excelRow = sheet.createRow(rowNum);
      for (Map.Entry<String, Integer> entry : columnNameIndexMap.entrySet()) {
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
              if (!BLANK.equals(cell.getCellType())) {
                columnNames.put(cell.getColumnIndex(), cell.getStringCellValue());
              }
            }
          }
          // otherwise it is a normal row to be added to result
          else {
            Row row = convertRow(sheetName, columnNames, excelRow);
            // ignore empty lines
            if (row.getValueMap().size() > 0) {
              result.add(row);
            }
          }
        }
        this.cache.put(sheetName, result);
      }
    } catch (IOException ioe) {
      throw new MolgenisException("Import failed", ioe);
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
          "Import failed: Table with name " + name + " not found in Excel file");
    }
    return this.cache.get(name);
  }

  @Override
  public void processTable(String name, RowProcessor processor) {
    processor.process(readTable(name).iterator());
  }

  private Row convertRow(
      String name, Map<Integer, String> columnNames, org.apache.poi.ss.usermodel.Row excelRow)
      throws IOException {
    Row row = new Row();
    for (Cell cell : excelRow) {
      String colName = columnNames.get(cell.getColumnIndex());
      if (colName == null && !BLANK.equals(cell.getCellType())) {
        throw new IOException(
            "Read of table '"
                + name
                + "' failed: column index "
                + cell.getColumnIndex()
                + " has no column name and contains value '"
                + cell.getStringCellValue()
                + "'");
      }

      if (cell.getCellType().equals(FORMULA)) {
        convertCellToRowValue(row, cell, cell.getCachedFormulaResultType(), colName);
      } else {
        convertCellToRowValue(row, cell, cell.getCellType(), colName);
      }
    }
    return row;
  }

  private void convertCellToRowValue(Row row, Cell cell, CellType cellType, String colName) {
    switch (cellType) {
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
        throw new UnsupportedOperationException(
            "Found formula in Excel file; should not happen in this function");
      case BLANK:
        row.set(colName, null);
        break;
      default:
        throw new UnsupportedOperationException(
            "Found unknown type "
                + cellType
                + " in Excel file; should not happen in this function");
    }
  }

  @Override
  public boolean containsTable(String name) {
    if (this.cache == null) {
      this.cache();
    }
    return this.cache.containsKey(name);
  }
}
