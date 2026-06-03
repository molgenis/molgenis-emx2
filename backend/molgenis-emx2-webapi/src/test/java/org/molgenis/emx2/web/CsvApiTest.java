package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.Constants.ANONYMOUS;
import static org.molgenis.emx2.Constants.IS_CHANGELOG_ENABLED;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.datamodels.DataModels.Profile.PET_STORE;
import static org.molgenis.emx2.web.Constants.ACCEPT_CSV;
import static org.molgenis.emx2.web.Constants.ACCEPT_ZIP;

import io.restassured.response.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;

class CsvApiTest extends ApiTestBase {

  private static final String SCHEMA_NAME = CsvApiTest.class.getSimpleName();

  public static final String TABLE_NAME_WITH_SPACES = "table with spaces";

  public static final String PET_SHOP_OWNER = "pet_shop_owner";
  public static final String PET_SHOP_VIEWER = "shopviewer";
  public static final String PET_SHOP_MANAGER = "shopmanager";

  @BeforeAll
  static void setupSchema() {
    database.dropSchemaIfExists(SCHEMA_NAME);
    PET_STORE.getImportTask(database, SCHEMA_NAME, "CSV API test database", true).run();
    Schema schema = database.getSchema(SCHEMA_NAME);
    schema.create(table(TABLE_NAME_WITH_SPACES, column("name", STRING).setKey(1)));

    login("admin", "admin");
  }

  @Test
  void shouldUploadAndDownload() throws IOException {
    // create a new schema for complete csv data round trip
    String schemaName = SCHEMA_NAME + "roundTrip";
    Schema schema = database.dropCreateSchema(schemaName);
    if (schema.getTable(TABLE_NAME_WITH_SPACES) == null) {
      schema.create(table(TABLE_NAME_WITH_SPACES, column("name", STRING).setKey(1)));
    }

    // download csv metadata and data from existing schema
    byte[] contentsMeta = getContentAsByteArray(ACCEPT_CSV, "/" + SCHEMA_NAME + "/api/csv");
    byte[] contentsCategoryData =
        getContentAsByteArray(ACCEPT_CSV, "/" + SCHEMA_NAME + "/api/csv/Category");
    byte[] contentsOrderData =
        getContentAsByteArray(ACCEPT_CSV, "/" + SCHEMA_NAME + "/api/csv/Order");
    byte[] contentsPetData = getContentAsByteArray(ACCEPT_CSV, "/" + SCHEMA_NAME + "/api/csv/Pet");
    byte[] contentsUserData =
        getContentAsByteArray(ACCEPT_CSV, "/" + SCHEMA_NAME + "/api/csv/User");
    byte[] contentsTagData = getContentAsByteArray(ACCEPT_CSV, "/" + SCHEMA_NAME + "/api/csv/Tag");
    byte[] contentsTableWithSpacesData =
        getContentAsByteArray(ACCEPT_CSV, "/" + SCHEMA_NAME + "/api/csv/" + TABLE_NAME_WITH_SPACES);

    // create tmp files for csv metadata and data
    File contentsMetaFile = createTempFile(contentsMeta, ".csv");
    File contentsCategoryDataFile = createTempFile(contentsCategoryData, ".csv");
    File contentsOrderDataFile = createTempFile(contentsOrderData, ".csv");
    File contentsPetDataFile = createTempFile(contentsPetData, ".csv");
    File contentsUserDataFile = createTempFile(contentsUserData, ".csv");
    File contentsTagDataFile = createTempFile(contentsTagData, ".csv");
    File contentsTableWithSpacesDataFile = createTempFile(contentsTableWithSpacesData, ".csv");

    // upload csv metadata and data into the new schema
    // here we use 'body' (instead of 'multiPart' in e.g. testCsvApi_zipUploadDownload) because csv,
    // json and yaml import is submitted in the request body
    acceptFileUpload(contentsMetaFile, schemaName, "molgenis", false);
    acceptFileUpload(contentsCategoryDataFile, schemaName, "Category", false);
    acceptFileUpload(contentsTagDataFile, schemaName, "Tag", false);
    acceptFileUpload(contentsPetDataFile, schemaName, "Pet", false);
    acceptFileUpload(contentsUserDataFile, schemaName, "User", false);
    acceptFileUpload(contentsOrderDataFile, schemaName, "Order", false);
    acceptFileUpload(contentsTableWithSpacesDataFile, schemaName, TABLE_NAME_WITH_SPACES, false);

    // download csv from the new schema
    String contentsMetaNew = getContentAsString(schemaName + "/api/csv");
    String contentsCategoryDataNew = getContentAsString(schemaName + "/api/csv/Category");
    String contentsPetDataNew = getContentAsString(schemaName + "/api/csv/Pet");
    String contentsUserDataNew = getContentAsString(schemaName + "/api/csv/User");
    String contentsTagDataNew = getContentAsString(schemaName + "/api/csv/Tag");
    String contentsTableWithSpacesDataNew =
        getContentAsString(
            schemaName
                + "/api/csv/"
                + TABLE_NAME_WITH_SPACES.toUpperCase()); // -o test for case-insensitive match

    // test if existing and new schema are equal
    assertArrayEquals(toSortedArray(new String(contentsMeta)), toSortedArray(contentsMetaNew));
    assertArrayEquals(
        toSortedArray(new String(contentsCategoryData)), toSortedArray(contentsCategoryDataNew));
    assertArrayEquals(
        toSortedArray(new String(contentsPetData)), toSortedArray(contentsPetDataNew));
    assertArrayEquals(
        toSortedArray(new String(contentsUserData)), toSortedArray(contentsUserDataNew));
    assertArrayEquals(
        toSortedArray(new String(contentsTagData)), toSortedArray(contentsTagDataNew));
    assertArrayEquals(
        toSortedArray(new String(contentsTableWithSpacesData)),
        toSortedArray(contentsTableWithSpacesDataNew));

    // Test async
    String response = acceptFileUpload(contentsOrderDataFile, schemaName, "Order", true);
    assertTrue(response.contains("id"));
  }

  @Test
  void givenNoSession_whenDownloadingMembers_thenUnauthorized() {
    Response response =
        given().accept(ACCEPT_CSV).when().get("/" + SCHEMA_NAME + "/api/csv/members");
    assertEquals(400, response.getStatusCode());
    assertEquals(
        """
            {
              "errors" : [
                {
                  "message" : "Unauthorized to get schema members"
                }
              ]
            }""",
        response.body().asString());
  }

  @Test
  void shouldDownloadMembers() throws IOException {
    String schemaName = SCHEMA_NAME + "Members";
    Schema memberSchema = database.dropCreateSchema(schemaName);

    // grant a user permission
    database.setUserPassword(PET_SHOP_OWNER, PET_SHOP_OWNER);
    database.setUserPassword(PET_SHOP_VIEWER, PET_SHOP_VIEWER);
    database.setUserPassword(PET_SHOP_MANAGER, PET_SHOP_MANAGER);

    memberSchema.addMember(PET_SHOP_MANAGER, Privileges.MANAGER.toString());
    memberSchema.addMember(PET_SHOP_VIEWER, Privileges.VIEWER.toString());
    memberSchema.addMember(PET_SHOP_OWNER, Privileges.OWNER.toString());
    memberSchema.addMember(ANONYMOUS, Privileges.VIEWER.toString());

    database.grantCreateSchema(PET_SHOP_OWNER);

    Response response =
        given()
            .sessionId(sessionId)
            .accept(ACCEPT_CSV)
            .when()
            .get("/" + schemaName + "/api/csv/members");

    Pattern contentDisposition =
        Pattern.compile("attachment; filename=\"" + schemaName + "_members_\\d{12}\\.csv\"");
    assertTrue(contentDisposition.matcher(response.getHeader("Content-Disposition")).matches());

    Path path =
        Path.of(Objects.requireNonNull(getClass().getResource("csv/members.csv")).getPath());
    String expected = Files.readString(path);
    assertEquals(expected, response.asString());
  }

  @Test
  void shouldDownloadSettings() throws IOException {
    Response response =
        given()
            .sessionId(sessionId)
            .accept(ACCEPT_CSV)
            .when()
            .get("/" + SCHEMA_NAME + "/api/csv/settings");

    Pattern contentDisposition =
        Pattern.compile("attachment; filename=\"" + SCHEMA_NAME + "_settings_\\d{12}\\.csv\"");
    assertTrue(contentDisposition.matcher(response.getHeader("Content-Disposition")).matches());

    Path path =
        Path.of(Objects.requireNonNull(getClass().getResource("csv/settings.csv")).getPath());
    String expected = Files.readString(path);
    assertEquals(expected, response.asString());
  }

  @Test
  void shouldDownloadChangelog() {
    Schema settingsSchema = database.dropCreateSchema(SCHEMA_NAME + "Settings");
    settingsSchema.getMetadata().setSetting(IS_CHANGELOG_ENABLED, "true");
    settingsSchema.create(table("test", column("A").setPkey(), column("B")));
    settingsSchema.getTable("test").insert(List.of(row("A", "a1", "B", "B")));

    Response response =
        given()
            .sessionId(sessionId)
            .accept(ACCEPT_CSV)
            .when()
            .get("/" + settingsSchema.getName() + "/api/csv/changelog");

    Pattern contentDisposition =
        Pattern.compile(
            "attachment; filename=\"" + settingsSchema.getName() + "_changelog_\\d{12}\\.csv\"");
    assertTrue(contentDisposition.matcher(response.getHeader("Content-Disposition")).matches());

    String formatted =
        """
        operation,stamp,userid,tablename,old,new
        I,%s,molgenis,test,,"{""A"":""a1"",""B"":""B"",""test_TEXT_SEARCH_COLUMN"":"" a1 B "",""mg_draft"":null,""mg_insertedBy"":""admin"",""mg_insertedOn"":"""
            .formatted(settingsSchema.getChanges(1).getFirst().stamp());

    assertTrue(response.body().asString().startsWith(formatted));
  }

  @Test
  void givenOffset_whenDownloadingChangelog_thenSkipOffset() {
    Schema settingsSchema = database.dropCreateSchema(SCHEMA_NAME + "Settings");
    settingsSchema.getMetadata().setSetting(IS_CHANGELOG_ENABLED, "true");
    settingsSchema.create(table("test", column("A").setPkey(), column("B")));
    settingsSchema.getTable("test").insert(List.of(row("A", "a1", "B", "B")));

    Response response =
        given()
            .sessionId(sessionId)
            .accept(ACCEPT_CSV)
            .param("offset", "1")
            .when()
            .get("/" + settingsSchema.getName() + "/api/csv/changelog");

    Pattern contentDisposition =
        Pattern.compile(
            "attachment; filename=\"" + settingsSchema.getName() + "_changelog_\\d{12}\\.csv\"");
    assertTrue(contentDisposition.matcher(response.getHeader("Content-Disposition")).matches());

    String formatted = "operation,stamp,userid,tablename,old,new";

    assertTrue(response.body().asString().startsWith(formatted));
  }

  @Test
  void givenLimitPassedCap_whenDownloadingChangelog_thenError() {
    Response response =
        given()
            .sessionId(sessionId)
            .accept(ACCEPT_CSV)
            .param("limit", "1001")
            .when()
            .get("/" + SCHEMA_NAME + "/api/csv/changelog");
    assertEquals(400, response.getStatusCode());
    assertEquals(
        """
                 {
                   "errors" : [
                     {
                       "message" : "Requested 1001 changes, but the maximum allowed is 1000."
                     }
                   ]
                 }""",
        response.body().asString());
  }

  @Test
  void givenInvalidLimitValue_thenError() {
    Response response =
        given()
            .sessionId(sessionId)
            .accept(ACCEPT_CSV)
            .param("limit", "invalid-value")
            .when()
            .get("/" + SCHEMA_NAME + "/api/csv/changelog");
    assertEquals(400, response.getStatusCode());
    assertEquals(
        """
                      {
                        "errors" : [
                          {
                            "message" : "Invalid limit provided, should be a number: For input string: \\"invalid-value\\""
                          }
                        ]
                      }""",
        response.body().asString());
  }

  @Test
  void givenInvalidOffsetValue_thenError() {
    Response response =
        given()
            .sessionId(sessionId)
            .accept(ACCEPT_CSV)
            .param("offset", "invalid-value")
            .when()
            .get("/" + SCHEMA_NAME + "/api/csv/changelog");
    assertEquals(400, response.getStatusCode());
    assertEquals(
        """
                        {
                          "errors" : [
                            {
                              "message" : "Invalid offset provided, should be a number: For input string: \\"invalid-value\\""
                            }
                          ]
                        }""",
        response.body().asString());
  }

  @Test
  void givenNoSession_whenDownloadingChangelog_thenUnauthorized() {
    Response response =
        given().accept(ACCEPT_CSV).when().get("/" + SCHEMA_NAME + "/api/csv/changelog");

    assertEquals(400, response.getStatusCode());
    assertEquals(
        """
        {
          "errors" : [
            {
              "message" : "Unauthorized to get schema changelog"
            }
          ]
        }""",
        response.body().asString());
  }

  @Test
  void shouldFilterTable() {
    String result =
        given()
            .sessionId(sessionId)
            .queryParam("filter", "{\"name\":{\"equals\":\"pooky\"}}")
            .accept(ACCEPT_CSV)
            .when()
            .get("/" + SCHEMA_NAME + "/api/csv/Pet")
            .asString();
    assertTrue(result.contains("pooky"));
    assertFalse(result.contains("spike"));

    result =
        given()
            .sessionId(sessionId)
            .queryParam("filter", "{\"tags\":{\"name\": {\"equals\":\"blue\"}}}")
            .accept(ACCEPT_CSV)
            .when()
            .get("/" + SCHEMA_NAME + "/api/csv/Pet")
            .asString();
    assertTrue(result.contains("jerry"));
    assertFalse(result.contains("spike"));
  }

  @Test
  void shouldUpdateTableMetadata() throws IOException {

    // fresh schema for testing
    String schemaName = SCHEMA_NAME + "Metadata";
    database.dropCreateSchema(schemaName);

    // full table header present in exported table metadata
    String header =
        "tableName,tableExtends,tableType,columnName,formLabel,columnType,key,required,readonly,refSchema,refTable,refLink,refBack,refLabel,defaultValue,validation,visible,computed,semantics,profiles,label,description\n";

    // add new table with description and semantics as metadata
    addUpdateTableAndCompare(
        header,
        schemaName,
        "tableName,description,semantics\nTestMetaTable,TestDesc,TestSem",
        "TestMetaTable,,,,,,,,,,,,,,,,,,TestSem,,,TestDesc\n");

    // update table without new description or semantics, values should be untouched
    addUpdateTableAndCompare(
        header,
        schemaName,
        "tableName\nTestMetaTable",
        "TestMetaTable,,,,,,,,,,,,,,,,,,TestSem,,,TestDesc\n");

    // update only description, semantics should be untouched
    addUpdateTableAndCompare(
        header,
        schemaName,
        "tableName,description\nTestMetaTable,NewTestDesc",
        "TestMetaTable,,,,,,,,,,,,,,,,,,TestSem,,,NewTestDesc\n");

    // make semantics empty by not supplying a value, description  should be untouched
    addUpdateTableAndCompare(
        header,
        schemaName,
        "tableName,semantics\nTestMetaTable,",
        "TestMetaTable,,,,,,,,,,,,,,,,,,,,,NewTestDesc\n");

    // make description empty while also adding a new value for semantics
    addUpdateTableAndCompare(
        header,
        schemaName,
        "tableName,description,semantics\nTestMetaTable,,NewTestSem",
        "TestMetaTable,,,,,,,,,,,,,,,,,,NewTestSem,,,\n");

    // empty both description and semantics
    addUpdateTableAndCompare(
        header,
        schemaName,
        "tableName,description,semantics\nTestMetaTable,,",
        "TestMetaTable,,,,,,,,,,,,,,,,,,,,,\n");

    // add description value, and string array value for semantics
    addUpdateTableAndCompare(
        header,
        schemaName,
        "tableName,description,semantics\nTestMetaTable,TestDesc,\"TestSem1,TestSem2\"",
        "TestMetaTable,,,,,,,,,,,,,,,,,,\"TestSem1,TestSem2\",,,TestDesc\n");
  }

  @Test
  void shouldDownloadAndUploadZip() throws IOException {
    // get original schema
    String schemaCsv =
        given()
            .sessionId(sessionId)
            .accept(ACCEPT_CSV)
            .when()
            .get("/" + SCHEMA_NAME + "/api/csv")
            .asString();

    // create a new schema for zip
    String uploadSchemaName = SCHEMA_NAME + "ZipUploadDownload";
    database.dropCreateSchema(uploadSchemaName);

    // download zip contents of old schema
    byte[] zipContents = getContentAsByteArray(ACCEPT_ZIP, "/" + SCHEMA_NAME + "/api/zip");

    // upload zip contents into new schema
    File zipFile = createTempFile(zipContents, ".zip");
    given()
        .sessionId(sessionId)
        .multiPart(zipFile)
        .when()
        .post("/" + uploadSchemaName + "/api/zip")
        .then()
        .statusCode(200);

    // check if schema equal using json representation
    String schemaCsv2 =
        given()
            .sessionId(sessionId)
            .accept(ACCEPT_CSV)
            .when()
            .get("/" + uploadSchemaName + "/api/csv")
            .asString();
    assertArrayEquals(toSortedArray(schemaCsv), toSortedArray(schemaCsv2));
  }

  @Test
  void shouldUploadAndDownloadCsv() {
    String path = "/" + SCHEMA_NAME + "/api/csv/Tag";

    String result = given().sessionId(sessionId).accept(ACCEPT_CSV).when().get(path).asString();
    assertTrue(result.contains("green,,,colors"));

    String update = "name,parent\r\nyellow,colors\r\n";
    given().sessionId(sessionId).body(update).when().post(path).then().statusCode(200);

    result = given().sessionId(sessionId).accept(ACCEPT_CSV).when().get(path).asString();
    assertTrue(result.contains("yellow"));

    given().sessionId(sessionId).body(update).when().delete(path).then().statusCode(200);

    result = given().sessionId(sessionId).accept(ACCEPT_CSV).when().get(path).asString();
    assertTrue(result.contains("green,,,colors"));
  }

  private String[] toSortedArray(String string) {
    String[] lines = string.split("\n");
    Arrays.sort(lines);
    return lines;
  }

  private byte[] getContentAsByteArray(String fileType, String path) {
    return given().sessionId(sessionId).accept(fileType).when().get(path).asByteArray();
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

  private String acceptFileUpload(File content, String schema, String table, boolean async) {
    Response response =
        given()
            .sessionId(sessionId)
            .body(content)
            .header("fileName", table)
            .when()
            .post("/" + schema + "/api/csv" + (async ? "?async=true" : ""));

    response.then().statusCode(200);

    return response.asString();
  }

  private String getContentAsString(String path) {
    return given().sessionId(sessionId).accept(ACCEPT_CSV).when().get("/" + path).asString();
  }

  /** Helper function to prevent code duplication */
  private void addUpdateTableAndCompare(
      String header, String schemaName, String tableMeta, String expected) throws IOException {
    byte[] addUpdateTable = tableMeta.getBytes(StandardCharsets.UTF_8);
    File addUpdateTableFile = createTempFile(addUpdateTable, ".csv");
    acceptFileUpload(addUpdateTableFile, schemaName, "molgenis", false);
    String actual = getContentAsString(schemaName + "/api/csv");
    assertEquals(header + expected, actual);
  }
}
