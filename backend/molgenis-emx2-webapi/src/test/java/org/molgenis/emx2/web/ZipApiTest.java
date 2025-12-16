package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.web.Constants.*;

import io.restassured.response.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@Tag("slow")
@ExtendWith(SystemStubsExtension.class)
class ZipApiTest extends ApiTestBase {

  @Test
  void testCsvApi_zipUploadDownload() throws IOException {
    // get original schema
    String schemaCsv = getCsv();

    // create a new schema for zip
    db.dropCreateSchema("pet store zip");

    // download zip contents of old schema
    byte[] zipContents =
        given()
            .sessionId(sessionId)
            .accept(ACCEPT_ZIP)
            .when()
            .get("/pet store/api/zip")
            .asByteArray();

    // upload zip contents into new schema
    File zipFile = createTempFile(zipContents, ".zip");
    given()
        .sessionId(sessionId)
        .multiPart(zipFile)
        .when()
        .post("/pet store zip/api/zip")
        .then()
        .statusCode(200);

    // check if schema equal using json representation
    String schemaCsv2 = getCsv();

    assertArrayEquals(toSortedArray(schemaCsv), toSortedArray(schemaCsv2));

    // delete the new schema
    db.dropSchema("pet store zip");
  }

  private String[] toSortedArray(String string) {
    String[] lines = string.split("\n");
    Arrays.sort(lines);
    return lines;
  }

  @Test
  void shouldDownloadFilesInZip() throws IOException, InterruptedException {
    Response response =
        given().sessionId(sessionId).accept(ACCEPT_ZIP).when().get("/pet store/api/zip");

    File zip = TestUtils.responseToFile(response);
    try (ZipFile zipFile = new ZipFile(zip)) {
      assertZipContainsCsvEntries(
          zipFile,
          "Category.csv",
          "molgenis.csv",
          "molgenis_settings.csv",
          "Order.csv",
          "Pet.csv",
          "Tag.csv",
          "User.csv");
    }
  }

  @Test
  void whenMembersIncluded_thenIncludeMembers() throws IOException, InterruptedException {
    Response response = request(ACCEPT_ZIP, "/pet store/api/zip", Map.of(INCLUDE_MEMBERS, true));
    File zip = TestUtils.responseToFile(response);
    try (ZipFile zipFile = new ZipFile(zip)) {

      assertZipContainsCsvEntries(
          zipFile,
          "Category.csv",
          "molgenis.csv",
          "molgenis_members.csv",
          "molgenis_settings.csv",
          "Order.csv",
          "Pet.csv",
          "Tag.csv",
          "User.csv");
    }
  }

  @Test
  void whenSystemColumnsSpecified_thenIncludeSystemColumns()
      throws IOException, InterruptedException {
    Response response = request(ACCEPT_ZIP, "/pet store/api/zip", Map.of(INCLUDE_SYSTEM_COLUMNS, true));
    File file = TestUtils.responseToFile(response);
    List<File> files = TestUtils.extractFileFromZip(file);
    var categoryFile =
        files.stream().filter(f -> f.getName().equals("Category.csv")).findFirst().orElseThrow();
    String result = Files.readString(categoryFile.toPath());
    assertTrue(result.contains("mg_"));
  }

  @Test
  void downloadZipTable() throws IOException, InterruptedException {
    Response response = request(ACCEPT_ZIP, "/pet store/api/zip/Pet");
    File file = TestUtils.responseToFile(response);
    List<File> files = TestUtils.extractFileFromZip(file);
    String result = Files.readString(files.get(0).toPath());
    assertTrue(result.contains("name,category,photoUrls,status,tags,weight"));
    assertTrue(result.contains("pooky,cat,,available,,9.4"));
  }

  @Test
  void downloadZipTableWithSystemColumns() throws IOException, InterruptedException {
    Response response = request(ACCEPT_ZIP, "/pet store/api/zip", Map.of(INCLUDE_SYSTEM_COLUMNS, true));
    File file = TestUtils.responseToFile(response);
    List<File> files = TestUtils.extractFileFromZip(file);
    String result = Files.readString(files.get(0).toPath());
    assertTrue(result.contains("mg_"));
  }


  private void assertZipContainsCsvEntries(ZipFile zipFile, String... entries) {
    List<String> expectedEntries = List.of(entries);
    List<String> csvFileNames =
        zipFile.stream().map(ZipEntry::getName).filter(name -> name.endsWith(".csv")).toList();

    assertTrue(
        csvFileNames.containsAll(expectedEntries) && expectedEntries.containsAll(csvFileNames));
  }

  private File createTempFile(byte[] zipContents, String extension) throws IOException {
    File tempFile = File.createTempFile("some", extension);
    tempFile.deleteOnExit();
    OutputStream os = new FileOutputStream(tempFile);
    os.write(zipContents);
    os.flush();
    os.close();
    return tempFile;
  }

  private String getCsv() {
    return given().sessionId(sessionId).accept(ACCEPT_CSV).expect().statusCode(200).when().get("/pet store/api/csv").asString();
  }

  private Response request(String accept, String url) {
    return request(accept, url, Map.of());
  }

  private Response request(String accept, String url, Map<String, Object> params) {
    RequestSpecification request = given().sessionId(sessionId).accept(accept).when();
    params.forEach(request::param);
    return request.get(url);
  }

}
