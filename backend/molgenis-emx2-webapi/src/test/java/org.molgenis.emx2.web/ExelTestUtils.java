package org.molgenis.emx2.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExelTestUtils {
  private ExelTestUtils() {}

  public static List<String> readExcelSheet(File file) throws IOException, InvalidFormatException {
    List<String> result = new ArrayList<>();
    // Create a FileInputStream object
    // for getting the information of the file
    FileInputStream fip = new FileInputStream(file);
    XSSFWorkbook workbook = new XSSFWorkbook(fip);
    XSSFSheet selSheet = workbook.getSheetAt(0);
    // Loop through all the rows
    for (Row row : selSheet) {
      // Loop through all rows and add ","
      Iterator<Cell> cellIterator = row.cellIterator();
      StringBuilder stringBuffer = new StringBuilder();
      while (cellIterator.hasNext()) {
        Cell cell = cellIterator.next();
        if (stringBuffer.length() != 0) {
          stringBuffer.append(",");
        }
        stringBuffer.append(cell.getStringCellValue());
      }
      result.add(stringBuffer.toString());
    }
    workbook.close();
    return result;
  }
}
