package org.molgenis.emx2.web;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileTests {

  private static final Logger logger = LoggerFactory.getLogger(FileTests.class);

  @Test
  public void testFile() throws IOException, InvalidFormatException, InterruptedException {

    String uri = "https://emx2.dev.molgenis.org/pet%20store/api/excel/Pet";

    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri)).GET().build();
    HttpResponse<InputStream> response =
        HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofInputStream());

    FileOutputStream fos = new FileOutputStream("testFile.xlsx");
    fos.write(response.body().readAllBytes());
    fos.close();

    File file = new File("testFile.xlsx");
    file.deleteOnExit();

    List<String> rows = ExelTestUtils.readExcelSheet(file);
    logger.info(rows.get(0));
  }
}
