package org.molgenis.emx2.io.tablestore;

import static org.molgenis.emx2.io.FileUtils.getTempFile;

import com.github.pjfanning.xlsx.StreamingReader;
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
import org.molgenis.emx2.Row;

/** Now caches all data. Might want to change to SAX parser for XLSX. */
public class TableStoreForXlsxFile implements TableStore {
  public static final int ROW_ACCESS_WINDOW_SIZE = 100;
  private final Path excelFilePath;
  private List<String> sheetNames;

  public TableStoreForXlsxFile(Path excelFilePath) {
    this.excelFilePath = excelFilePath;
  }

  @Override
  public Collection<String> getTableNames() {
    if (sheetNames == null) {
      try (InputStream is = new FileInputStream(excelFilePath.toFile());
          Workbook workbook =
              StreamingReader.builder()
                  .rowCacheSize(ROW_ACCESS_WINDOW_SIZE)
                  .bufferSize(4096)
                  .open(is)) {
        sheetNames = new ArrayList<>();
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
          sheetNames.add(workbook.getSheetName(i));
        }
        return sheetNames;
      } catch (Throwable e) {
        throw new MolgenisException("Error reading excel file", new Exception(e));
      }
    }
    return sheetNames;
  }

  @Override
  public void writeTable(String name, List<String> columnNames, Iterable<Row> rows) {
    SXSSFWorkbook wb;
    try {
      if (name.length() > 30)
        throw new IOException("Excel sheet name '" + name + "' is too long. Maximum 30 characters");
      // streaming workbook
      if (!Files.exists(excelFilePath)) {
        wb = new SXSSFWorkbook(ROW_ACCESS_WINDOW_SIZE);
      } else {
        // move to a temp file, so we can merge result into the original file location
        File tempFile = getTempFile("temp", ".xlsx");
        Path temp =
            Files.move(excelFilePath, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        wb = new SXSSFWorkbook(new XSSFWorkbook(temp.toFile()), ROW_ACCESS_WINDOW_SIZE);
      }
      if (rows.iterator().hasNext()) {
        writeRowsToSheet(name, columnNames, rows, wb);
      } else {
        writeHeaderOnlyToSheet(name, columnNames, wb);
      }
      // write contents to a temp file and overwrite original
      try (FileOutputStream outputStream = new FileOutputStream(excelFilePath.toFile())) {
        wb.write(outputStream);
      } finally {
        wb.close();
      }
    } catch (Exception ife) {
      throw new MolgenisException("Writing of excel failed", ife);
    }
  }

  private void writeHeaderOnlyToSheet(String name, List<String> columnNames, Workbook wb) {
    Sheet sheet = wb.createSheet(name);
    org.apache.poi.ss.usermodel.Row excelRow = sheet.createRow(0);
    for (int i = 0; i < columnNames.size(); i++) {
      excelRow.createCell(i).setCellValue(columnNames.get(i));
    }
  }

  private void writeRowsToSheet(
      String name, List<String> columnNames, Iterable<Row> rows, SXSSFWorkbook wb)
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
    sheet.flushRows();
  }

  @Override
  public AutoCloseableIterable<Row> readTable(String name) {
    return new WorkbookRowIterable(name);
  }

  @Override
  public void processTable(String name, RowProcessor processor) {
    processor.process(readTable(name).iterator(), this);
  }

  @Override
  public boolean containsTable(String name) {
    return this.getTableNames().contains(name);
  }

  public class WorkbookRowIterable implements AutoCloseableIterable<Row> {
    private final Workbook workbook;
    private final InputStream is;
    private final String tableName;

    public WorkbookRowIterable(String tableName) {
      this.tableName = tableName;
      try {
        is = new FileInputStream(excelFilePath.toFile());
        workbook =
            StreamingReader.builder()
                .rowCacheSize(ROW_ACCESS_WINDOW_SIZE)
                .bufferSize(4096)
                .open(is);
      } catch (Exception e) {
        throw new MolgenisException("Read of excel file failed", e);
      }
    }

    @Override
    public Iterator<Row> iterator() {
      try {
        Sheet sheet = workbook.getSheet(tableName);
        return new RowIterator(this, this.tableName, sheet.iterator());
      } catch (Exception e) {
        throw new MolgenisException("Failed to read sheet '" + tableName + "'.", e);
      }
    }

    @Override
    public void close() {
      try {
        if (workbook != null) {
          workbook.close();
        }
        if (is != null) {
          is.close();
        }
      } catch (Exception e) {
        throw new MolgenisException("Couldn't close workbook");
      }
    }

    private static class RowIterator implements Iterator<Row> {
      private final Iterator<org.apache.poi.ss.usermodel.Row> iterator;
      private final Map<Integer, String> columnNames;
      private final String tableName;
      private final WorkbookRowIterable container;

      public RowIterator(
          WorkbookRowIterable container,
          String tableName,
          Iterator<org.apache.poi.ss.usermodel.Row> iterator) {
        this.tableName = tableName;
        this.iterator = iterator;
        this.container = container;
        columnNames = new LinkedHashMap<>();

        if (iterator.hasNext()) {
          for (Cell cell : iterator.next()) {
            String value = cell.getStringCellValue();
            if (value != null) {
              value = value.trim();
            }
            columnNames.put(cell.getColumnIndex(), value);
          }
        } else {
          container.close();
        }
      }

      @Override
      public boolean hasNext() {
        if (!iterator.hasNext()) {
          container.close();
          return false;
        } else {
          return true;
        }
      }

      @Override
      public Row next() {
        return convertRow(tableName, columnNames, iterator.next());
      }

      private Row convertRow(
          String name, Map<Integer, String> columnNames, org.apache.poi.ss.usermodel.Row excelRow) {
        Row row = new Row();
        for (Cell cell : excelRow) {
          String colName = columnNames.get(cell.getColumnIndex());
          if (!cell.getStringCellValue().trim().isEmpty()) {
            if (colName == null) {
              throw new MolgenisException(
                  "Read of table '"
                      + name
                      + "' failed: column index "
                      + cell.getColumnIndex()
                      + " has no column name and contains value '"
                      + cell.getStringCellValue()
                      + "'");
            }
            row.set(colName, getTypedCellValue(cell));
          }
        }
        return row;
      }

      private Object getTypedCellValue(Cell cell) {

        switch (cell.getCellType()) {
          case BLANK:
            return null;
          case NUMERIC:
            // check format string to see if it might be a date or datetime
            String format = cell.getCellStyle().getDataFormatString();
            if (format.contains("d") && format.contains("m") && format.contains("y")) {
              return DateUtil.getLocalDateTime(cell.getNumericCellValue());
            } else {
              // Check if the numeric value has a fractional part
              double numericValue = cell.getNumericCellValue();
              if (numericValue % 1 == 0) {
                // It's a long!
                return (long) numericValue;
              } else {
                // Otherwise, treat it as decimal
                return numericValue;
              }
            }
          case STRING:
            return cell.getStringCellValue().trim();
          case BOOLEAN:
            return cell.getBooleanCellValue();
          case FORMULA:
            throw new UnsupportedOperationException(
                "Found formula in Excel file; currently not supported");
          default:
            throw new UnsupportedOperationException(
                "Found unknown type "
                    + cell.getCellType()
                    + " in Excel file; should not happen in this function");
        }
      }
    }
  }
}
