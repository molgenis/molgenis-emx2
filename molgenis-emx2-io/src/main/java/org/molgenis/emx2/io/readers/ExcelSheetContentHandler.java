package org.molgenis.emx2.io.readers;

import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler;
import org.apache.poi.xssf.usermodel.XSSFComment;

public class ExcelSheetContentHandler implements SheetContentsHandler {
  private int currentRow = -1;
  private int currentCol = -1;

  @Override
  public void startRow(int i) {}

  @Override
  public void endRow(int i) {}

  @Override
  public void cell(String cellReference, String formattedValue, XSSFComment comment) {

    // gracefully handle missing CellRef here in a similar way as XSSFCell does
    if (cellReference == null) {
      cellReference = new CellAddress(currentRow, currentCol).formatAsString();
    }
  }
}
