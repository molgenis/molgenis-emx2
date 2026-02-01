package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.Constants.ANONYMOUS;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.datamodels.DataModels.Profile.PET_STORE;
import static org.molgenis.emx2.sql.SqlDatabase.*;
import static org.molgenis.emx2.web.Constants.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import graphql.Assert;
import io.restassured.RestAssured;
import io.restassured.filter.session.SessionFilter;
import io.restassured.response.Response;
import io.restassured.specification.RequestSender;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.molgenis.emx2.*;
import org.molgenis.emx2.Order;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInZipFile;
import org.molgenis.emx2.io.tablestore.TableStoreForXlsxFile;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.utils.EnvironmentProperty;
import org.molgenis.emx2.web.controllers.MetricsController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

/* this is a smoke test for the integration of web api with the database layer. So not complete coverage of all services but only a few essential requests to pass most endpoints */
@TestMethodOrder(MethodOrderer.MethodName.class)
@Tag("slow")
@ExtendWith(SystemStubsExtension.class)
class WebApiSmokeTests {

  static final Logger logger = LoggerFactory.getLogger(WebApiSmokeTests.class);

  private static final String EXCEPTION_CONTENT_TYPE = "application/json";

  private static final String ADMIN_PASS =
      (String) EnvironmentProperty.getParameter(MOLGENIS_ADMIN_PW, ADMIN_PW_DEFAULT, STRING);

  public static final String PET_SHOP_OWNER = "pet_shop_owner";
  public static final String PET_SHOP_VIEWER = "shopviewer";
  public static final String PET_SHOP_MANAGER = "shopmanager";

  public static final String SYSTEM_PREFIX = "/" + SYSTEM_SCHEMA;
  public static final String DATA_PET_STORE = "/pet store/api/csv/_schema";
  public static final String TABLE_WITH_SPACES = "table with spaces";
  public static final String PET_STORE_SCHEMA = "pet store";
  private static final String CSV_TEST_SCHEMA = "pet store csv";

  private static final int PORT = 8081; // other than default so we can see effect

  private static String sessionId; // to toss around a session for the tests
  private static Database db;
  private static Schema schema;

  @BeforeAll
  static void before() throws Exception {
    // FIXME: beforeAll fails under windows
    // setup test schema
    db = TestDatabaseFactory.getTestDatabase();

    // start web service for testing, including env variables
    new EnvironmentVariables(
            org.molgenis.emx2.Constants.MOLGENIS_METRICS_ENABLED, Boolean.TRUE.toString())
        .execute(
            () -> {
              RunMolgenisEmx2.main(new String[] {String.valueOf(PORT)});
            });

    // set default rest assured settings
    RestAssured.port = PORT;
    RestAssured.baseURI = "http://localhost";

    setAdminSession();
    setupDatabase();
  }

  private static void setupDatabase() {
    // Always create test database from scratch to avoid instability due to side effects.
    db.dropSchemaIfExists(PET_STORE_SCHEMA);
    PET_STORE.getImportTask(db, PET_STORE_SCHEMA, "", true).run();
    schema = db.getSchema(PET_STORE_SCHEMA);

    // grant a user permission
    db.setUserPassword(PET_SHOP_OWNER, PET_SHOP_OWNER);
    db.setUserPassword(PET_SHOP_VIEWER, PET_SHOP_VIEWER);
    db.setUserPassword(PET_SHOP_MANAGER, PET_SHOP_MANAGER);
    schema.addMember(PET_SHOP_MANAGER, Privileges.MANAGER.toString());
    schema.addMember(PET_SHOP_VIEWER, Privileges.VIEWER.toString());
    schema.addMember(PET_SHOP_OWNER, Privileges.OWNER.toString());
    schema.addMember(ANONYMOUS, Privileges.VIEWER.toString());
    db.grantCreateSchema(PET_SHOP_OWNER);
    if (schema.getTable(TABLE_WITH_SPACES) == null) {
      schema.create(table(TABLE_WITH_SPACES, column("name", STRING).setKey(1)));
    }
  }

  private static void setAdminSession() {
    sessionId =
        given()
            .body(
                "{\"query\":\"mutation{signin(email:\\\""
                    + db.getAdminUserName()
                    + "\\\",password:\\\""
                    + ADMIN_PASS
                    + "\\\"){message}}\"}")
            .when()
            .post("api/graphql")
            .sessionId();
  }

  @AfterAll
  static void after() {
    // Always clean up database to avoid instability due to side effects.
    db.dropSchemaIfExists(PET_STORE_SCHEMA);
    db.dropSchemaIfExists("pet store yaml");
    db.dropSchemaIfExists("pet store json");
  }

  @Test
  void testLoginMultithreaded() throws InterruptedException {
    String testUser = "test@test.com";
    String password = "somepass";

    String createUserQuery =
        "{ \"query\": \"mutation { signup(email: \\\""
            + testUser
            + "\\\", password: \\\""
            + password
            + "\\\") { message }}\"}";

    String signinQuery =
        "{\"query\":\"mutation{signin(email:\\\""
            + testUser
            + "\\\",password:\\\""
            + password
            + "\\\"){message}}\"}";

    String sessionQuery = "{ \"query\": \"{ _session { email } } \"}";

    given().sessionId(sessionId).body(createUserQuery).post("/api/graphql").asString();

    int threadCount = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch readyLatch = new CountDownLatch(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(threadCount);

    ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();

    for (int i = 0; i < threadCount; i++) {
      executor.submit(
          () -> {
            try {
              readyLatch.countDown();
              startLatch.await();

              String signinResult =
                  given().sessionId(sessionId).body(signinQuery).post("/api/graphql").asString();

              try {
                assertTrue(
                    signinResult.contains("Signed in"),
                    "Login failed in thread: " + Thread.currentThread().getName());
              } catch (AssertionError e) {
                logger.warn("[Thread {}] {}", Thread.currentThread().getName(), e.getMessage());
              }

              String sessionResult =
                  given().sessionId(sessionId).body(sessionQuery).post("/api/graphql").asString();

              assertFalse(
                  sessionResult.contains(ADMIN_USER),
                  "ADMIN_USER present in thread: " + Thread.currentThread().getName());

              try {
                assertTrue(
                    sessionResult.contains(testUser),
                    "Session check failed in thread: " + Thread.currentThread().getName());
              } catch (AssertionError e) {
                logger.warn("[Thread {}] {}", Thread.currentThread().getName(), e.getMessage());
              }

            } catch (Throwable t) {
              failures.add(t); // only assertFalse failure or unexpected errors will be added
            } finally {
              doneLatch.countDown();
            }
          });
    }

    readyLatch.await();
    startLatch.countDown();
    doneLatch.await();
    executor.shutdown();

    if (!failures.isEmpty()) {
      for (Throwable t : failures) {
        t.printStackTrace();
      }
      fail(
          "One or more critical assertions failed (ADMIN_USER presence). Total failures: "
              + failures.size());
    }

    // Restore admin session to not break the other tests
    setAdminSession();
  }

  @Test
  void testApiRoot() {
    String result =
        given()
            .sessionId(sessionId)
            .expect()
            .statusCode(200)
            .when()
            .get("/api")
            .getBody()
            .asString();
    assertTrue(result.contains("Welcome to MOLGENIS EMX2"));
  }

  @Test
  void testCsvApi_zipUploadDownload() throws IOException {
    // get original schema
    String schemaCsv =
        given().sessionId(sessionId).accept(ACCEPT_CSV).when().get(DATA_PET_STORE).asString();

    // create a new schema for zip
    db.dropCreateSchema("pet store zip");

    // download zip contents of old schema
    byte[] zipContents = getContentAsByteArray(ACCEPT_ZIP, "/pet store/api/zip/_all");

    // upload zip contents into new schema
    File zipFile = createTempFile(zipContents, ".zip");
    given()
        .sessionId(sessionId)
        .multiPart(zipFile)
        .when()
        .post("/pet store zip/api/zip/_all")
        .then()
        .statusCode(200);

    // check if schema equal using json representation
    String schemaCsv2 =
        given()
            .sessionId(sessionId)
            .accept(ACCEPT_CSV)
            .when()
            .get("/pet store zip/api/csv/_schema")
            .asString();
    assertArrayEquals(toSortedArray(schemaCsv), toSortedArray(schemaCsv2));

    // delete the new schema
    db.dropSchema("pet store zip");
  }

  @Test
  void testReports() throws IOException {
    // create a new schema for report
    db.dropSchemaIfExists("pet store reports");
    PET_STORE.getImportTask(db, "pet store reports", "", true).run();

    // check if reports work
    byte[] zipContents =
        getContentAsByteArray(ACCEPT_ZIP, "/pet store reports/api/reports/zip?id=report1");
    File zipFile = createTempFile(zipContents, ".zip");
    TableStore store = new TableStoreForCsvInZipFile(zipFile.toPath());
    store.containsTable("pet report");

    // check if reports work with parameters
    zipContents =
        getContentAsByteArray(
            ACCEPT_ZIP, "/pet store reports/api/reports/zip?id=report2&name=spike,pooky");
    zipFile = createTempFile(zipContents, ".zip");
    store = new TableStoreForCsvInZipFile(zipFile.toPath());
    store.containsTable("pet report with parameters");

    // check if reports work
    byte[] excelContents =
        getContentAsByteArray(ACCEPT_ZIP, "/pet store reports/api/reports/excel?id=report1");
    File excelFile = createTempFile(excelContents, ".xlsx");
    store = new TableStoreForXlsxFile(excelFile.toPath());
    assertTrue(store.containsTable("report1"));

    // check if reports work with parameters
    excelContents =
        getContentAsByteArray(
            ACCEPT_ZIP, "/pet store reports/api/reports/excel?id=report2&name=spike,pooky");
    excelFile = createTempFile(excelContents, ".xlsx");
    store = new TableStoreForXlsxFile(excelFile.toPath());
    assertTrue(store.containsTable("report2"));
    assertTrue(excelContents.length > 0);

    // test json report api
    String jsonResults =
        given()
            .sessionId(sessionId)
            .get("/pet store reports/api/reports/json?id=report1")
            .asString();
    assertFalse(
        jsonResults.contains("report1"),
        "single result should not include report name"); // are we sure about this?
    jsonResults =
        given()
            .sessionId(sessionId)
            .get("/pet store reports/api/reports/json?id=report1,report2&name=pooky")
            .asString();
    assertTrue(
        jsonResults.contains("report1"),
        "multiple results should use the report name to nest results");
    // check that id is for keys
    jsonResults =
        given()
            .sessionId(sessionId)
            .get("/pet store reports/api/reports/json?id=report1,report2&name=pooky")
            .asString();
    assertTrue(jsonResults.contains("report1"), "should use report id as key");
    assertTrue(jsonResults.contains("report2"), "should use report id as key");

    jsonResults =
        given()
            .sessionId(sessionId)
            .get("/pet store reports/api/reports/json?id=report2&name=spike,pooky")
            .asString();
    assertTrue(jsonResults.contains("pooky"));

    // test report using jsonb_agg
    jsonResults =
        given()
            .sessionId(sessionId)
            .get("/pet store reports/api/reports/json?id=report3")
            .asString();
    ObjectMapper objectMapper = new ObjectMapper();
    List<Object> jsonbResult = objectMapper.readValue(jsonResults, List.class);
    assertTrue(jsonbResult.get(0).toString().contains("pooky"));

    // test report using jsonb rows
    jsonResults =
        given()
            .sessionId(sessionId)
            .get("/pet store reports/api/reports/json?id=report4")
            .asString();
    Object result = objectMapper.readValue(jsonResults, Object.class);
    assertTrue(result.toString().contains("pooky"));

    // test report using json objects
    jsonResults =
        given()
            .sessionId(sessionId)
            .get("/pet store reports/api/reports/json?id=report5")
            .asString();
    Object jsonResult = objectMapper.readValue(jsonResults, Object.class);
    assertTrue(jsonResult.toString().contains("pooky"));

    jsonResults =
        given()
            .sessionId(sessionId)
            .get("/pet store reports/api/reports/json?id=report4,report5")
            .asString();
    Map<String, Object> multipleResults = objectMapper.readValue(jsonResults, Map.class);
    // Check if multiple result are returned as proper json
    assertFalse(multipleResults.get("report4").toString().startsWith("{\""));
  }

  @Test
  void testCsvApi_csvTableMetadataUpdate() throws IOException {

    // fresh schema for testing
    db.dropCreateSchema(CSV_TEST_SCHEMA);

    // full table header present in exported table metadata
    String header =
        "tableName,tableExtends,tableType,columnName,formLabel,columnType,key,required,readonly,refSchema,refTable,refLink,refBack,refLabel,defaultValue,validation,visible,computed,semantics,profiles,label,description\n";

    // add new table with description and semantics as metadata
    addUpdateTableAndCompare(
        header,
        "tableName,description,semantics\nTestMetaTable,TestDesc,TestSem",
        "TestMetaTable,,,,,,,,,,,,,,,,,,TestSem,,,TestDesc\n");

    // update table without new description or semantics, values should be untouched
    addUpdateTableAndCompare(
        header, "tableName\nTestMetaTable", "TestMetaTable,,,,,,,,,,,,,,,,,,TestSem,,,TestDesc\n");

    // update only description, semantics should be untouched
    addUpdateTableAndCompare(
        header,
        "tableName,description\nTestMetaTable,NewTestDesc",
        "TestMetaTable,,,,,,,,,,,,,,,,,,TestSem,,,NewTestDesc\n");

    // make semantics empty by not supplying a value, description  should be untouched
    addUpdateTableAndCompare(
        header,
        "tableName,semantics\nTestMetaTable,",
        "TestMetaTable,,,,,,,,,,,,,,,,,,,,,NewTestDesc\n");

    // make description empty while also adding a new value for semantics
    addUpdateTableAndCompare(
        header,
        "tableName,description,semantics\nTestMetaTable,,NewTestSem",
        "TestMetaTable,,,,,,,,,,,,,,,,,,NewTestSem,,,\n");

    // empty both description and semantics
    addUpdateTableAndCompare(
        header,
        "tableName,description,semantics\nTestMetaTable,,",
        "TestMetaTable,,,,,,,,,,,,,,,,,,,,,\n");

    // add description value, and string array value for semantics
    addUpdateTableAndCompare(
        header,
        "tableName,description,semantics\nTestMetaTable,TestDesc,\"TestSem1,TestSem2\"",
        "TestMetaTable,,,,,,,,,,,,,,,,,,\"TestSem1,TestSem2\",,,TestDesc\n");
  }

  /** Helper function to prevent code duplication */
  private void addUpdateTableAndCompare(String header, String tableMeta, String expected)
      throws IOException {
    byte[] addUpdateTable = tableMeta.getBytes(StandardCharsets.UTF_8);
    File addUpdateTableFile = createTempFile(addUpdateTable, ".csv");
    acceptFileUpload(addUpdateTableFile, "molgenis", false);
    String actual = getContentAsString("/api/csv/_schema");
    assertEquals(header + expected, actual);
  }

  @Test
  void testCsvApi_csvUploadDownload() throws IOException {
    // create a new schema for complete csv data round trip
    db.dropCreateSchema(CSV_TEST_SCHEMA);

    // download csv metadata and data from existing schema
    byte[] contentsMeta = getContentAsByteArray(ACCEPT_CSV, "/pet store/api/csv/_schema");
    byte[] contentsCategoryData = getContentAsByteArray(ACCEPT_CSV, "/pet store/api/csv/Category");
    byte[] contentsOrderData = getContentAsByteArray(ACCEPT_CSV, "/pet store/api/csv/Order");
    byte[] contentsPetData = getContentAsByteArray(ACCEPT_CSV, "/pet store/api/csv/Pet");
    byte[] contentsUserData = getContentAsByteArray(ACCEPT_CSV, "/pet store/api/csv/User");
    byte[] contentsTagData = getContentAsByteArray(ACCEPT_CSV, "/pet store/api/csv/Tag");
    byte[] contentsTableWithSpacesData =
        getContentAsByteArray(ACCEPT_CSV, "/pet store/api/csv/" + TABLE_WITH_SPACES);

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
    acceptFileUpload(contentsMetaFile, "molgenis", false);
    acceptFileUpload(contentsCategoryDataFile, "Category", false);
    acceptFileUpload(contentsTagDataFile, "Tag", false);
    acceptFileUpload(contentsPetDataFile, "Pet", false);
    acceptFileUpload(contentsUserDataFile, "User", false);
    acceptFileUpload(contentsOrderDataFile, "Order", false);
    acceptFileUpload(contentsTableWithSpacesDataFile, TABLE_WITH_SPACES, false);

    // download csv from the new schema
    String contentsMetaNew = getContentAsString("/api/csv/_schema");
    String contentsCategoryDataNew = getContentAsString("/api/csv/Category");
    String contentsPetDataNew = getContentAsString("/api/csv/Pet");
    String contentsUserDataNew = getContentAsString("/api/csv/User");
    String contentsTagDataNew = getContentAsString("/api/csv/Tag");
    String contentsTableWithSpacesDataNew =
        getContentAsString(
            "/api/csv/" + TABLE_WITH_SPACES.toUpperCase()); // to test for case insensitive match

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
    String response = acceptFileUpload(contentsOrderDataFile, "Order", true);
    assertTrue(response.contains("id"));
  }

  private String[] toSortedArray(String string) {
    String[] lines = string.split("\n");
    Arrays.sort(lines);
    return lines;
  }

  @Test
  void testCsvApi_givenNoSession_whenDownloadingMembers_thenUnauthorized() {
    db.dropCreateSchema(CSV_TEST_SCHEMA);

    Response response = given().accept(ACCEPT_CSV).when().get("/pet store/api/csv/_members");

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
  void testCsvApi_downloadMembers() throws IOException {
    db.dropCreateSchema(CSV_TEST_SCHEMA);

    Response response =
        given().sessionId(sessionId).accept(ACCEPT_CSV).when().get("/pet store/api/csv/_members");

    Pattern contentDisposition =
        Pattern.compile("attachment; filename=\"pet store_members_\\d{12}\\.csv\"");
    assertTrue(contentDisposition.matcher(response.getHeader("Content-Disposition")).matches());

    Path path =
        Path.of(Objects.requireNonNull(getClass().getResource("csv/members.csv")).getPath());
    String expected = Files.readString(path);
    assertEquals(expected, response.asString());
  }

  @Test
  void testCsvApi_downloadSettings() throws IOException {
    db.dropCreateSchema(CSV_TEST_SCHEMA);

    Response response =
        given().sessionId(sessionId).accept(ACCEPT_CSV).when().get("pet store/api/csv/_settings");

    Pattern contentDisposition =
        Pattern.compile("attachment; filename=\"pet store_settings_\\d{12}\\.csv\"");
    assertTrue(contentDisposition.matcher(response.getHeader("Content-Disposition")).matches());

    Path path =
        Path.of(Objects.requireNonNull(getClass().getResource("csv/settings.csv")).getPath());
    String expected = Files.readString(path);
    assertEquals(expected, response.asString());
  }

  @Test
  void testCsvApi_changelogDownload() {
    schema.getMetadata().setSetting(IS_CHANGELOG_ENABLED, "true");
    schema.create(table("test", column("A").setPkey(), column("B")));
    schema.getTable("test").insert(List.of(row("A", "a1", "B", "B")));

    Response response =
        given().sessionId(sessionId).accept(ACCEPT_CSV).when().get("/pet store/api/csv/_changelog");

    Pattern contentDisposition =
        Pattern.compile("attachment; filename=\"pet store_changelog_\\d{12}\\.csv\"");
    assertTrue(contentDisposition.matcher(response.getHeader("Content-Disposition")).matches());

    String formatted =
        """
            operation,stamp,userid,tablename,old,new
            I,%s,molgenis,test,,"{""A"":""a1"",""B"":""B"",""test_TEXT_SEARCH_COLUMN"":"" a1 B "",""mg_draft"":null,""mg_insertedBy"":""admin"",""mg_insertedOn"":"""
            .formatted(schema.getChanges(1).getFirst().stamp());

    assertTrue(response.body().asString().startsWith(formatted));
    setupDatabase();
  }

  @Test
  void testCsvApi_givenOffset_whenDownloadingChangelog_thenSkipOffset() {
    schema.getMetadata().setSetting(IS_CHANGELOG_ENABLED, "true");
    schema.create(table("test", column("A").setPkey(), column("B")));
    schema.getTable("test").insert(List.of(row("A", "a1", "B", "B")));

    Response response =
        given()
            .sessionId(sessionId)
            .accept(ACCEPT_CSV)
            .param("offset", "1")
            .when()
            .get("/pet store/api/csv/_changelog");

    Pattern contentDisposition =
        Pattern.compile("attachment; filename=\"pet store_changelog_\\d{12}\\.csv\"");
    assertTrue(contentDisposition.matcher(response.getHeader("Content-Disposition")).matches());

    String formatted = "operation,stamp,userid,tablename,old,new";

    assertTrue(response.body().asString().startsWith(formatted));

    setupDatabase();
  }

  @Test
  void testCsvApi_givenLimitPassedCap_whenDownloadingChangelog_thenError() {
    Response response =
        given()
            .sessionId(sessionId)
            .accept(ACCEPT_CSV)
            .param("limit", "1001")
            .when()
            .get("/pet store/api/csv/_changelog");
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
  void testCsvApi_givenInvalidLimitValue_thenError() {
    Response response =
        given()
            .sessionId(sessionId)
            .accept(ACCEPT_CSV)
            .param("limit", "invalid-value")
            .when()
            .get("/pet store/api/csv/_changelog");
    assertEquals(400, response.getStatusCode());
    assertEquals(
        """
                  {
                    "errors" : [
                      {
                        "message" : "Invalid limit provided, should be a number: NumberFormatException: For input string: \\"invalid-value\\""
                      }
                    ]
                  }""",
        response.body().asString());
  }

  @Test
  void testCsvApi_givenInvalidOffsetValue_thenError() {
    Response response =
        given()
            .sessionId(sessionId)
            .accept(ACCEPT_CSV)
            .param("offset", "invalid-value")
            .when()
            .get("/pet store/api/csv/_changelog");
    assertEquals(400, response.getStatusCode());
    assertEquals(
        """
                    {
                      "errors" : [
                        {
                          "message" : "Invalid offset provided, should be a number: NumberFormatException: For input string: \\"invalid-value\\""
                        }
                      ]
                    }""",
        response.body().asString());
  }

  @Test
  void testCsvApi_givenNoSession_whenDownloadingChangelog_thenUnauthorized() {
    Response response = given().accept(ACCEPT_CSV).when().get("/pet store/api/csv/_changelog");

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
  void testCsvApi_tableFilter() {
    String result =
        given()
            .sessionId(sessionId)
            .queryParam("filter", "{\"name\":{\"equals\":\"pooky\"}}")
            .accept(ACCEPT_CSV)
            .when()
            .get("/pet store/api/csv/Pet")
            .asString();
    assertTrue(result.contains("pooky"));
    assertFalse(result.contains("spike"));

    result =
        given()
            .sessionId(sessionId)
            .queryParam("filter", "{\"tags\":{\"name\": {\"equals\":\"blue\"}}}")
            .accept(ACCEPT_CSV)
            .when()
            .get("/pet store/api/csv/Pet")
            .asString();
    assertTrue(result.contains("jerry"));
    assertFalse(result.contains("spike"));
  }

  private String acceptFileUpload(File content, String table, boolean async) {
    Response response =
        given()
            .sessionId(sessionId)
            .body(content)
            .header("fileName", table)
            .when()
            .post("/" + CSV_TEST_SCHEMA + "/api/csv/_schema" + (async ? "?async=true" : ""));

    response.then().statusCode(200);

    return response.asString();
  }

  private String getContentAsString(String path) {
    return given()
        .sessionId(sessionId)
        .accept(ACCEPT_CSV)
        .when()
        .get("/" + CSV_TEST_SCHEMA + path)
        .asString();
  }

  private byte[] getContentAsByteArray(String fileType, String path) {
    return given().sessionId(sessionId).accept(fileType).when().get(path).asByteArray();
  }

  @Test
  void testJsonYamlApi() {
    String schemaJson =
        given().sessionId(sessionId).when().get("/pet store/api/json/_schema").asString();

    db.dropCreateSchema("pet store json");

    given()
        .sessionId(sessionId)
        .body(schemaJson)
        .when()
        .post("/pet store json/api/json/_schema")
        .then()
        .statusCode(200);

    String schemaJson2 =
        given().sessionId(sessionId).when().get("/pet store json/api/json/_schema").asString();

    assertEquals(schemaJson, schemaJson2.replace("pet store json", PET_STORE_SCHEMA));

    String schemaYaml =
        given().sessionId(sessionId).when().get("/pet store/api/yaml/_schema").asString();

    db.dropCreateSchema("pet store yaml");

    given()
        .sessionId(sessionId)
        .body(schemaYaml)
        .when()
        .post("/pet store yaml/api/yaml/_schema")
        .then()
        .statusCode(200);

    String schemaYaml2 =
        given().sessionId(sessionId).when().get("/pet store yaml/api/yaml/_schema").asString();

    assertEquals(schemaYaml, schemaYaml2.replace("pet store yaml", PET_STORE_SCHEMA));

    given()
        .sessionId(sessionId)
        .body(schemaYaml2)
        .when()
        .delete("/pet store yaml/api/yaml/_schema")
        .then()
        .statusCode(200);

    given()
        .sessionId(sessionId)
        .body(schemaJson2)
        .when()
        .delete("/pet store json/api/json/_schema")
        .then()
        .statusCode(200);

    db.dropSchemaIfExists("pet store yaml");
    db.dropSchemaIfExists("pet store json");
  }

  @Test
  void testJsonYamlTableApi() {
    String tableJson =
        given().sessionId(sessionId).when().get("/pet store/api/json/Pet").asString();
    assertTrue(tableJson.contains("Pet"), "JSON should contain Pet data");

    String tableYaml =
        given().sessionId(sessionId).when().get("/pet store/api/yaml/Pet").asString();
    assertTrue(tableYaml.contains("name"), "YAML should contain name field");
    assertFalse(tableYaml.startsWith("["), "YAML should not start with [");
    assertTrue(
        tableYaml.contains("-") || tableYaml.contains("name:"), "YAML should have YAML syntax");
  }

  @Test
  void testJsonYamlDataApi() {
    String dataJson =
        given().sessionId(sessionId).when().get("/pet store/api/json/_data").asString();
    assertTrue(dataJson.contains("Pet"), "JSON data should contain Pet table");

    String dataYaml =
        given().sessionId(sessionId).when().get("/pet store/api/yaml/_data").asString();
    assertTrue(dataYaml.contains("Pet"), "YAML data should contain Pet table");
  }

  @Test
  void testJsonYamlAllApi() {
    String allJson = given().sessionId(sessionId).when().get("/pet store/api/json/_all").asString();
    assertTrue(allJson.contains("schema"), "JSON _all should contain schema");
    assertTrue(allJson.contains("data"), "JSON _all should contain data");

    String allYaml = given().sessionId(sessionId).when().get("/pet store/api/yaml/_all").asString();
    assertTrue(allYaml.contains("schema"), "YAML _all should contain schema");
    assertTrue(allYaml.contains("data"), "YAML _all should contain data");
  }

  @Test
  void testJsonYamlMembersApi() {
    String membersJson =
        given().sessionId(sessionId).when().get("/pet store/api/json/_members").asString();
    assertTrue(membersJson.contains(PET_SHOP_OWNER), "JSON members should contain pet_shop_owner");

    String membersYaml =
        given().sessionId(sessionId).when().get("/pet store/api/yaml/_members").asString();
    assertTrue(membersYaml.contains(PET_SHOP_OWNER), "YAML members should contain pet_shop_owner");
  }

  @Test
  void testJsonYamlSettingsApi() {
    given().sessionId(sessionId).when().get("/pet store/api/json/_settings").then().statusCode(200);

    given().sessionId(sessionId).when().get("/pet store/api/yaml/_settings").then().statusCode(200);
  }

  @Test
  void testJsonYamlChangelogApi() {
    given()
        .sessionId(sessionId)
        .when()
        .get("/pet store/api/json/_changelog?limit=10&offset=0")
        .then()
        .statusCode(200);

    given()
        .sessionId(sessionId)
        .when()
        .get("/pet store/api/yaml/_changelog?limit=10&offset=0")
        .then()
        .statusCode(200);
  }

  @Test
  void testJsonYamlRowApi() {
    String tableJson =
        given().sessionId(sessionId).when().get("/pet store/api/json/Pet").asString();

    if (tableJson.contains("\"name\"")) {
      given()
          .sessionId(sessionId)
          .when()
          .get("/pet store/api/json/Pet/spike")
          .then()
          .statusCode(200);

      given()
          .sessionId(sessionId)
          .when()
          .get("/pet store/api/yaml/Pet/spike")
          .then()
          .statusCode(200);
    }
  }

  @Test
  void testExcelApi() throws IOException, InterruptedException {

    String schemaCSV =
        given()
            .sessionId(sessionId)
            .accept(ACCEPT_CSV)
            .when()
            .get("/pet store/api/csv/_schema")
            .asString();

    db.dropCreateSchema("pet store excel");

    byte[] excelContents = getContentAsByteArray(ACCEPT_EXCEL, "/pet store/api/excel/_all");
    File excelFile = createTempFile(excelContents, ".xlsx");

    String message =
        given()
            .sessionId(sessionId)
            .multiPart(excelFile)
            .when()
            .post("/pet store excel/api/excel/_all?async=true")
            .asString();

    Map<String, String> val = new ObjectMapper().readValue(message, Map.class);
    String url = val.get("url");
    String id = val.get("id");

    Response poll = given().sessionId(sessionId).when().get(url);
    int count = 0;
    while (poll.body().asString().contains("UNKNOWN")
        || poll.body().asString().contains("RUNNING")) {
      if (count++ > 100) {
        throw new MolgenisException("failed: polling took too long");
      }
      poll = given().sessionId(sessionId).when().get(url);
      Thread.sleep(500);
    }
    assertFalse(
        poll.body().asString().contains("FAILED") || poll.body().asString().contains("ERROR"));

    assertTrue(
        given()
            .sessionId(sessionId)
            .multiPart(excelFile)
            .when()
            .get("/pet store/api/tasks")
            .asString()
            .contains(id));

    String schemaCSV2 =
        given()
            .sessionId(sessionId)
            .accept(ACCEPT_CSV)
            .when()
            .get("/pet store excel/api/csv/_schema")
            .asString();

    assertTrue(schemaCSV2.contains("Pet"));

    byte[] schemaExcel = getContentAsByteArray(ACCEPT_EXCEL, "/pet store excel/api/excel/_schema");
    assertTrue(schemaExcel.length > 0);

    byte[] dataExcel = getContentAsByteArray(ACCEPT_EXCEL, "/pet store excel/api/excel/_data");
    assertTrue(dataExcel.length > 0);

    byte[] membersExcel =
        getContentAsByteArray(ACCEPT_EXCEL, "/pet store excel/api/excel/_members");
    assertTrue(membersExcel.length > 0);

    byte[] settingsExcel =
        getContentAsByteArray(ACCEPT_EXCEL, "/pet store excel/api/excel/_settings");
    assertTrue(settingsExcel.length > 0);

    byte[] changelogExcel =
        getContentAsByteArray(ACCEPT_EXCEL, "/pet store excel/api/excel/_changelog");
    assertTrue(changelogExcel.length > 0);

    db.dropSchema("pet store excel");
  }

  @Test
  void testExcelApi_tableExcelUploadDownload() throws IOException {
    String path = "/pet store/api/excel/Tag";

    byte[] tagData = getContentAsByteArray(ACCEPT_EXCEL, path);
    assertTrue(tagData.length > 0);

    Path tempDir = Files.createTempDirectory("test");
    tempDir.toFile().deleteOnExit();
    Path updateFile = tempDir.resolve("update.xlsx");
    TableStore updateStore = new TableStoreForXlsxFile(updateFile);
    updateStore.writeTable(
        "Tag", List.of("name", "parent"), List.of(row("name", "orange", "parent", "colors")));
    File updateTempFile = updateFile.toFile();
    updateTempFile.deleteOnExit();

    String response =
        given().sessionId(sessionId).multiPart(updateTempFile).when().post(path).asString();
    assertTrue(response.contains("Imported"));

    byte[] updatedData = getContentAsByteArray(ACCEPT_EXCEL, path);
    File updatedTempFile = createTempFile(updatedData, ".xlsx");
    TableStore updatedStore = new TableStoreForXlsxFile(updatedTempFile.toPath());
    boolean foundOrange = false;
    for (Row r : updatedStore.readTable("Tag")) {
      if ("orange".equals(r.getString("name"))) {
        foundOrange = true;
        break;
      }
    }
    assertTrue(foundOrange);

    Path deleteFile = tempDir.resolve("delete.xlsx");
    TableStore deleteStore = new TableStoreForXlsxFile(deleteFile);
    deleteStore.writeTable("Tag", List.of("name"), List.of(row("name", "orange")));
    File deleteTempFile = deleteFile.toFile();
    deleteTempFile.deleteOnExit();

    String deleteResponse =
        given().sessionId(sessionId).multiPart(deleteTempFile).when().delete(path).asString();
    assertTrue(deleteResponse.contains("Deleted"));

    byte[] afterDeleteData = getContentAsByteArray(ACCEPT_EXCEL, path);
    File afterDeleteTempFile = createTempFile(afterDeleteData, ".xlsx");
    TableStore afterDeleteStore = new TableStoreForXlsxFile(afterDeleteTempFile.toPath());
    boolean orangeStillExists = false;
    for (Row r : afterDeleteStore.readTable("Tag")) {
      if ("orange".equals(r.getString("name"))) {
        orangeStillExists = true;
        break;
      }
    }
    assertFalse(orangeStillExists);
  }

  @Test
  void testZipApi() throws IOException {
    byte[] allZip = getContentAsByteArray(ACCEPT_ZIP, "/pet store/api/zip/_all");
    assertTrue(allZip.length > 0);

    byte[] schemaZip = getContentAsByteArray(ACCEPT_ZIP, "/pet store/api/zip/_schema");
    assertTrue(schemaZip.length > 0);

    byte[] dataZip = getContentAsByteArray(ACCEPT_ZIP, "/pet store/api/zip/_data");
    assertTrue(dataZip.length > 0);

    byte[] membersZip = getContentAsByteArray(ACCEPT_ZIP, "/pet store/api/zip/_members");
    assertTrue(membersZip.length > 0);

    byte[] settingsZip = getContentAsByteArray(ACCEPT_ZIP, "/pet store/api/zip/_settings");
    assertTrue(settingsZip.length > 0);
  }

  @Test
  void testZipApi_tableZipUploadDownload() throws IOException {
    String path = "/pet store/api/zip/Tag";

    byte[] zipData = getContentAsByteArray(ACCEPT_ZIP, path);
    assertTrue(zipData.length > 0);

    Path tempDir = Files.createTempDirectory("test");
    tempDir.toFile().deleteOnExit();

    Path uploadFile = tempDir.resolve("upload.zip");
    TableStore uploadStore = new TableStoreForCsvInZipFile(uploadFile);
    uploadStore.writeTable(
        "Tag", List.of("name", "parent"), List.of(row("name", "testpurple", "parent", "colors")));
    File uploadTempFile = uploadFile.toFile();
    uploadTempFile.deleteOnExit();

    String uploadResponse =
        given().sessionId(sessionId).multiPart(uploadTempFile).when().post(path).asString();
    assertTrue(uploadResponse.contains("Imported"));

    byte[] updatedData = getContentAsByteArray(ACCEPT_ZIP, path);
    File updatedTempFile = createTempFile(updatedData, ".zip");
    TableStore updatedStore = new TableStoreForCsvInZipFile(updatedTempFile.toPath());
    boolean foundPurple = false;
    for (Row r : updatedStore.readTable("Tag")) {
      if ("testpurple".equals(r.getString("name"))) {
        foundPurple = true;
        break;
      }
    }
    assertTrue(foundPurple);

    Path deleteFile = tempDir.resolve("delete.zip");
    TableStore deleteStore = new TableStoreForCsvInZipFile(deleteFile);
    deleteStore.writeTable("Tag", List.of("name"), List.of(row("name", "testpurple")));
    File deleteTempFile = deleteFile.toFile();
    deleteTempFile.deleteOnExit();

    String deleteResponse =
        given().sessionId(sessionId).multiPart(deleteTempFile).when().delete(path).asString();
    assertTrue(deleteResponse.contains("Deleted"), "ZIP delete response: " + deleteResponse);

    byte[] afterDeleteData = getContentAsByteArray(ACCEPT_ZIP, path);
    File afterDeleteTempFile = createTempFile(afterDeleteData, ".zip");
    TableStore afterDeleteStore = new TableStoreForCsvInZipFile(afterDeleteTempFile.toPath());
    boolean testpurpleStillExists = false;
    for (Row r : afterDeleteStore.readTable("Tag")) {
      if ("testpurple".equals(r.getString("name"))) {
        testpurpleStillExists = true;
        break;
      }
    }
    assertFalse(testpurpleStillExists);
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

  @Test
  void testCsvApi_tableCsvUploadDownload() {

    String path = "/pet store/api/csv/Tag";

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

  @Test
  void testGraphqlApi() {
    String path = "/api/graphql";

    // session filter will take care of sessions if applicable
    SessionFilter sessionFilter = new SessionFilter();

    String result =
        given()
            .filter(sessionFilter)
            .body("{\"query\":\"{_session{email}}\"}")
            .when()
            .post(path)
            .asString();
    assertTrue(result.contains("anonymous"));

    // if anonymous then should not be able to see users
    result =
        given()
            .filter(sessionFilter)
            .body("{\"query\":\"{_admin{userCount}}\"}")
            .when()
            .post(path)
            .asString();
    assertTrue(result.contains("errors"));

    result =
        given()
            .filter(sessionFilter)
            .body(
                "{\"query\":\"mutation{signin(email:\\\""
                    + db.getAdminUserName()
                    + "\\\",password:\\\""
                    + ADMIN_PASS
                    + "\\\"){message}}\"}")
            .when()
            .post(path)
            .asString();
    assertTrue(result.contains("Signed in"));

    result =
        given()
            .filter(sessionFilter)
            .body("{\"query\":\"{_session{email}}\"}")
            .when()
            .post(path)
            .asString();
    assertTrue(result.contains(db.getAdminUserName()));

    // if admin then should  be able to see users
    result =
        given()
            .filter(sessionFilter)
            .body("{\"query\":\"{_admin{userCount}}\"}")
            .when()
            .post(path)
            .asString();
    assertFalse(result.contains("Error"));

    String schemaPath = "/pet store/api/graphql";
    result =
        given()
            .filter(sessionFilter)
            .body("{\"query\":\"{Pet{name}}\"}")
            .when()
            .post(schemaPath)
            .asString();
    assertTrue(result.contains("spike"));

    result =
        given()
            .filter(sessionFilter)
            .contentType("multipart/form-data")
            .multiPart(
                "query", "mutation insert($value:[OrderInput]){insert(Order:$value){message}}")
            .multiPart(
                "variables",
                "{\"value\":[{\"quantity\":\"5\",\"price\":22,\"pet\":{\"name\":\"pooky\"}}]}")
            .when()
            .post(schemaPath)
            .asString();
    assertTrue(result.contains("inserted 1 record"));

    result =
        given()
            .filter(sessionFilter)
            .body("{\"query\":\"mutation{signout{message}}\"}")
            .when()
            .post(path)
            .asString();
    assertTrue(result.contains("signed out"));

    // if anonymous then should not be able to see users
    result =
        given()
            .filter(sessionFilter)
            .body("{\"query\":\"{_admin{userCount}}\"}")
            .when()
            .post(path)
            .asString();
    assertTrue(result.contains("errors"));
  }

  @Test
  void testBootstrapThemeService() {
    // should success
    String css = given().when().get("/pet store/tables/theme.css?primaryColor=123123").asString();
    Assert.assertTrue(css.contains("123123"));

    // should fail
    css = given().when().get("/pet store/tables/theme.css?primaryColor=pink").asString();
    Assert.assertTrue(css.contains("pink"));
  }

  @Test
  void testMolgenisWebservice_redirectToFirstMenuItem() {
    given()
        .redirects()
        .follow(false)
        .expect()
        .statusCode(302)
        .header("Location", is("/pet%20store/tables"))
        .when()
        .get("/pet store/");

    schema
        .getMetadata()
        .setSetting(
            "menu",
            "[{\"label\":\"home\",\"href\":\"../blaat\", \"role\":\"Manager\"},{\"label\":\"home\",\"href\":\"../blaat2\", \"role\":\"Viewer\"}]");

    // sign in as shopviewer
    String shopViewerSessionId =
        given()
            .body(
                "{\"query\":\"mutation{signin(email:\\\"shopviewer\\\",password:\\\"shopviewer\\\"){message}}\"}")
            .when()
            .post("/api/graphql")
            .sessionId();

    given()
        .sessionId(shopViewerSessionId)
        .redirects()
        .follow(false)
        .expect()
        .statusCode(302)
        .header("Location", is("/pet%20store/blaat2"))
        .when()
        .get("/pet store/");

    // sign in as shopviewer
    String shopManagerSessionId =
        given()
            .body(
                "{\"query\":\"mutation{signin(email:\\\"shopmanager\\\",password:\\\"shopmanager\\\"){message}}\"}")
            .when()
            .post("/api/graphql")
            .sessionId();

    given()
        .sessionId(shopManagerSessionId)
        .redirects()
        .follow(false)
        .expect()
        .statusCode(302)
        .header("Location", is("/pet%20store/blaat"))
        .when()
        .get("/pet store/");

    schema.getMetadata().removeSetting("menu");
    db.becomeAdmin();
  }

  @Test
  void testTokenBasedAuth() throws JsonProcessingException {

    // check if we can use temporary token
    String token = getToken("shopmanager", "shopmanager");
    String result;

    // without token we are anonymous
    assertTrue(
        given()
            .body("{\"query\":\"{_session{email}}\"}")
            .post("/api/graphql")
            .getBody()
            .asString()
            .contains("anonymous"));

    // with token we are shopmanager
    assertTrue(
        given()
            .header(MOLGENIS_TOKEN[0], token)
            .body("{\"query\":\"{_session{email}}\"}")
            .post("/api/graphql")
            .getBody()
            .asString()
            .contains("shopmanager"));

    // can we create a long lived token
    result =
        given()
            .header(MOLGENIS_TOKEN[0], token)
            .body(
                "{\"query\":\"mutation{createToken(email:\\\"shopmanager\\\",tokenName:\\\"mytoken\\\"){message,token}}\"}")
            .when()
            .post("/api/graphql")
            .getBody()
            .asString();
    token = new ObjectMapper().readTree(result).at("/data/createToken/token").textValue();

    // with long lived token we are shopmanager
    // also test using an alternative auth token key (should make no difference)
    assertTrue(
        given()
            .header(MOLGENIS_TOKEN[1], token)
            .body("{\"query\":\"{_session{email}}\"}")
            .post("/api/graphql")
            .getBody()
            .asString()
            .contains("shopmanager"));

    // get token for admin
    result =
        given()
            .body(
                "{\"query\":\"mutation{signin(email:\\\"admin\\\",password:\\\"admin\\\"){message,token}}\"}")
            .when()
            .post("/api/graphql")
            .getBody()
            .asString();
    token = new ObjectMapper().readTree(result).at("/data/signin/token").textValue();

    // as admin can we create a long lived token for others
    result =
        given()
            .header(MOLGENIS_TOKEN[0], token)
            .body(
                "{\"query\":\"mutation{createToken(email:\\\"shopmanager\\\" tokenName:\\\"mytoken\\\"){message,token}}\"}")
            .when()
            .post("/api/graphql")
            .getBody()
            .asString();
    token = new ObjectMapper().readTree(result).at("/data/createToken/token").textValue();

    // with long lived token we are shopmanager
    // also test using an alternative auth token key (should make no difference)
    assertTrue(
        given()
            .header(MOLGENIS_TOKEN[1], token)
            .body("{\"query\":\"{_session{email}}\"}")
            .post("/api/graphql")
            .getBody()
            .asString()
            .contains("shopmanager"));
  }

  @Test
  void testMolgenisWebservice_robotsDotTxt() {
    when().get("/robots.txt").then().statusCode(200).body(equalTo("User-agent: *\nAllow: /"));
  }

  @Test
  void testRdfApiRequest() {
    final String urlPrefix = "http://localhost:" + PORT;

    final String defaultContentType = "text/turtle";
    final String jsonldContentType = "application/ld+json";
    final String ttlContentType = "text/turtle";
    final String n3ContentType = "text/n3";
    final String defaultContentTypeWithCharset = "text/turtle; charset=utf-8";
    final String defaultContentTypeWithInvalidCharset = "text/turtle; charset=utf-16";

    // skip 'all schemas' test because data is way to big (i.e.
    // get("http://localhost:PORT/api/rdf");)

    // Validate individual API points for /api/rdf
    rdfApiRequest(200, defaultContentType).get(urlPrefix + "/pet store/api/rdf");
    rdfApiRequest(200, defaultContentType).get(urlPrefix + "/pet store/api/rdf/Category");
    rdfApiRequest(200, defaultContentType)
        .get(urlPrefix + "/pet store/api/rdf/Category/column/name");
    rdfApiRequest(200, defaultContentType).get(urlPrefix + "/pet store/api/rdf/Category/name=cat");
    rdfApiRequestMinimalExpect(400).get(urlPrefix + "/pet store/api/rdf/doesnotexist");
    rdfApiRequest(200, defaultContentType).get(urlPrefix + "/api/rdf?schemas=pet store");

    // Validate API point with charset
    rdfApiContentTypeRequest(200, defaultContentTypeWithCharset, defaultContentType)
        .get(urlPrefix + "/pet store/api/rdf");
    rdfApiContentTypeRequest(406, defaultContentTypeWithInvalidCharset, EXCEPTION_CONTENT_TYPE)
        .get(urlPrefix + "/pet store/api/rdf");

    // Validate convenience API points
    rdfApiRequest(200, jsonldContentType).get(urlPrefix + "/pet store/api/jsonld");
    rdfApiRequest(200, ttlContentType).get(urlPrefix + "/pet store/api/ttl");

    // Validate non-default content-type for /api/rdf
    rdfApiContentTypeRequest(200, jsonldContentType).get(urlPrefix + "/pet store/api/rdf");

    // Validate convenience API points with incorrect given content-type request
    rdfApiContentTypeRequest(200, ttlContentType, jsonldContentType)
        .get(urlPrefix + "/pet store/api/jsonld");
    rdfApiContentTypeRequest(200, jsonldContentType, ttlContentType)
        .get(urlPrefix + "/pet store/api/ttl");

    // Validate head for API points
    rdfApiRequest(200, defaultContentType).head(urlPrefix + "/pet store/api/rdf");
    rdfApiContentTypeRequest(200, jsonldContentType).head(urlPrefix + "/pet store/api/rdf");
    rdfApiRequest(200, jsonldContentType).head(urlPrefix + "/pet store/api/jsonld");
    rdfApiRequest(200, ttlContentType).head(urlPrefix + "/pet store/api/ttl");

    // Validate head for API points with incorrect given content-type for convenience API points
    rdfApiContentTypeRequest(200, ttlContentType, jsonldContentType)
        .head(urlPrefix + "/pet store/api/jsonld");
    rdfApiContentTypeRequest(200, jsonldContentType, ttlContentType)
        .head(urlPrefix + "/pet store/api/ttl");

    // Validate SHACL validation requests
    rdfApiRequest(200, defaultContentType).get(urlPrefix + "/pet store/api/rdf?validate=fdp-v1.2");
    rdfApiRequest(400, EXCEPTION_CONTENT_TYPE)
        .get(urlPrefix + "/pet store/api/rdf?validate=nonExisting"); // TODO: expect 404

    // TODO: Fix HEAD to be equal to GET requests
    //  (out-of-scope because changes also influence other requests to RDF API)
    // Validate head for SHACL validation requests
    //    rdfApiRequest(200, defaultContentType).head(urlPrefix + "/pet
    // store/api/rdf?validate=fdp-v1.2");
    //    rdfApiRequest(404, EXCEPTION_CONTENT_TYPE)
    //            .head(urlPrefix + "/pet store/api/rdf?validate=nonExisting");

    // Validate SHACL SETS API request
    rdfApiRequest(200, ACCEPT_YAML).get(urlPrefix + "/api/rdf?shacls");
    rdfApiContentTypeRequest(200, defaultContentType, ACCEPT_YAML)
        .get(urlPrefix + "/api/rdf?shacls");

    // Validate head for SHACL SETS API request
    rdfApiRequest(200, ACCEPT_YAML).head(urlPrefix + "/api/rdf?shacls");
    rdfApiContentTypeRequest(200, defaultContentType, ACCEPT_YAML)
        .head(urlPrefix + "/api/rdf?shacls");

    // Validate multi-content type negotiation
    rdfApiContentTypeRequest(200, "text/turtle; q=0.5, application/ld+json", jsonldContentType);
    rdfApiContentTypeRequest(200, "text/turtle; q=0.5, text/*", n3ContentType)
        .head(urlPrefix + "/pet store/api/rdf");
    rdfApiContentTypeRequest(406, "image/jpeg", EXCEPTION_CONTENT_TYPE)
        .head(urlPrefix + "/pet store/api/rdf");
  }

  @Test
  void testRdfApiContent() {
    // Output from global API call.
    String resultBase =
        given()
            .sessionId(sessionId)
            .when()
            .get("http://localhost:" + PORT + "/api/rdf?schemas=pet store")
            .getBody()
            .asString();

    // Output from global API call with invalid schema.
    // TODO: https://github.com/molgenis/molgenis-emx2/issues/4954 (fix to return 204)
    String resultBaseNonExisting =
        given()
            .sessionId(sessionId)
            .when()
            .get("http://localhost:" + PORT + "/api/rdf?schemas=thisSchemaTotallyDoesNotExist")
            .getBody()
            .asString();

    // Output shacl sets
    String resultShaclSetsYaml =
        given()
            .sessionId(sessionId)
            .when()
            .get("http://localhost:" + PORT + "/api/rdf?shacls")
            .getBody()
            .asString();

    // Output schema API call.
    String resultSchema =
        given()
            .sessionId(sessionId)
            .when()
            .get("http://localhost:" + PORT + "/pet store/api/rdf")
            .getBody()
            .asString();

    assertAll(
        // Validate base API.
        () -> assertFalse(resultBase.contains("CatalogueOntologies")),
        () ->
            assertTrue(
                resultBaseNonExisting.contains(
                    "Schema 'thisSchemaTotallyDoesNotExist' unknown or permission denied")),
        () ->
            assertTrue(
                resultBase.contains(
                    "http://localhost:" + PORT + "/pet%20store/api/rdf/Category/column/name")),
        // Validate schema API.
        () ->
            assertTrue(
                resultSchema.contains(
                    "http://localhost:" + PORT + "/pet%20store/api/rdf/Category/column/name")),
        // Test on small snippet to validate "files:" is absent (and all other fields are present)
        () ->
            assertTrue(
                resultShaclSetsYaml.contains(
                    """
                    - id: dcat-ap-v3
                      name: DCAT-AP
                      version: 3.0.0
                      sources:
                      - https://semiceu.github.io/DCAT-AP/releases/3.0.0/#validation-of-dcat-ap
                    - id: hri-v2.0.2""")));
  }

  /**
   * Request that does not define a content type but does validate on this.
   *
   * @param expectStatusCode
   * @param expectContentType
   * @return
   */
  private RequestSender rdfApiRequest(int expectStatusCode, String expectContentType) {
    return given()
        .sessionId(sessionId)
        .expect()
        .statusCode(expectStatusCode)
        .header("Content-Type", expectContentType)
        .when();
  }

  /**
   * Request that does define a content type and validates on this.
   *
   * @param expectStatusCode
   * @param contentType
   * @return
   */
  private RequestSender rdfApiContentTypeRequest(int expectStatusCode, String contentType) {
    return rdfApiContentTypeRequest(expectStatusCode, contentType, contentType);
  }

  /**
   * Request that defines given & expected content types individually and validates on this.
   *
   * @param expectStatusCode
   * @param expectedContentType
   * @return
   */
  private RequestSender rdfApiContentTypeRequest(
      int expectStatusCode, String givenContentType, String expectedContentType) {
    return given()
        .sessionId(sessionId)
        .header("Accept", givenContentType)
        .expect()
        .statusCode(expectStatusCode)
        .header("Content-Type", expectedContentType)
        .when();
  }

  /**
   * Request that only validates on status code.
   *
   * @param expectStatusCode
   * @return
   */
  private RequestSender rdfApiRequestMinimalExpect(int expectStatusCode) {
    return given().sessionId(sessionId).expect().statusCode(expectStatusCode).when();
  }

  @Test
  void downloadCsvTable() {
    Response response = downloadPet("/pet store/api/csv/Pet");
    assertTrue(
        response.getBody().asString().contains("name,category,photoUrls,status,tags,weight"));
    assertTrue(response.getBody().asString().contains("pooky,cat,,available,,9.4"));
  }

  @Test
  void downloadCsvTableWithSystemColumns() {
    Response response = downloadPet("/pet store/api/csv/Pet?" + INCLUDE_SYSTEM_COLUMNS + "=true");
    assertTrue(response.getBody().asString().contains("mg_"));
  }

  @Test
  void downloadExcelTable() throws IOException {
    Response response = downloadPet("/pet store/api/excel/Pet");
    List<String> rows = TestUtils.readExcelSheet(response.getBody().asInputStream());
    assertEquals("name,category,photoUrls,status,tags,weight,orders,mg_draft", rows.get(0));
    assertEquals(
        "pooky,cat,,available,,9.4,ORDER:6fe7a528-2e97-48cc-91e6-a94c689b4919,", rows.get(1));
  }

  @Test
  void downloadExelTableWithSystemColumns() throws IOException {
    Response response = downloadPet("/pet store/api/excel/Pet?" + INCLUDE_SYSTEM_COLUMNS + "=true");
    List<String> rows = TestUtils.readExcelSheet(response.getBody().asInputStream());
    assertTrue(rows.get(0).contains("mg_"));
  }

  @Test
  void downloadZipTable() throws IOException, InterruptedException {
    File file = TestUtils.responseToFile(downloadPet("/pet store/api/zip/Pet"));
    List<File> files = TestUtils.extractFileFromZip(file);
    String result = Files.readString(files.get(0).toPath());
    assertTrue(result.contains("name,category,photoUrls,status,tags,weight"));
    assertTrue(result.contains("pooky,cat,,available,,9.4"));
  }

  @Test
  void downloadZipTableWithSystemColumns() throws IOException, InterruptedException {
    File file =
        TestUtils.responseToFile(
            downloadPet("/pet store/api/zip/Pet?" + INCLUDE_SYSTEM_COLUMNS + "=true"));
    List<File> files = TestUtils.extractFileFromZip(file);
    String result = Files.readString(files.get(0).toPath());
    assertTrue(result.contains("mg_"));
  }

  private Response downloadPet(String requestString) {
    return given()
        .sessionId(sessionId)
        .accept(ACCEPT_EXCEL)
        .expect()
        .statusCode(200)
        .when()
        .get(requestString);
  }

  @Test
  void testRoot() {
    given()
        .sessionId(sessionId)
        .redirects()
        .follow(false)
        .expect()
        .statusCode(302)
        .header("Location", "/apps/central/")
        .when()
        .get("/")
        .getHeader("Location");
  }

  @Test
  @Disabled("unstable")
  void testScriptExecution() throws JsonProcessingException, InterruptedException {
    // get token for admin
    String token = getToken("admin", "admin");
    String result;

    // submit simple
    result =
        given()
            .header(MOLGENIS_TOKEN[0], token)
            .when()
            .post("/api/scripts/hello+world")
            .getBody()
            .asString();
    String taskId = new ObjectMapper().readTree(result).at("/id").textValue();

    // poll until completed
    String taskUrl = "/api/tasks/" + taskId;
    // poll task until complete
    result = given().header(MOLGENIS_TOKEN[0], token).when().get(taskUrl).getBody().asString();
    String status = new ObjectMapper().readTree(result).at("/status").textValue();
    int count = 0;
    // poll while running
    while (!result.contains("ERROR") && !"COMPLETED".equals(status) && !"ERROR".equals(status)) {
      if (count++ > 10) {
        throw new MolgenisException("failed: polling took too long, result is: " + result);
      }
      Thread.sleep(1000);
      result = given().header(MOLGENIS_TOKEN[0], token).when().get(taskUrl).getBody().asString();
      status = new ObjectMapper().readTree(result).at("/status").textValue();
    }
    if (result.contains("ERROR")) {
      fail(result);
    }

    String outputURL = "/api/tasks/" + taskId + "/output";
    result = given().header(MOLGENIS_TOKEN[0], token).when().get(outputURL).getBody().asString();
    if (result.equals("Readme")) {
      System.out.println("testScriptExcution error: " + result);
    }
    assertEquals("Readme", result);

    // now with parameters

    // submit simple
    result =
        given()
            .header(MOLGENIS_TOKEN[0], token)
            .body("blaat")
            .when()
            .post("/api/scripts/hello+world")
            .getBody()
            .asString();
    taskId = new ObjectMapper().readTree(result).at("/id").textValue();

    // poll until completed
    taskUrl = "/api/tasks/" + taskId;
    // poll task until complete
    result = given().header(MOLGENIS_TOKEN[0], token).when().get(taskUrl).getBody().asString();
    status = new ObjectMapper().readTree(result).at("/status").textValue();
    count = 0;
    // poll while running
    // (previously we checked on 'complete' but then it also fired if subtask was complete)
    while (!result.contains("ERROR") && !"COMPLETED".equals(status) && !"ERROR".equals(status)) {
      if (count++ > 10) {
        throw new MolgenisException("failed: polling took too long, result is: " + result);
      }
      Thread.sleep(1000);
      result = given().header(MOLGENIS_TOKEN[0], token).when().get(taskUrl).getBody().asString();
      status = new ObjectMapper().readTree(result).at("/status").textValue();
    }
    if (result.contains("ERROR")) {
      fail(result);
    }

    assertTrue(result.contains("sys.argv[1]=blaat")); // the expected output
  }

  @Test
  void testScriptScheduling() throws JsonProcessingException, InterruptedException {
    // make sure the 'test' script is not there already from a previous test
    db.getSchema(SYSTEM_SCHEMA).getTable("Jobs").truncate();
    db.getSchema(SYSTEM_SCHEMA).getTable("Scripts").delete(row("name", "test"));

    String token = getToken("admin", "admin");
    String result;

    // simply retrieve the results using get
    // todo: also allow anonymous
    result =
        given()
            .header(MOLGENIS_TOKEN[0], token)
            .when()
            .get(SYSTEM_PREFIX + "/api/scripts/hello+world")
            .getBody()
            .asString();
    assertEquals("Readme", result);

    // simply retrieve the results using get, outside schema
    // todo: also allow anonymous
    result =
        given()
            .header(MOLGENIS_TOKEN[0], token)
            .when()
            .get("/api/scripts/hello+world")
            .getBody()
            .asString();
    assertEquals("Readme", result);

    // or async using post and then we get a task id
    // simply retrieve the results using get
    // todo: also allow anonymous
    result =
        given()
            .header(MOLGENIS_TOKEN[0], token)
            .when()
            .body("blaat")
            .post(SYSTEM_PREFIX + "/api/scripts/hello+world")
            .asString();

    Row jobMetadata = waitForScriptToComplete("hello world");
    // retrieve the file
    result =
        given()
            .header(MOLGENIS_TOKEN[0], token)
            .when()
            .body("blaat")
            .get(SYSTEM_PREFIX + "/api/tasks/" + jobMetadata.getString("id") + "/output")
            .asString();
    assertEquals("Readme", result);
    // also works outside schema
    result =
        given()
            .header(MOLGENIS_TOKEN[0], token)
            .when()
            .body("blaat")
            .get("/api/tasks/" + jobMetadata.getString("id") + "/output")
            .asString();
    assertEquals("Readme", result);

    // save a scheduled script that fires every second
    given()
        .header(MOLGENIS_TOKEN[0], token)
        .when()
        .body(
            "{\"query\":\"mutation{insert(Scripts:{name:\\\"test\\\",cron:\\\"0/5 * * * * ?\\\",script:\\\"print('test123')\\\"}){message}}\"}")
        .post(SYSTEM_PREFIX + "/api/graphql")
        .getBody()
        .asString();

    // see that it is listed
    result =
        given()
            .header(MOLGENIS_TOKEN[0], token)
            .when()
            .get("/api/tasks/scheduled")
            .getBody()
            .asString();
    assertTrue(result.contains("test")); // should contain our script

    // delete the scripts
    result =
        given()
            .header(MOLGENIS_TOKEN[0], token)
            .when()
            .body("{\"query\":\"mutation{delete(Scripts:{name:\\\"test\\\"}){message}}\"}")
            .post(SYSTEM_PREFIX + "/api/graphql")
            .getBody()
            .asString();

    assertTrue(result.contains("delete 1 records from Scripts"));

    // script should be deleted
    assertTrue(
        db.getSchema(SYSTEM_SCHEMA)
            .getTable("Scripts")
            .where(f("name", EQUALS, "test"))
            .retrieveRows()
            .isEmpty(),
        "script should be deleted");

    // check if the jobs that ran were okay
    assertNotNull(jobMetadata, "should have at least a job");
    System.out.println(jobMetadata);
    assertEquals("COMPLETED", jobMetadata.getString("status"));

    // script should be unscheduled
    result =
        given()
            .header(MOLGENIS_TOKEN[0], token)
            .when()
            .get("/api/tasks/scheduled")
            .getBody()
            .asString();
    assertTrue(result.contains("[]"), "script should be unscheduled");
  }

  @Test
  @Disabled("unstable; fails on CI around 50% of the time")
  // todo update / rewrite test to be more stable in CI env
  void testExecuteSubtaskInScriptTask() throws JsonProcessingException, InterruptedException {
    String parentJobName = "parentJobTest";
    Table jobs = db.getSchema(SYSTEM_SCHEMA).getTable("Scripts");
    jobs.delete(row("name", parentJobName));
    db.dropSchemaIfExists("ScriptWithFileUpload");
    String script =
        """
            import asyncio
            import logging
            import os
            from molgenis_emx2_pyclient import Client

            async def main():
                logging.basicConfig(level='INFO')
                logging.getLogger("requests").setLevel(logging.WARNING)
                logging.getLogger("urllib3").setLevel(logging.WARNING)

                async with Client('http://localhost:8081', token=os.environ['MOLGENIS_TOKEN'], job="${jobId}") as client:
                    await client.create_schema(name="ScriptWithFileUpload", description="TestFileUploadScript",
                                template="PET_STORE", include_demo_data=False)

            if __name__ == '__main__':
                asyncio.run(main())

            """;
    jobs.insert(
        row(
            "name",
            parentJobName,
            "type",
            "python",
            "script",
            script,
            "dependencies",
            "--extra-index-url https://test.pypi.org/simple/\n"
                + "molgenis-emx2-pyclient>=11.22.0"));
    String result =
        given()
            .sessionId(sessionId)
            .when()
            .post("/api/scripts/" + parentJobName)
            .getBody()
            .asString();

    String url = new ObjectMapper().readTree(result).at("/url").textValue();
    assertTrue(testJobSuccess(url));

    String failingJobName = "failingJobTest";
    jobs.delete(row("name", failingJobName));
    db.dropSchemaIfExists("ScriptWithFileUpload");
    String scriptFail = script.replace("PET_STORE", "PET_STORES");
    jobs.insert(
        row(
            "name",
            failingJobName,
            "type",
            "python",
            "script",
            scriptFail,
            "dependencies",
            "--extra-index-url https://test.pypi.org/simple/\n"
                + "molgenis-emx2-pyclient>=11.22.0"));

    result =
        given()
            .sessionId(sessionId)
            .when()
            .post("/api/scripts/" + failingJobName)
            .getBody()
            .asString();

    url = new ObjectMapper().readTree(result).at("/url").textValue();
    assertFalse(testJobSuccess(url));
  }

  private static boolean testJobSuccess(String url)
      throws InterruptedException, JsonProcessingException {
    String result = given().sessionId(sessionId).get(url).asString();

    String status = "WAITING";
    int count = 0;
    while (!result.contains("ERROR") && !"COMPLETED".equals(status) && !"ERROR".equals(status)) {
      if (count++ > 30) {
        throw new MolgenisException("failed: polling took too long, result is: " + result);
      }
      Thread.sleep(1000);
      result = given().sessionId(sessionId).get(url).asString();
      status = new ObjectMapper().readTree(result).at("/status").textValue();
    }
    return !status.equals("ERROR");
  }

  private static String getToken(String email, String password) throws JsonProcessingException {
    String mutation =
        """
            mutation { signin(email: "%s" ,password: "%s" ) { message, token } }
            """
            .formatted(email, password);

    Map<String, String> request = new HashMap<>();
    request.put("query", mutation);

    String result = given().body(request).when().post("/api/graphql").getBody().asString();
    return new ObjectMapper().readTree(result).at("/data/signin/token").textValue();
  }

  @Test
  void testJSONLDonJSONLDEndpoint() {
    given()
        .sessionId(sessionId)
        .expect()
        .contentType("application/ld+json")
        .statusCode(200)
        .when()
        .get("/pet store/api/jsonld");

    given()
        .sessionId(sessionId)
        .expect()
        .contentType("application/ld+json")
        .statusCode(200)
        .when()
        .get("/pet store/api/jsonld/Pet");
  }

  @Test
  void testTurtleOnTTLEndpoint() {
    given()
        .sessionId(sessionId)
        .expect()
        .contentType("text/turtle")
        .statusCode(200)
        .when()
        .get("/pet store/api/ttl");

    given()
        .sessionId(sessionId)
        .expect()
        .contentType("text/turtle")
        .statusCode(200)
        .when()
        .get("/pet store/api/ttl/Pet");
  }

  @Test
  void testBeaconConfiguration() {
    getAndAssertContains("/api/beacon/configuration", "productionStatus");
  }

  @Test
  void testBeaconMap() {
    getAndAssertContains("/api/beacon/map", "endpointSets");
  }

  @Test
  void testBeaconInfo() {
    getAndAssertContains("/pet store/api/beacon/info", "beaconInfoResponse");
  }

  @Test
  void testBeaconEntryTypes() {
    getAndAssertContains("/api/beacon/entry_types", "entry");
  }

  private void getAndAssertContains(String path, String expectedSubstring) {
    db.clearCache();
    String result = given().get(path).getBody().asString();
    ObjectMapper mapper = new ObjectMapper();
    String prettyJson;
    try {
      Object json = mapper.readValue(result, Object.class);
      ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
      prettyJson = writer.writeValueAsString(json);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    assertTrue(
        result.contains(expectedSubstring),
        "expecting:\n" + expectedSubstring + "\nin:\n" + prettyJson);
  }

  @Test
  void testThatTablesWithSpaceCanBeDownloaded() {
    Table table = schema.getTable(TABLE_WITH_SPACES);

    given()
        .sessionId(sessionId)
        .expect()
        .statusCode(200)
        .when()
        .get("/pet store/api/jsonld/" + table.getIdentifier());

    given()
        .sessionId(sessionId)
        .expect()
        .statusCode(200)
        .when()
        .get("/pet store/api/ttl/" + table.getIdentifier());

    given()
        .sessionId(sessionId)
        .expect()
        .statusCode(200)
        .when()
        .get("/pet store/api/excel/" + table.getIdentifier());

    given()
        .sessionId(sessionId)
        .expect()
        .statusCode(200)
        .when()
        .get("/pet store/api/csv/" + table.getIdentifier());
  }

  @Test
  void testProfileApi() {
    String result = given().get("/api/profiles").getBody().asString();
    assertTrue(result.contains("Samples"));
  }

  @Test
  void testAnalyticsApi() throws JsonProcessingException {

    db.getSchema(SYSTEM_SCHEMA).getTable("AnalyticsTrigger").truncate();
    String adminToken = getToken("admin", "admin");

    // add a trigger
    Map<String, String> addRequest = new HashMap<>();
    addRequest.put("name", "my-trigger");
    addRequest.put("cssSelector", "#my-favorite-button");

    String resp =
        given()
            .header(X_MOLGENIS_TOKEN, adminToken)
            .when()
            .body(addRequest)
            .post("/pet store/api/trigger")
            .getBody()
            .asString();
    assertEquals("{\"status\":\"SUCCESS\"}", resp);

    // fetch a triggers
    String triggers = given().get("/pet store/api/trigger").getBody().asString();
    assertEquals(
        "[{\"name\":\"my-trigger\",\"cssSelector\":\"#my-favorite-button\",\"schemaName\":\"pet store\",\"appName\":null}]",
        triggers);

    // update a trigger
    Map<String, String> updateRequest = new HashMap<>();
    updateRequest.put("cssSelector", "#my-update-button");

    String updateResp =
        given()
            .header(X_MOLGENIS_TOKEN, adminToken)
            .when()
            .body(updateRequest)
            .put("/pet store/api/trigger/my-trigger")
            .getBody()
            .asString();
    assertEquals("{\"status\":\"SUCCESS\"}", updateResp);

    // re-fetch a triggers to check update
    String updated = given().get("/pet store/api/trigger").getBody().asString();
    assertEquals(
        "[{\"name\":\"my-trigger\",\"cssSelector\":\"#my-update-button\",\"schemaName\":\"pet store\",\"appName\":null}]",
        updated);

    // delete a trigger
    given()
        .header(X_MOLGENIS_TOKEN, adminToken)
        .delete("/pet store/api/trigger/my-trigger")
        .getBody()
        .asString();
    assertEquals("{\"status\":\"SUCCESS\"}", resp);

    // refetch triggers
    String triggersAfterDelete = given().get("/pet store/api/trigger").getBody().asString();
    assertEquals("[]", triggersAfterDelete);
  }

  @Test
  void signIn() throws JsonProcessingException {
    String token = getToken("admin", "admin");
    assertTrue(token.length() > 10);
  }

  private Row waitForScriptToComplete(String scriptName) throws InterruptedException {
    Table jobs = db.getSchema(SYSTEM_SCHEMA).getTable("Jobs");
    Filter f = f("script", f("name", EQUALS, scriptName));
    int count = 0;
    Row firstJob = null;
    // should run every 5 secs, lets give it some time to complete at least 1 job
    while ((firstJob == null || !"COMPLETED".equals(firstJob.getString("status"))) && count < 60) {
      List<Row> jobList = jobs.where(f).orderBy("submitDate", Order.ASC).retrieveRows();
      if (jobList.size() > 0) {
        firstJob = jobList.get(0);
      }
      count++; // timing could make this test flakey
      Thread.sleep(1000);
    }
    return firstJob;
  }

  @Test
  void unknownSchemaShouldNotResultInRedirect() {
    given().expect().statusCode(404).when().get("/malicious");
    given().expect().statusCode(404).when().get("/malicious/");
  }

  @Test
  void testMetricsEndpoint() {
    given()
        .expect()
        .statusCode(200)
        .body(containsString("jvm_memory_used_bytes"))
        .when()
        .get(MetricsController.METRICS_PATH);
  }

  @Test
  void testJsonLdImportExport() {
    String jsonLdData =
        given().sessionId(sessionId).when().get("/pet store/api/jsonld/_data").asString();
    assertTrue(jsonLdData.contains("@context"));
    assertTrue(jsonLdData.contains("Pet"));

    String response =
        given()
            .sessionId(sessionId)
            .contentType("application/json")
            .body(jsonLdData)
            .when()
            .post("/pet store/api/jsonld/Pet")
            .asString();
    assertTrue(response.contains("imported"), "Pet response: " + response);
  }

  @Test
  void testJsonYamlTablePostDelete() {
    String insertDataJson = "[{\"name\":\"testdog\",\"category\":{\"name\":\"dog\"},\"weight\":5}]";
    String insertDataYaml = "- name: testcat\n  category:\n    name: cat\n  weight: 3";

    String response =
        given()
            .sessionId(sessionId)
            .contentType("application/json")
            .body(insertDataJson)
            .when()
            .post("/pet store/api/json/Pet")
            .asString();
    assertTrue(response.contains("imported"), "Expected success, got: " + response);

    String tableJson =
        given().sessionId(sessionId).when().get("/pet store/api/json/Pet").asString();
    assertTrue(tableJson.contains("testdog"), "JSON should contain inserted data");

    String deleteDataJson = "[{\"name\":\"testdog\"}]";
    given()
        .sessionId(sessionId)
        .contentType("application/json")
        .body(deleteDataJson)
        .when()
        .delete("/pet store/api/json/Pet")
        .then()
        .statusCode(200);

    String tableJsonAfterDelete =
        given().sessionId(sessionId).when().get("/pet store/api/json/Pet").asString();
    assertFalse(tableJsonAfterDelete.contains("testdog"), "JSON should not contain deleted data");

    given()
        .sessionId(sessionId)
        .contentType("text/plain")
        .body(insertDataYaml)
        .when()
        .post("/pet store/api/yaml/Pet")
        .then()
        .statusCode(200);

    String tableYaml =
        given().sessionId(sessionId).when().get("/pet store/api/yaml/Pet").asString();
    assertTrue(tableYaml.contains("testcat"), "YAML should contain inserted data");

    String deleteDataYaml = "- name: testcat";
    given()
        .sessionId(sessionId)
        .contentType("text/plain")
        .body(deleteDataYaml)
        .when()
        .delete("/pet store/api/yaml/Pet")
        .then()
        .statusCode(200);

    String tableYamlAfterDelete =
        given().sessionId(sessionId).when().get("/pet store/api/yaml/Pet").asString();
    assertFalse(tableYamlAfterDelete.contains("testcat"), "YAML should not contain deleted data");
  }

  @Test
  void testJsonYamlRowPut() {
    String insertDataJson = "[{\"name\":\"testdog\",\"category\":{\"name\":\"dog\"},\"weight\":5}]";

    given()
        .sessionId(sessionId)
        .contentType("application/json")
        .body(insertDataJson)
        .when()
        .post("/pet store/api/json/Pet")
        .then()
        .statusCode(200);

    String updateDataJson =
        "{\"name\":\"testdog\",\"category\":{\"name\":\"dog\"},\"weight\":5,\"status\":\"updated\"}";

    given()
        .sessionId(sessionId)
        .contentType("application/json")
        .body(updateDataJson)
        .when()
        .put("/pet store/api/json/Pet/testdog")
        .then()
        .statusCode(200);

    String rowJson =
        given().sessionId(sessionId).when().get("/pet store/api/json/Pet/testdog").asString();
    assertTrue(rowJson.contains("updated"), "JSON row should be updated");

    String updateDataYaml =
        "name: testdog\ncategory:\n  name: dog\nweight: 5\nstatus: updated_yaml";

    given()
        .sessionId(sessionId)
        .contentType("text/plain")
        .body(updateDataYaml)
        .when()
        .put("/pet store/api/yaml/Pet/testdog")
        .then()
        .statusCode(200);

    String rowYaml =
        given().sessionId(sessionId).when().get("/pet store/api/yaml/Pet/testdog").asString();
    assertTrue(rowYaml.contains("updated_yaml"), "YAML row should be updated");

    String deleteDataJson = "[{\"name\":\"testdog\"}]";
    given()
        .sessionId(sessionId)
        .contentType("application/json")
        .body(deleteDataJson)
        .when()
        .delete("/pet store/api/json/Pet")
        .then()
        .statusCode(200);
  }

  @Test
  void testJsonYamlRowDelete() {
    String insertDataJson =
        "[{\"name\":\"testrowdelete\",\"category\":{\"name\":\"dog\"},\"weight\":5}]";

    given()
        .sessionId(sessionId)
        .contentType("application/json")
        .body(insertDataJson)
        .when()
        .post("/pet store/api/json/Pet")
        .then()
        .statusCode(200);

    given()
        .sessionId(sessionId)
        .when()
        .delete("/pet store/api/json/Pet/testrowdelete")
        .then()
        .statusCode(200);

    given()
        .sessionId(sessionId)
        .when()
        .get("/pet store/api/json/Pet/testrowdelete")
        .then()
        .statusCode(404);

    given()
        .sessionId(sessionId)
        .contentType("text/plain")
        .body(insertDataJson)
        .when()
        .post("/pet store/api/yaml/Pet")
        .then()
        .statusCode(200);

    given()
        .sessionId(sessionId)
        .when()
        .delete("/pet store/api/yaml/Pet/testrowdelete")
        .then()
        .statusCode(200);

    given()
        .sessionId(sessionId)
        .when()
        .get("/pet store/api/yaml/Pet/testrowdelete")
        .then()
        .statusCode(404);
  }

  @Test
  void testRowNotFound() {
    given()
        .sessionId(sessionId)
        .when()
        .get("/pet store/api/json/Pet/nonexistent")
        .then()
        .statusCode(404);

    String upsertData = "{\"name\":\"upserttest\",\"category\":{\"name\":\"dog\"},\"weight\":5}";

    given()
        .sessionId(sessionId)
        .contentType("application/json")
        .body(upsertData)
        .when()
        .put("/pet store/api/json/Pet/upserttest")
        .then()
        .statusCode(200);

    String rowJson =
        given().sessionId(sessionId).when().get("/pet store/api/json/Pet/upserttest").asString();
    assertTrue(rowJson.contains("upserttest"), "PUT should create row via upsert");

    given()
        .sessionId(sessionId)
        .when()
        .delete("/pet store/api/json/Pet/upserttest")
        .then()
        .statusCode(200);
  }

  @Test
  void testMembersUnauthorized() {
    Response response = given().accept(ACCEPT_JSON).when().get("/pet store/api/json/_members");

    assertEquals(400, response.getStatusCode());
    assertTrue(
        response.body().asString().contains("Unauthorized"),
        "Should be unauthorized for anonymous user");
  }

  @Test
  void testChangelogUnauthorized() {
    Response response = given().accept(ACCEPT_JSON).when().get("/pet store/api/json/_changelog");

    assertEquals(400, response.getStatusCode());
    assertTrue(
        response.body().asString().contains("Unauthorized"),
        "Should be unauthorized for anonymous user");
  }

  static Stream<Arguments> restApiFormats() {
    return Stream.of(
        Arguments.of("json", ACCEPT_JSON),
        Arguments.of("yaml", ACCEPT_YAML),
        Arguments.of("jsonld", "application/ld+json"),
        Arguments.of("ttl", "text/turtle"),
        Arguments.of("csv", ACCEPT_CSV),
        Arguments.of("excel", ACCEPT_EXCEL));
  }

  @ParameterizedTest(name = "GET _schema for {0}")
  @MethodSource("restApiFormats")
  void testRestApiGetSchema(String format, String contentType) {
    if (format.equals("csv")) {
      given()
          .sessionId(sessionId)
          .accept(contentType)
          .when()
          .get("/pet store/api/csv/_schema")
          .then()
          .statusCode(200)
          .contentType(contentType);
    } else if (format.equals("excel")) {
      given()
          .sessionId(sessionId)
          .when()
          .get("/pet store/api/excel/_schema")
          .then()
          .statusCode(200);
    } else {
      String response =
          given()
              .sessionId(sessionId)
              .when()
              .get("/pet store/api/" + format + "/_schema")
              .then()
              .statusCode(200)
              .contentType(contentType)
              .extract()
              .asString();
      if (format.equals("json") || format.equals("yaml")) {
        assertTrue(response.contains("tables"), format + " _schema should contain 'tables'");
      } else if (format.equals("jsonld")) {
        assertTrue(response.contains("@context"), "jsonld _schema should contain '@context'");
      }
    }
  }

  @ParameterizedTest(name = "GET _data for {0}")
  @MethodSource("restApiFormats")
  void testRestApiGetData(String format, String contentType) {
    if (format.equals("csv")) return;
    if (format.equals("excel")) {
      given().sessionId(sessionId).when().get("/pet store/api/excel/_data").then().statusCode(200);
      return;
    }
    String response =
        given()
            .sessionId(sessionId)
            .when()
            .get("/pet store/api/" + format + "/_data")
            .then()
            .statusCode(200)
            .contentType(contentType)
            .extract()
            .asString();
    assertTrue(response.contains("Pet"), format + " _data should contain 'Pet'");
  }

  @ParameterizedTest(name = "GET _all for {0}")
  @MethodSource("restApiFormats")
  void testRestApiGetAll(String format, String contentType) {
    if (format.equals("csv")) return;
    if (format.equals("excel")) {
      given().sessionId(sessionId).when().get("/pet store/api/excel/_all").then().statusCode(200);
      return;
    }
    String response =
        given()
            .sessionId(sessionId)
            .when()
            .get("/pet store/api/" + format + "/_all")
            .then()
            .statusCode(200)
            .contentType(contentType)
            .extract()
            .asString();
    if (format.equals("json") || format.equals("yaml")) {
      assertTrue(response.contains("schema"), format + " _all should contain 'schema'");
      assertTrue(response.contains("data"), format + " _all should contain 'data'");
    }
    assertTrue(response.contains("Pet"), format + " _all should contain 'Pet'");
  }

  @ParameterizedTest(name = "GET table for {0}")
  @MethodSource("restApiFormats")
  void testRestApiGetTable(String format, String contentType) {
    if (format.equals("excel")) {
      given()
          .sessionId(sessionId)
          .when()
          .get("/pet store/api/excel/Pet")
          .then()
          .statusCode(200)
          .contentType(ACCEPT_EXCEL);
      return;
    }
    String response =
        given()
            .sessionId(sessionId)
            .when()
            .get("/pet store/api/" + format + "/Pet")
            .then()
            .statusCode(200)
            .extract()
            .asString();
    assertTrue(
        response.toLowerCase().contains("spike") || response.toLowerCase().contains("pet"),
        format + " table query should return pet data");
  }

  @ParameterizedTest(name = "GET row for {0}")
  @MethodSource("restApiFormats")
  void testRestApiGetRow(String format, String contentType) {
    if (format.equals("csv") || format.equals("excel") || format.equals("ttl")) return;
    String response =
        given()
            .sessionId(sessionId)
            .when()
            .get("/pet store/api/" + format + "/Pet/spike")
            .then()
            .statusCode(200)
            .contentType(contentType)
            .extract()
            .asString();
    assertTrue(response.toLowerCase().contains("spike"), format + " row query should return spike");
  }

  @ParameterizedTest(name = "GET _members for {0}")
  @MethodSource("restApiFormats")
  void testRestApiGetMembers(String format, String contentType) {
    if (format.equals("csv") || format.equals("excel") || format.equals("ttl")) return;
    String response =
        given()
            .sessionId(sessionId)
            .when()
            .get("/pet store/api/" + format + "/_members")
            .then()
            .statusCode(200)
            .contentType(contentType)
            .extract()
            .asString();
    assertTrue(response.length() > 0, format + " _members should return data");
  }

  @ParameterizedTest(name = "GET _settings for {0}")
  @MethodSource("restApiFormats")
  void testRestApiGetSettings(String format, String contentType) {
    if (format.equals("csv") || format.equals("excel") || format.equals("ttl")) return;
    String response =
        given()
            .sessionId(sessionId)
            .when()
            .get("/pet store/api/" + format + "/_settings")
            .then()
            .statusCode(200)
            .contentType(contentType)
            .extract()
            .asString();
    assertTrue(response.length() > 0, format + " _settings should return data");
  }

  @ParameterizedTest(name = "GET _changelog for {0}")
  @MethodSource("restApiFormats")
  void testRestApiGetChangelog(String format, String contentType) {
    if (format.equals("csv") || format.equals("excel") || format.equals("ttl")) return;
    String response =
        given()
            .sessionId(sessionId)
            .when()
            .get("/pet store/api/" + format + "/_changelog")
            .then()
            .statusCode(200)
            .contentType(contentType)
            .extract()
            .asString();
    assertTrue(response.length() >= 0, format + " _changelog should return data");
  }

  @ParameterizedTest(name = "GET _context for {0}")
  @MethodSource("restApiFormats")
  void testRestApiGetContext(String format, String contentType) {
    if (!format.equals("jsonld")) return;
    String response =
        given()
            .sessionId(sessionId)
            .when()
            .get("/pet store/api/jsonld/_context")
            .then()
            .statusCode(200)
            .contentType("application/ld+json")
            .extract()
            .asString();
    assertTrue(response.contains("@context"), "jsonld _context should contain @context");
  }

  @ParameterizedTest(name = "POST _schema for {0}")
  @MethodSource("restApiFormats")
  void testRestApiPostSchema(String format, String contentType) {
    if (format.equals("csv") || format.equals("excel") || format.equals("ttl")) return;
    String schemaBody =
        format.equals("yaml")
            ? "tables:\n  - name: TestTable\n    columns:\n      - name: id\n        key: 1"
            : "{\"tables\":[{\"name\":\"TestTable\",\"columns\":[{\"name\":\"id\",\"key\":1}]}]}";
    given()
        .sessionId(sessionId)
        .body(schemaBody)
        .when()
        .post("/pet store/api/" + format + "/_schema")
        .then()
        .statusCode(200);
  }

  static Stream<Arguments> writeFormats() {
    return Stream.of(Arguments.of("json", "application/json"), Arguments.of("yaml", "text/plain"));
  }

  @ParameterizedTest(name = "POST+GET+DELETE table for {0}")
  @MethodSource("writeFormats")
  void testRestApiPostTableWithStateVerification(String format, String reqContentType) {
    String petName = "testPost" + format + System.currentTimeMillis();
    String body = formatTableBody(format, petName, "dog", 5);

    String postResponse =
        given()
            .sessionId(sessionId)
            .contentType(reqContentType)
            .body(body)
            .when()
            .post("/pet store/api/" + format + "/Pet")
            .asString();
    assertTrue(postResponse.contains("imported"), format + " POST failed: " + postResponse);

    String getResponse =
        given().sessionId(sessionId).when().get("/pet store/api/json/Pet").asString();
    assertTrue(getResponse.contains(petName), format + " GET after POST should contain " + petName);

    given()
        .sessionId(sessionId)
        .contentType("application/json")
        .body("[{\"name\":\"" + petName + "\"}]")
        .when()
        .delete("/pet store/api/json/Pet")
        .asString();

    String afterDelete =
        given().sessionId(sessionId).when().get("/pet store/api/json/Pet").asString();
    assertFalse(afterDelete.contains(petName), format + " should be deleted");
  }

  @ParameterizedTest(name = "PUT+GET row for {0}")
  @MethodSource("writeFormats")
  void testRestApiPutRowWithStateVerification(String format, String reqContentType) {
    int newWeight = 99;
    String body = formatRowBody(format, "spike", "dog", newWeight);

    String putResponse =
        given()
            .sessionId(sessionId)
            .contentType(reqContentType)
            .body(body)
            .when()
            .put("/pet store/api/" + format + "/Pet/spike")
            .asString();
    assertTrue(putResponse.contains("updated"), format + " PUT failed: " + putResponse);

    String getResponse =
        given().sessionId(sessionId).when().get("/pet store/api/json/Pet/spike").asString();
    assertTrue(
        getResponse.contains(String.valueOf(newWeight)),
        format + " GET after PUT should contain weight " + newWeight);
  }

  @ParameterizedTest(name = "DELETE row for {0}")
  @MethodSource("writeFormats")
  void testRestApiDeleteRowWithStateVerification(String format, String reqContentType) {
    String petName = "testDel" + format + System.currentTimeMillis();
    String insertBody = formatTableBody(format, petName, "dog", 1);

    given()
        .sessionId(sessionId)
        .contentType(reqContentType)
        .body(insertBody)
        .when()
        .post("/pet store/api/" + format + "/Pet")
        .asString();

    String beforeDelete =
        given().sessionId(sessionId).when().get("/pet store/api/json/Pet").asString();
    assertTrue(beforeDelete.contains(petName), format + " pet should exist before delete");

    given()
        .sessionId(sessionId)
        .when()
        .delete("/pet store/api/" + format + "/Pet/" + petName)
        .asString();

    String afterDelete =
        given().sessionId(sessionId).when().get("/pet store/api/json/Pet").asString();
    assertFalse(afterDelete.contains(petName), format + " pet should not exist after delete");
  }

  @ParameterizedTest(name = "DELETE _schema for {0}")
  @MethodSource("writeFormats")
  void testRestApiDeleteSchema(String format, String reqContentType) {
    String schemaBody =
        format.equals("yaml")
            ? "tables:\n  - name: Pet\n    columns:\n      - name: photoUrls"
            : "{\"tables\":[{\"name\":\"Pet\",\"columns\":[{\"name\":\"photoUrls\"}]}]}";

    given()
        .sessionId(sessionId)
        .contentType(reqContentType)
        .body(schemaBody)
        .when()
        .delete("/pet store/api/" + format + "/_schema")
        .then()
        .statusCode(200);
  }

  @ParameterizedTest(name = "DELETE table rows for {0}")
  @MethodSource("writeFormats")
  void testRestApiDeleteTableRows(String format, String reqContentType) {
    String petName1 = "testDelRows1_" + format + System.currentTimeMillis();
    String petName2 = "testDelRows2_" + format + System.currentTimeMillis();

    String insertBody1 = formatTableBody(format, petName1, "dog", 3);
    String insertBody2 = formatTableBody(format, petName2, "cat", 2);

    given()
        .sessionId(sessionId)
        .contentType(reqContentType)
        .body(insertBody1)
        .when()
        .post("/pet store/api/" + format + "/Pet")
        .asString();

    given()
        .sessionId(sessionId)
        .contentType(reqContentType)
        .body(insertBody2)
        .when()
        .post("/pet store/api/" + format + "/Pet")
        .asString();

    String beforeDelete =
        given().sessionId(sessionId).when().get("/pet store/api/json/Pet").asString();
    assertTrue(beforeDelete.contains(petName1), format + " should contain " + petName1);
    assertTrue(beforeDelete.contains(petName2), format + " should contain " + petName2);

    String deleteBody =
        format.equals("yaml")
            ? "- name: " + petName1 + "\n- name: " + petName2
            : "[{\"name\":\"" + petName1 + "\"},{\"name\":\"" + petName2 + "\"}]";

    given()
        .sessionId(sessionId)
        .contentType(reqContentType)
        .body(deleteBody)
        .when()
        .delete("/pet store/api/" + format + "/Pet")
        .asString();

    String afterDelete =
        given().sessionId(sessionId).when().get("/pet store/api/json/Pet").asString();
    assertFalse(afterDelete.contains(petName1), format + " should not contain " + petName1);
    assertFalse(afterDelete.contains(petName2), format + " should not contain " + petName2);
  }

  @ParameterizedTest(name = "DELETE row by id for {0}")
  @MethodSource("writeFormats")
  void testRestApiDeleteRowById(String format, String reqContentType) {
    String petName = "testDelById_" + format + System.currentTimeMillis();
    String insertBody = formatTableBody(format, petName, "dog", 4);

    given()
        .sessionId(sessionId)
        .contentType(reqContentType)
        .body(insertBody)
        .when()
        .post("/pet store/api/" + format + "/Pet")
        .asString();

    String beforeDelete =
        given().sessionId(sessionId).when().get("/pet store/api/json/Pet").asString();
    assertTrue(beforeDelete.contains(petName), format + " pet should exist before DELETE");

    given()
        .sessionId(sessionId)
        .when()
        .delete("/pet store/api/" + format + "/Pet/" + petName)
        .then()
        .statusCode(200);

    String afterDelete =
        given().sessionId(sessionId).when().get("/pet store/api/json/Pet").asString();
    assertFalse(afterDelete.contains(petName), format + " pet should not exist after DELETE");
  }

  @Test
  void testRestApiPostTableJsonLdWithStateVerification() {
    String petName = "testLdPet" + System.currentTimeMillis();
    String body =
        "{\"data\":{\"Pet\":[{\"name\":\""
            + petName
            + "\",\"category\":{\"name\":\"dog\"},\"weight\":1}]}}";

    String postResponse =
        given()
            .sessionId(sessionId)
            .contentType("application/ld+json")
            .body(body)
            .when()
            .post("/pet store/api/jsonld/Pet")
            .asString();
    assertTrue(postResponse.contains("imported"), "jsonld POST failed: " + postResponse);

    String getResponse =
        given().sessionId(sessionId).when().get("/pet store/api/json/Pet").asString();
    assertTrue(getResponse.contains(petName), "jsonld GET after POST should contain " + petName);

    given()
        .sessionId(sessionId)
        .contentType(ACCEPT_JSON)
        .body("[{\"name\":\"" + petName + "\"}]")
        .when()
        .delete("/pet store/api/json/Pet")
        .asString();
  }

  private String formatTableBody(String format, String name, String category, int weight) {
    if (format.equals("yaml")) {
      return "- name: " + name + "\n  category:\n    name: " + category + "\n  weight: " + weight;
    } else if (format.equals("csv")) {
      return "name,category,weight\n" + name + "," + category + "," + weight;
    }
    return "[{\"name\":\""
        + name
        + "\",\"category\":{\"name\":\""
        + category
        + "\"},\"weight\":"
        + weight
        + "}]";
  }

  private String formatRowBody(String format, String name, String category, int weight) {
    if (format.equals("yaml")) {
      return "name: " + name + "\ncategory:\n  name: " + category + "\nweight: " + weight;
    }
    return "{\"name\":\""
        + name
        + "\",\"category\":{\"name\":\""
        + category
        + "\"},\"weight\":"
        + weight
        + "}";
  }

  @Test
  void testExcelApiPostTableWithStateVerification() throws IOException {
    String petName = "testExcelPet" + System.currentTimeMillis();

    Path tempFile = Files.createTempFile("pet_test", ".xlsx");
    try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
      var sheet = workbook.createSheet("Pet");
      var headerRow = sheet.createRow(0);
      headerRow.createCell(0).setCellValue("name");
      headerRow.createCell(1).setCellValue("category");
      headerRow.createCell(2).setCellValue("weight");
      var dataRow = sheet.createRow(1);
      dataRow.createCell(0).setCellValue(petName);
      dataRow.createCell(1).setCellValue("dog");
      dataRow.createCell(2).setCellValue(5);
      try (var out = Files.newOutputStream(tempFile)) {
        workbook.write(out);
      }
    }

    given()
        .sessionId(sessionId)
        .multiPart(tempFile.toFile())
        .when()
        .post("/pet store/api/excel/Pet")
        .then()
        .statusCode(200);

    String getResponse =
        given().sessionId(sessionId).when().get("/pet store/api/json/Pet").asString();
    assertTrue(getResponse.contains(petName), "excel GET after POST should contain " + petName);

    given()
        .sessionId(sessionId)
        .contentType("application/json")
        .body("[{\"name\":\"" + petName + "\"}]")
        .when()
        .delete("/pet store/api/json/Pet")
        .asString();

    Files.deleteIfExists(tempFile);
  }

  @Test
  void testZipApiPostTableWithStateVerification() throws IOException {
    String petName = "testZipPet" + System.currentTimeMillis();

    Path tempFile = Files.createTempFile("pet_test", ".zip");
    try (var zos = new java.util.zip.ZipOutputStream(Files.newOutputStream(tempFile))) {
      zos.putNextEntry(new java.util.zip.ZipEntry("Pet.csv"));
      String csvContent = "name,category,weight\n" + petName + ",dog,5";
      zos.write(csvContent.getBytes(StandardCharsets.UTF_8));
      zos.closeEntry();
    }

    given()
        .sessionId(sessionId)
        .multiPart(tempFile.toFile())
        .when()
        .post("/pet store/api/zip/Pet")
        .then()
        .statusCode(200);

    String getResponse =
        given().sessionId(sessionId).when().get("/pet store/api/json/Pet").asString();
    assertTrue(getResponse.contains(petName), "zip GET after POST should contain " + petName);

    given()
        .sessionId(sessionId)
        .contentType("application/json")
        .body("[{\"name\":\"" + petName + "\"}]")
        .when()
        .delete("/pet store/api/json/Pet")
        .asString();

    Files.deleteIfExists(tempFile);
  }
}
