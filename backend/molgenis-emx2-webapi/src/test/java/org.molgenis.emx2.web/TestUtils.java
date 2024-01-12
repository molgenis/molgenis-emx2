package org.molgenis.emx2.web;

import io.restassured.response.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.LocalFileHeader;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class TestUtils {
  private TestUtils() {}

  public static List<String> readExcelSheet(File file) throws IOException {
    FileInputStream fip = new FileInputStream(file);
    return TestUtils.readExcelSheet(fip);
  }

  public static List<String> readExcelSheet(InputStream inputStream) throws IOException {
    List<String> result = new ArrayList<>();
    XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
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

  public static File responseToFile(Response response) throws IOException, InterruptedException {

    String contentDisposition = response.getHeader("content-disposition");
    String fileName = contentDisposition.substring(21);
    FileOutputStream fos = new FileOutputStream(fileName);

    fos.write(response.getBody().asByteArray());
    fos.close();

    File file = new File(fileName);
    file.deleteOnExit();

    return file;
  }

  public static List<File> extractFileFromZip(File zipfile) throws IOException {
    List<File> files = new ArrayList<File>();
    LocalFileHeader localFileHeader;
    int readLen;
    byte[] readBuffer = new byte[4096];

    InputStream inputStream = new FileInputStream(zipfile);
    try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
      while ((localFileHeader = zipInputStream.getNextEntry()) != null) {
        File extractedFile = new File(localFileHeader.getFileName());
        extractedFile.deleteOnExit();
        try (OutputStream outputStream = new FileOutputStream(extractedFile)) {
          while ((readLen = zipInputStream.read(readBuffer)) != -1) {
            outputStream.write(readBuffer, 0, readLen);
          }
        }
        files.add(extractedFile);
      }
    }
    return files;
  }
}
