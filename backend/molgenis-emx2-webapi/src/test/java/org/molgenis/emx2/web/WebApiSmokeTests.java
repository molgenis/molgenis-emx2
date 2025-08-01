package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.Constants.SYSTEM_SCHEMA;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.TestResourceLoader.getFileAsString;
import static org.molgenis.emx2.datamodels.DataModels.Profile.PET_STORE;
import static org.molgenis.emx2.sql.SqlDatabase.*;
import static org.molgenis.emx2.web.Constants.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.Assert;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSender;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.Order;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInZipFile;
import org.molgenis.emx2.io.tablestore.TableStoreForXlsxFile;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.utils.EnvironmentProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* this is a smoke test for the integration of web api with the database layer. So not complete coverage of all services but only a few essential requests to pass most endpoints */
@TestMethodOrder(MethodOrderer.MethodName.class)
@Tag("slow")
public class WebApiSmokeTests {

  static final Logger logger = LoggerFactory.getLogger(WebApiSmokeTests.class);

  private static final String EXCEPTION_CONTENT_TYPE = "application/json";

  public static final String DATA_PET_STORE = "/pet store/api/csv";
  public static final String PET_SHOP_OWNER = "pet_shop_owner";
  public static final String PET_SHOP_VIEWER = "shopviewer";
  public static final String PET_SHOP_MANAGER = "shopmanager";
  public static final String SYSTEM_PREFIX = "/" + SYSTEM_SCHEMA;
  public static final String TABLE_WITH_SPACES = "table with spaces";
  public static final String PET_STORE_SCHEMA = "pet store";
  public static String SESSION_ID; // to toss around a session for the tests
  private static Database db;
  private static Schema schema;
  final String CSV_TEST_SCHEMA = "pet store csv";
  static final int PORT = 8081; // other than default so we can see effect

  @BeforeAll
  public static void before() throws Exception {
    // FIXME: beforeAll fails under windows
    // setup test schema
    db = TestDatabaseFactory.getTestDatabase();

    // start web service for testing, including env variables
    RunMolgenisEmx2.main(new String[] {String.valueOf(PORT)});

    // set default rest assured settings
    RestAssured.port = PORT;
    RestAssured.baseURI = "http://localhost";

    // create an admin session to work with
    String adminPass =
        (String)
            EnvironmentProperty.getParameter(
                org.molgenis.emx2.Constants.MOLGENIS_ADMIN_PW, ADMIN_PW_DEFAULT, STRING);
    SESSION_ID =
        given()
            .body(
                "{\"query\":\"mutation{signin(email:\\\""
                    + db.getAdminUserName()
                    + "\\\",password:\\\""
                    + adminPass
                    + "\\\"){message}}\"}")
            .when()
            .post("api/graphql")
            .sessionId();

    // Always create test database from scratch to avoid instability due to side effects.
    db.dropSchemaIfExists(PET_STORE_SCHEMA);
    schema = db.createSchema(PET_STORE_SCHEMA);
    PET_STORE.getImportTask(schema, true).run();

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

  @AfterAll
  public static void after() {
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

    given().sessionId(SESSION_ID).body(createUserQuery).post("/api/graphql").asString();

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
                  given().sessionId(SESSION_ID).body(signinQuery).post("/api/graphql").asString();

              try {
                assertTrue(
                    signinResult.contains("Signed in"),
                    "Login failed in thread: " + Thread.currentThread().getName());
              } catch (AssertionError e) {
                logger.warn("[Thread {}] {}", Thread.currentThread().getName(), e.getMessage());
              }

              String sessionResult =
                  given().sessionId(SESSION_ID).body(sessionQuery).post("/api/graphql").asString();

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
  }

  @Test
  public void testApiRoot() {
    String result =
        given()
            .sessionId(SESSION_ID)
            .expect()
            .statusCode(200)
            .when()
            .get("/api")
            .getBody()
            .asString();
    assertTrue(result.contains("Welcome to MOLGENIS EMX2"));
  }

  @Test
  public void testCsvApi_zipUploadDownload() throws IOException {
    // get original schema
    String schemaCsv =
        given().sessionId(SESSION_ID).accept(ACCEPT_CSV).when().get(DATA_PET_STORE).asString();

    // create a new schema for zip
    db.dropCreateSchema("pet store zip");

    // download zip contents of old schema
    byte[] zipContents = getContentAsByteArray(ACCEPT_ZIP, "/pet store/api/zip");

    // upload zip contents into new schema
    File zipFile = createTempFile(zipContents, ".zip");
    given()
        .sessionId(SESSION_ID)
        .multiPart(zipFile)
        .when()
        .post("/pet store zip/api/zip")
        .then()
        .statusCode(200);

    // check if schema equal using json representation
    String schemaCsv2 =
        given()
            .sessionId(SESSION_ID)
            .accept(ACCEPT_CSV)
            .when()
            .get("/pet store zip/api/csv")
            .asString();
    assertArrayEquals(toSortedArray(schemaCsv), toSortedArray(schemaCsv2));

    // delete the new schema
    db.dropSchema("pet store zip");
  }

  @Test
  public void testReports() throws IOException {
    // create a new schema for report
    Schema schema = db.dropCreateSchema("pet store reports");
    PET_STORE.getImportTask(schema, true).run();

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
            .sessionId(SESSION_ID)
            .get("/pet store reports/api/reports/json?id=report1")
            .asString();
    assertFalse(
        jsonResults.contains("report1"),
        "single result should not include report name"); // are we sure about this?
    jsonResults =
        given()
            .sessionId(SESSION_ID)
            .get("/pet store reports/api/reports/json?id=report1,report2&name=pooky")
            .asString();
    assertTrue(
        jsonResults.contains("report1"),
        "multiple results should use the report name to nest results");
    // check that id is for keys
    jsonResults =
        given()
            .sessionId(SESSION_ID)
            .get("/pet store reports/api/reports/json?id=report1,report2&name=pooky")
            .asString();
    assertTrue(jsonResults.contains("report1"), "should use report id as key");
    assertTrue(jsonResults.contains("report2"), "should use report id as key");

    jsonResults =
        given()
            .sessionId(SESSION_ID)
            .get("/pet store reports/api/reports/json?id=report2&name=spike,pooky")
            .asString();
    assertTrue(jsonResults.contains("pooky"));

    // test report using jsonb_agg
    jsonResults =
        given()
            .sessionId(SESSION_ID)
            .get("/pet store reports/api/reports/json?id=report3")
            .asString();
    ObjectMapper objectMapper = new ObjectMapper();
    List<Object> jsonbResult = objectMapper.readValue(jsonResults, List.class);
    assertTrue(jsonbResult.get(0).toString().contains("pooky"));

    // test report using jsonb rows
    jsonResults =
        given()
            .sessionId(SESSION_ID)
            .get("/pet store reports/api/reports/json?id=report4")
            .asString();
    Object result = objectMapper.readValue(jsonResults, Object.class);
    assertTrue(result.toString().contains("pooky"));

    // test report using json objects
    jsonResults =
        given()
            .sessionId(SESSION_ID)
            .get("/pet store reports/api/reports/json?id=report5")
            .asString();
    Object jsonResult = objectMapper.readValue(jsonResults, Object.class);
    assertTrue(jsonResult.toString().contains("pooky"));

    jsonResults =
        given()
            .sessionId(SESSION_ID)
            .get("/pet store reports/api/reports/json?id=report4,report5")
            .asString();
    Map<String, Object> multipleResults = objectMapper.readValue(jsonResults, Map.class);
    // Check if multiple result are returned as proper json
    assertFalse(multipleResults.get("report4").toString().startsWith("{\""));
  }

  @Test
  public void testCsvApi_csvTableMetadataUpdate() throws IOException {

    // fresh schema for testing
    db.dropCreateSchema(CSV_TEST_SCHEMA);

    // full table header present in exported table metadata
    String header =
        "tableName,tableExtends,tableType,columnName,columnType,key,required,readonly,refSchema,refTable,refLink,refBack,refLabel,defaultValue,validation,visible,computed,semantics,profiles,label,description\r\n";

    // add new table with description and semantics as metadata
    addUpdateTableAndCompare(
        header,
        "tableName,description,semantics\r\nTestMetaTable,TestDesc,TestSem",
        "TestMetaTable,,,,,,,,,,,,,,,,,TestSem,,,TestDesc\r\n");

    // update table without new description or semantics, values should be untouched
    addUpdateTableAndCompare(
        header,
        "tableName\r\nTestMetaTable",
        "TestMetaTable,,,,,,,,,,,,,,,,,TestSem,,,TestDesc\r\n");

    // update only description, semantics should be untouched
    addUpdateTableAndCompare(
        header,
        "tableName,description\r\nTestMetaTable,NewTestDesc",
        "TestMetaTable,,,,,,,,,,,,,,,,,TestSem,,,NewTestDesc\r\n");

    // make semantics empty by not supplying a value, description  should be untouched
    addUpdateTableAndCompare(
        header,
        "tableName,semantics\r\nTestMetaTable,",
        "TestMetaTable,,,,,,,,,,,,,,,,,,,,NewTestDesc\r\n");

    // make description empty while also adding a new value for semantics
    addUpdateTableAndCompare(
        header,
        "tableName,description,semantics\r\nTestMetaTable,,NewTestSem",
        "TestMetaTable,,,,,,,,,,,,,,,,,NewTestSem,,,\r\n");

    // empty both description and semantics
    addUpdateTableAndCompare(
        header,
        "tableName,description,semantics\r\nTestMetaTable,,",
        "TestMetaTable,,,,,,,,,,,,,,,,,,,,\r\n");

    // add description value, and string array value for semantics
    addUpdateTableAndCompare(
        header,
        "tableName,description,semantics\r\nTestMetaTable,TestDesc,\"TestSem1,TestSem2\"",
        "TestMetaTable,,,,,,,,,,,,,,,,,\"TestSem1,TestSem2\",,,TestDesc\r\n");
  }

  /** Helper function to prevent code duplication */
  private void addUpdateTableAndCompare(String header, String tableMeta, String expected)
      throws IOException {
    byte[] addUpdateTable = tableMeta.getBytes(StandardCharsets.UTF_8);
    File addUpdateTableFile = createTempFile(addUpdateTable, ".csv");
    acceptFileUpload(addUpdateTableFile, "molgenis", false);
    String actual = getContentAsString("/api/csv");
    assertEquals(header + expected, actual);
  }

  @Test
  public void testCsvApi_csvUploadDownload() throws IOException {
    // create a new schema for complete csv data round trip
    db.dropCreateSchema(CSV_TEST_SCHEMA);

    // download csv metadata and data from existing schema
    byte[] contentsMeta = getContentAsByteArray(ACCEPT_CSV, "/pet store/api/csv");
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
    String contentsMetaNew = getContentAsString("/api/csv");
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
  public void testCsvApi_tableFilter() {
    String result =
        given()
            .sessionId(SESSION_ID)
            .queryParam("filter", "{\"name\":{\"equals\":\"pooky\"}}")
            .accept(ACCEPT_CSV)
            .when()
            .get("/pet store/api/csv/Pet")
            .asString();
    assertTrue(result.contains("pooky"));
    assertFalse(result.contains("spike"));

    result =
        given()
            .sessionId(SESSION_ID)
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
            .sessionId(SESSION_ID)
            .body(content)
            .header("fileName", table)
            .when()
            .post("/" + CSV_TEST_SCHEMA + "/api/csv" + (async ? "?async=true" : ""));

    response.then().statusCode(200);

    return response.asString();
  }

  private String getContentAsString(String path) {
    return given()
        .sessionId(SESSION_ID)
        .accept(ACCEPT_CSV)
        .when()
        .get("/" + CSV_TEST_SCHEMA + path)
        .asString();
  }

  private byte[] getContentAsByteArray(String fileType, String path) {
    return given().sessionId(SESSION_ID).accept(fileType).when().get(path).asByteArray();
  }

  @Test
  public void testJsonYamlApi() {
    String schemaJson = given().sessionId(SESSION_ID).when().get("/pet store/api/json").asString();

    db.dropCreateSchema("pet store json");

    given()
        .sessionId(SESSION_ID)
        .body(schemaJson)
        .when()
        .post("/pet store json/api/json")
        .then()
        .statusCode(200);

    String schemaJson2 =
        given().sessionId(SESSION_ID).when().get("/pet store json/api/json").asString();

    assertEquals(schemaJson, schemaJson2.replace("pet store json", PET_STORE_SCHEMA));

    String schemaYaml = given().sessionId(SESSION_ID).when().get("/pet store/api/yaml").asString();

    db.dropCreateSchema("pet store yaml");

    given()
        .sessionId(SESSION_ID)
        .body(schemaYaml)
        .when()
        .post("/pet store yaml/api/yaml")
        .then()
        .statusCode(200);

    String schemaYaml2 =
        given().sessionId(SESSION_ID).when().get("/pet store yaml/api/yaml").asString();

    assertEquals(schemaYaml, schemaYaml2.replace("pet store yaml", PET_STORE_SCHEMA));

    given()
        .sessionId(SESSION_ID)
        .body(schemaYaml2)
        .when()
        .delete("/pet store yaml/api/yaml")
        .then()
        .statusCode(200);

    given()
        .sessionId(SESSION_ID)
        .body(schemaJson2)
        .when()
        .delete("/pet store json/api/json")
        .then()
        .statusCode(200);

    db.dropSchemaIfExists("pet store yaml");
    db.dropSchemaIfExists("pet store json");
  }

  @Test
  public void testExcelApi() throws IOException, InterruptedException {

    // download json schema
    String schemaCSV =
        given()
            .sessionId(SESSION_ID)
            .accept(ACCEPT_CSV)
            .when()
            .get("/pet store/api/csv")
            .asString();

    // create a new schema for excel
    db.dropCreateSchema("pet store excel");

    // download excel contents from schema
    byte[] excelContents = getContentAsByteArray(ACCEPT_EXCEL, "/pet store/api/excel");
    File excelFile = createTempFile(excelContents, ".xlsx");

    // upload excel into new schema
    String message =
        given()
            .sessionId(SESSION_ID)
            .multiPart(excelFile)
            .when()
            .post("/pet store excel/api/excel?async=true")
            .asString();

    Map<String, String> val = new ObjectMapper().readValue(message, Map.class);
    String url = val.get("url");
    String id = val.get("id");

    // poll task until complete
    Response poll = given().sessionId(SESSION_ID).when().get(url);
    int count = 0;
    // poll while running
    // (previously we checked on 'complete' but then it also fired if subtask was complete)
    while (poll.body().asString().contains("UNKNOWN")
        || poll.body().asString().contains("RUNNING")) {
      if (count++ > 100) {
        throw new MolgenisException("failed: polling took too long");
      }
      poll = given().sessionId(SESSION_ID).when().get(url);
      Thread.sleep(500);
    }
    assertFalse(
        poll.body().asString().contains("FAILED") || poll.body().asString().contains("ERROR"));

    // check if id in tasks list
    assertTrue(
        given()
            .sessionId(SESSION_ID)
            .multiPart(excelFile)
            .when()
            .get("/pet store/api/tasks")
            .asString()
            .contains(id));

    // check if schema equal using json representation
    String schemaCSV2 =
        given()
            .sessionId(SESSION_ID)
            .accept(ACCEPT_CSV)
            .when()
            .get("/pet store excel/api/csv")
            .asString();

    assertTrue(schemaCSV2.contains("Pet"));

    // delete a new schema for excel
    db.dropSchema("pet store excel");
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
  public void testCsvApi_tableCsvUploadDownload() {

    String path = "/pet store/api/csv/Tag";

    String result = given().sessionId(SESSION_ID).accept(ACCEPT_CSV).when().get(path).asString();
    assertTrue(result.contains("green,,,colors"));

    String update = "name,parent\r\nyellow,colors\r\n";
    given().sessionId(SESSION_ID).body(update).when().post(path).then().statusCode(200);

    result = given().sessionId(SESSION_ID).accept(ACCEPT_CSV).when().get(path).asString();
    assertTrue(result.contains("yellow"));

    given().sessionId(SESSION_ID).body(update).when().delete(path).then().statusCode(200);

    result = given().sessionId(SESSION_ID).accept(ACCEPT_CSV).when().get(path).asString();
    assertTrue(result.contains("green,,,colors"));
  }

  @Test
  public void testGraphqlApi() {
    String path = "/api/graphql";

    // create a new session, separate from the session shared in these tests
    String sessionId =
        given().body("{\"query\":\"{_session{email}}\"}").when().post(path).sessionId();

    String result =
        given()
            .sessionId(sessionId)
            .body("{\"query\":\"{_session{email}}\"}")
            .when()
            .post(path)
            .asString();
    assertTrue(result.contains("anonymous"));

    // if anonymous then should not be able to see users
    result =
        given()
            .sessionId(sessionId)
            .body("{\"query\":\"{_admin{userCount}}\"}")
            .when()
            .post(path)
            .asString();
    assertTrue(result.contains("errors"));

    // read admin password from environment if necessary
    String adminPass =
        (String)
            EnvironmentProperty.getParameter(
                org.molgenis.emx2.Constants.MOLGENIS_ADMIN_PW, ADMIN_PW_DEFAULT, STRING);

    result =
        given()
            .sessionId(sessionId)
            .body(
                "{\"query\":\"mutation{signin(email:\\\""
                    + db.getAdminUserName()
                    + "\\\",password:\\\""
                    + adminPass
                    + "\\\"){message}}\"}")
            .when()
            .post(path)
            .asString();
    assertTrue(result.contains("Signed in"));

    result =
        given()
            .sessionId(sessionId)
            .body("{\"query\":\"{_session{email}}\"}")
            .when()
            .post(path)
            .asString();
    assertTrue(result.contains(db.getAdminUserName()));

    // if admin then should  be able to see users
    result =
        given()
            .sessionId(sessionId)
            .body("{\"query\":\"{_admin{userCount}}\"}")
            .when()
            .post(path)
            .asString();
    assertFalse(result.contains("Error"));

    String schemaPath = "/pet store/api/graphql";
    result =
        given()
            .sessionId(sessionId)
            .body("{\"query\":\"{Pet{name}}\"}")
            .when()
            .post(schemaPath)
            .asString();
    assertTrue(result.contains("spike"));

    result =
        given()
            .sessionId(sessionId)
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
            .sessionId(sessionId)
            .body("{\"query\":\"mutation{signout{message}}\"}")
            .when()
            .post(path)
            .asString();
    assertTrue(result.contains("signed out"));

    // if anonymous then should not be able to see users
    result =
        given()
            .sessionId(sessionId)
            .body("{\"query\":\"{_admin{userCount}}\"}")
            .when()
            .post(path)
            .asString();
    assertTrue(result.contains("errors"));
  }

  @Test
  public void testBootstrapThemeService() {
    // should success
    String css = given().when().get("/pet store/tables/theme.css?primaryColor=123123").asString();
    Assert.assertTrue(css.contains("123123"));

    // should fail
    css = given().when().get("/pet store/tables/theme.css?primaryColor=pink").asString();
    Assert.assertTrue(css.contains("pink"));
  }

  @Test
  public void testMolgenisWebservice_redirectToFirstMenuItem() {
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
  public void testTokenBasedAuth() throws JsonProcessingException {

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
  public void testMolgenisWebservice_robotsDotTxt() {
    when().get("/robots.txt").then().statusCode(200).body(equalTo("User-agent: *\nAllow: /"));
  }

  @Test
  public void testRdfApiRequest() {
    final String urlPrefix = "http://localhost:" + PORT;

    final String defaultContentType = "text/turtle";
    final String jsonldContentType = "application/ld+json";
    final String ttlContentType = "text/turtle";
    final String defaultContentTypeWithCharset = "text/turtle; charset=utf-8"; // charset is ignored

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
  }

  @Test
  void testRdfApiContent() {
    // Output from global API call.
    String resultBase =
        given()
            .sessionId(SESSION_ID)
            .when()
            .get("http://localhost:" + PORT + "/api/rdf?schemas=pet store")
            .getBody()
            .asString();

    // Output from global API call with invalid schema.
    // TODO: https://github.com/molgenis/molgenis-emx2/issues/4954 (fix to return 204)
    String resultBaseNonExisting =
        given()
            .sessionId(SESSION_ID)
            .when()
            .get("http://localhost:" + PORT + "/api/rdf?schemas=thisSchemaTotallyDoesNotExist")
            .getBody()
            .asString();

    // Output shacl sets
    String resultShaclSetsYaml =
        given()
            .sessionId(SESSION_ID)
            .when()
            .get("http://localhost:" + PORT + "/api/rdf?shacls")
            .getBody()
            .asString();

    // Output schema API call.
    String resultSchema =
        given()
            .sessionId(SESSION_ID)
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
        () -> assertEquals(getFileAsString("api/rdf/shacl_sets.yaml"), resultShaclSetsYaml));
  }

  /**
   * Request that does not define a content type but does validate on this.
   *
   * @param expectStatusCode
   * @param contentType
   * @return
   */
  private RequestSender rdfApiRequest(int expectStatusCode, String expectContentType) {
    return given()
        .sessionId(SESSION_ID)
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
   * @param contentType
   * @return
   */
  private RequestSender rdfApiContentTypeRequest(
      int expectStatusCode, String givenContentType, String expectedContentType) {
    return given()
        .sessionId(SESSION_ID)
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
    return given().sessionId(SESSION_ID).expect().statusCode(expectStatusCode).when();
  }

  @Test
  public void downloadCsvTable() {
    Response response = downloadPet("/pet store/api/csv/Pet");
    assertTrue(
        response.getBody().asString().contains("name,category,photoUrls,status,tags,weight"));
    assertTrue(response.getBody().asString().contains("pooky,cat,,available,,9.4"));
  }

  @Test
  public void downloadCsvTableWithSystemColumns() {
    Response response = downloadPet("/pet store/api/csv/Pet?" + INCLUDE_SYSTEM_COLUMNS + "=true");
    assertTrue(response.getBody().asString().contains("mg_"));
  }

  @Test
  public void downloadExcelTable() throws IOException {
    Response response = downloadPet("/pet store/api/excel/Pet");
    List<String> rows = TestUtils.readExcelSheet(response.getBody().asInputStream());
    assertEquals("name,category,photoUrls,status,tags,weight,orders,mg_draft", rows.get(0));
    assertEquals(
        "pooky,cat,,available,,9.4,ORDER:6fe7a528-2e97-48cc-91e6-a94c689b4919,", rows.get(1));
  }

  @Test
  public void downloadExelTableWithSystemColumns() throws IOException {
    Response response = downloadPet("/pet store/api/excel/Pet?" + INCLUDE_SYSTEM_COLUMNS + "=true");
    List<String> rows = TestUtils.readExcelSheet(response.getBody().asInputStream());
    assertTrue(rows.get(0).contains("mg_"));
  }

  @Test
  public void downloadZipTable() throws IOException, InterruptedException {
    File file = TestUtils.responseToFile(downloadPet("/pet store/api/zip/Pet"));
    List<File> files = TestUtils.extractFileFromZip(file);
    String result = Files.readString(files.get(0).toPath());
    assertTrue(result.contains("name,category,photoUrls,status,tags,weight"));
    assertTrue(result.contains("pooky,cat,,available,,9.4"));
  }

  @Test
  public void downloadZipTableWithSystemColumns() throws IOException, InterruptedException {
    File file =
        TestUtils.responseToFile(
            downloadPet("/pet store/api/zip/Pet?" + INCLUDE_SYSTEM_COLUMNS + "=true"));
    List<File> files = TestUtils.extractFileFromZip(file);
    String result = Files.readString(files.get(0).toPath());
    assertTrue(result.contains("mg_"));
  }

  private Response downloadPet(String requestString) {
    return given()
        .sessionId(SESSION_ID)
        .accept(ACCEPT_EXCEL)
        .expect()
        .statusCode(200)
        .when()
        .get(requestString);
  }

  @Test
  void testRoot() {
    given()
        .sessionId(SESSION_ID)
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
  public void testScriptExecution() throws JsonProcessingException, InterruptedException {
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
  public void testScriptScheduling() throws JsonProcessingException, InterruptedException {
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
    result =
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
  public void testExecuteSubtaskInScriptTask()
      throws JsonProcessingException, InterruptedException {
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
            .sessionId(SESSION_ID)
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
            .sessionId(SESSION_ID)
            .when()
            .post("/api/scripts/" + failingJobName)
            .getBody()
            .asString();

    url = new ObjectMapper().readTree(result).at("/url").textValue();
    assertFalse(testJobSuccess(url));
  }

  private static boolean testJobSuccess(String url)
      throws InterruptedException, JsonProcessingException {
    String result = given().sessionId(SESSION_ID).get(url).asString();

    String status = "WAITING";
    int count = 0;
    while (!result.contains("ERROR") && !"COMPLETED".equals(status) && !"ERROR".equals(status)) {
      if (count++ > 30) {
        throw new MolgenisException("failed: polling took too long, result is: " + result);
      }
      Thread.sleep(1000);
      result = given().sessionId(SESSION_ID).get(url).asString();
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
        .sessionId(SESSION_ID)
        .expect()
        .contentType("application/ld+json")
        .statusCode(200)
        .when()
        .get("/pet store/api/jsonld");

    given()
        .sessionId(SESSION_ID)
        .expect()
        .contentType("application/ld+json")
        .statusCode(200)
        .when()
        .get("/pet store/api/jsonld/Pet");
  }

  @Test
  void testTurtleOnTTLEndpoint() {
    given()
        .sessionId(SESSION_ID)
        .expect()
        .contentType("text/turtle")
        .statusCode(200)
        .when()
        .get("/pet store/api/ttl");

    given()
        .sessionId(SESSION_ID)
        .expect()
        .contentType("text/turtle")
        .statusCode(200)
        .when()
        .get("/pet store/api/ttl/Pet");
  }

  @Test
  public void testBeaconApiSmokeTests() {
    String result = given().get("/api/beacon/configuration").getBody().asString();
    assertTrue(result.contains("productionStatus"));

    result = given().get("/api/beacon/map").getBody().asString();
    assertTrue(result.contains("endpointSets"));

    result = given().get("/pet store/api/beacon/info").getBody().asString();
    assertTrue(result.contains("beaconInfoResponse"));

    result = given().get("/api/beacon/filtering_terms").getBody().asString();
    assertTrue(result.contains("filteringTerms"));

    result = given().get("/api/beacon/entry_types").getBody().asString();
    assertTrue(result.contains("entry"));

    result = given().get("/api/beacon/datasets").getBody().asString();
    assertTrue(result.contains("datasets"));

    result = given().get("/api/beacon/g_variants").getBody().asString();
    assertTrue(result.contains("datasets"));

    result = given().get("/api/beacon/analyses").getBody().asString();
    assertTrue(result.contains("datasets"));

    result = given().get("/api/beacon/biosamples").getBody().asString();
    assertTrue(result.contains("datasets"));

    result = given().get("/api/beacon/cohorts").getBody().asString();
    assertTrue(result.contains("datasets"));

    result = given().get("/api/beacon/individuals").getBody().asString();
    assertTrue(result.contains("datasets"));

    result =
        given()
            .body(
                """
                    {
                      "query": {
                      "filters": [
                        {
                        "id": "NCIT:C28421",
                        "value": "GSSO_000123",
                        "operator": "="
                        }
                      ]
                      }
                    }""")
            .post("/api/beacon/individuals")
            .asString();
    assertTrue(result.contains("datasets"));

    result = given().get("/api/beacon/runs").getBody().asString();
    assertTrue(result.contains("datasets"));
  }

  @Test
  void testThatTablesWithSpaceCanBeDownloaded() {
    var table = schema.getTable(TABLE_WITH_SPACES);

    given()
        .sessionId(SESSION_ID)
        .expect()
        .statusCode(200)
        .when()
        .get("/pet store/api/jsonld/" + table.getIdentifier());

    given()
        .sessionId(SESSION_ID)
        .expect()
        .statusCode(200)
        .when()
        .get("/pet store/api/ttl/" + table.getIdentifier());

    given()
        .sessionId(SESSION_ID)
        .expect()
        .statusCode(200)
        .when()
        .get("/pet store/api/excel/" + table.getIdentifier());

    given()
        .sessionId(SESSION_ID)
        .expect()
        .statusCode(200)
        .when()
        .get("/pet store/api/csv/" + table.getIdentifier());
  }

  @Test
  void testProfileApi() {
    String result = result = given().get("/api/profiles").getBody().asString();
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
}
