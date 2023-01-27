package org.molgenis.emx2.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
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

  public static File downLoadFile(String apiGetRequest) throws IOException, InterruptedException {

    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(apiGetRequest)).GET().build();
    HttpResponse<InputStream> response =
        HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofInputStream());

    Optional<String> s = response.headers().firstValue("content-disposition");
    if (s.isEmpty()) {
      return null;
    }
    String fileName = s.get().substring(21);
    FileOutputStream fos = new FileOutputStream(fileName);

    fos.write(response.body().readAllBytes());
    fos.close();

    File file = new File(fileName);
    file.deleteOnExit();

    return file;
  }
}
