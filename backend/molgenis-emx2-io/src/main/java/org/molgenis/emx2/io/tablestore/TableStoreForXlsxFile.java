package org.molgenis.emx2.io.tablestore;

import static org.apache.poi.ss.usermodel.CellType.BLANK;
import static org.apache.poi.ss.usermodel.CellType.FORMULA;
import static org.molgenis.emx2.io.FileUtils.getTempFile;

import com.monitorjbl.xlsx.StreamingReader;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.NameMapper;
import org.molgenis.emx2.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Now caches all data. Might want to change to SAX parser for XLSX. */
public class TableStoreForXlsxFile implements TableStore {
  public static final int ROW_ACCESS_WINDOW_SIZE = 100;
  private Path excelFilePath;
  private Map<String, List<Row>> cache;
  private static Logger logger = LoggerFactory.getLogger(TableStoreForXlsxFile.class);

  public TableStoreForXlsxFile(Path excelFilePath) {
    this.excelFilePath = excelFilePath;
  }

  @Override
  public Collection<String> tableNames() {
    if (this.cache == null) {
      this.cache();
    }
    return this.cache.keySet();
  }

  @Override
  public void writeTable(
      String name, List<String> columnNames, NameMapper nameMapper, Iterable<Row> rows) {
    SXSSFWorkbook wb;
    try {
      if (name.length() > 30)
        throw new IOException("Excel sheet name '" + name + "' is too long. Maximum 30 characters");
      // streaming workbook
      if (!Files.exists(excelFilePath)) {
        wb = new SXSSFWorkbook(ROW_ACCESS_WINDOW_SIZE);
      } else {
        // move to a temp file so we can merge result into the original file location
        File tempFile = getTempFile("temp", ".xlsx");
        Path temp =
            Files.move(excelFilePath, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        wb = new SXSSFWorkbook(new XSSFWorkbook(temp.toFile()), ROW_ACCESS_WINDOW_SIZE);
      }
      if (rows.iterator().hasNext()) {
        writeRowsToSheet(name, columnNames, nameMapper, rows, wb);
      } else {
        writeHeaderOnlyToSheet(name, columnNames, nameMapper, wb);
      }
      // write contents to a temp file and overwrite original
      try (FileOutputStream outputStream = new FileOutputStream(excelFilePath.toFile())) {
        wb.write(outputStream);
      } finally {
        wb.close();
      }
    } catch (Exception ife) {
      throw new MolgenisException("Writing of excel failed", ife);
    } finally {
      this.cache = null;
    }
  }

  private void writeHeaderOnlyToSheet(
      String name, List<String> columnNames, NameMapper nameMapper, Workbook wb) {
    Sheet sheet = wb.createSheet(name);
    org.apache.poi.ss.usermodel.Row excelRow = sheet.createRow(0);
    for (int i = 0; i < columnNames.size(); i++) {
      excelRow.createCell(i).setCellValue(nameMapper.map(columnNames.get(i)));
    }
  }

  private void writeRowsToSheet(
      String name,
      List<String> columnNames,
      NameMapper nameMapper,
      Iterable<Row> rows,
      SXSSFWorkbook wb)
      throws IOException {

    // create the sheet
    SXSSFSheet sheet = wb.createSheet(name);
    // buffer the row so it gets flushed
    sheet.setRandomAccessWindowSize(wb.getRandomAccessWindowSize());
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
          excelRow.createCell(entry.getValue()).setCellValue(nameMapper.map(entry.getKey()));
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
    sheet.flushRows();
  }

  private void cache() {
    long start = System.currentTimeMillis();
    try (InputStream is = new FileInputStream(excelFilePath.toFile());
        Workbook workbook =
            StreamingReader.builder()
                .rowCacheSize(ROW_ACCESS_WINDOW_SIZE)
                .bufferSize(4096)
                .open(is)) {
      this.cache = new LinkedHashMap<>();
      for (Sheet sheet : workbook) {
        String sheetName = sheet.getSheetName();
        List<Row> result = new ArrayList<>();
        Map<Integer, String> columnNames = null;
        for (org.apache.poi.ss.usermodel.Row excelRow : sheet) {
          // first non-empty row is column names
          if (columnNames == null) {
            columnNames = new LinkedHashMap<>();
            for (Cell cell : excelRow) {
              if (!BLANK.equals(cell.getCellType())) {
                String value = cell.getStringCellValue();
                if (value != null) {
                  value = value.trim();
                }
                columnNames.put(cell.getColumnIndex(), value);
              }
            }
          }
          // otherwise it is a normal row to be added to result
          else {
            Row row = convertRow(sheetName, columnNames, excelRow);
            // ignore empty lines
            if (notEmptyLine(row)) {
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

  private static boolean notEmptyLine(Row row) {
    for (String name : row.getColumnNames()) {
      if (row.notNull(name)) return true;
    }
    return false;
  }

  @Override
  public List<Row> readTable(String name) {
    return readTable(name, null);
  }

  @Override
  public List<Row> readTable(String name, NameMapper mapper) {
    if (this.cache == null) {
      this.cache();
    }
    if (!this.cache.containsKey(name)) {
      throw new MolgenisException(
          "Import failed: Table with name " + name + " not found in Excel file");
    }
    return this.cache.get(name).stream().map(row -> new Row(row, mapper)).toList();
  }

  @Override
  public void processTable(String name, NameMapper mapper, RowProcessor processor) {
    processor.process(readTable(name, mapper).iterator(), this);
  }

  private Row convertRow(
      String name, Map<Integer, String> columnNames, org.apache.poi.ss.usermodel.Row excelRow)
      throws IOException {
    Row row = new Row();
    for (Cell cell : excelRow) {
      String colName = columnNames.get(cell.getColumnIndex());
      if (!cell.getStringCellValue().trim().equals("")) {
        if (colName == null) {
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
    }
    return row;
  }

  private void convertCellToRowValue(Row row, Cell cell, CellType cellType, String colName) {
    switch (cellType) {
      case BLANK:
        row.set(colName, null);
        break;
      case STRING, NUMERIC:
        // don't use the auto guessing of Excel; we will do the cast ourselves
        row.setString(colName, cell.getStringCellValue().trim());
        break;
      case BOOLEAN:
        row.setBool(colName, cell.getBooleanCellValue());
        break;
      case FORMULA:
        throw new UnsupportedOperationException(
            "Found formula in Excel file; should not happen in this function");
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
