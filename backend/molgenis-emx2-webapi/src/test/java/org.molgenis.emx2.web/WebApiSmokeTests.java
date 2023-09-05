package org.molgenis.emx2.web;

import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.Constants.MOLGENIS_HTTP_PORT;
import static org.molgenis.emx2.Constants.SYSTEM_SCHEMA;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.RunMolgenisEmx2.CATALOGUE_DEMO;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN_PW_DEFAULT;
import static org.molgenis.emx2.sql.SqlDatabase.ANONYMOUS;
import static org.molgenis.emx2.web.Constants.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.Assert;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.Order;
import org.molgenis.emx2.datamodels.PetStoreLoader;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInZipFile;
import org.molgenis.emx2.io.tablestore.TableStoreForXlsxFile;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.utils.EnvironmentProperty;

/* this is a smoke test for the integration of web api with the database layer. So not complete coverage of all services but only a few essential requests to pass most endpoints */
@TestMethodOrder(MethodOrderer.MethodName.class)
@Tag("slow")
public class WebApiSmokeTests {

  public static final String DATA_PET_STORE = "/pet store/api/csv";
  public static final String PET_SHOP_OWNER = "pet_shop_owner";
  public static final String SYSTEM_PREFIX = "/" + SYSTEM_SCHEMA;
  public static String SESSION_ID; // to toss around a session for the tests
  private static Database db;
  private static Schema schema;
  final String CSV_TEST_SCHEMA = "pet store csv";
  static final int PORT = 8081; // other then default so we can see effect

  @BeforeAll
  public static void before() throws Exception {
    // FIXME: beforeAll fails under windows
    // setup test schema
    db = TestDatabaseFactory.getTestDatabase();

    // start web service for testing, including env variables
    withEnvironmentVariable(MOLGENIS_HTTP_PORT, "" + PORT)
        // disable because of parallism issues .and(MOLGENIS_INCLUDE_CATALOGUE_DEMO, "true")
        .execute(() -> RunMolgenisEmx2.main(new String[] {}));

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

    // should be created
    schema = db.getSchema("pet store");
    // grant a user permission
    schema.addMember(PET_SHOP_OWNER, Privileges.OWNER.toString());
    schema.addMember(ANONYMOUS, Privileges.VIEWER.toString());
    db.grantCreateSchema(PET_SHOP_OWNER);
  }

  @AfterAll
  public static void after() {
    MolgenisWebservice.stop();
    db.dropSchemaIfExists(CATALOGUE_DEMO);
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
    assertEquals(schemaCsv, schemaCsv2);

    // delete the new schema
    db.dropSchema("pet store zip");
  }

  @Test
  public void testReports() throws IOException {
    // create a new schema for report
    Schema schema = db.dropCreateSchema("pet store reports");
    new PetStoreLoader().load(schema, true);

    // check if reports work
    byte[] zipContents =
        getContentAsByteArray(ACCEPT_ZIP, "/pet store reports/api/reports/zip?id=0");
    File zipFile = createTempFile(zipContents, ".zip");
    TableStore store = new TableStoreForCsvInZipFile(zipFile.toPath());
    store.containsTable("pet report");

    // check if reports work with parameters
    zipContents =
        getContentAsByteArray(
            ACCEPT_ZIP, "/pet store reports/api/reports/zip?id=1&name=spike,pooky");
    zipFile = createTempFile(zipContents, ".zip");
    store = new TableStoreForCsvInZipFile(zipFile.toPath());
    store.containsTable("pet report with parameters");

    // check if reports work
    byte[] excelContents =
        getContentAsByteArray(ACCEPT_ZIP, "/pet store reports/api/reports/excel?id=0");
    File excelFile = createTempFile(excelContents, ".xlsx");
    store = new TableStoreForXlsxFile(excelFile.toPath());
    assertTrue(store.containsTable("pet report"));

    // check if reports work with parameters
    excelContents =
        getContentAsByteArray(
            ACCEPT_ZIP, "/pet store reports/api/reports/excel?id=1&name=spike,pooky");
    excelFile = createTempFile(excelContents, ".xlsx");
    store = new TableStoreForXlsxFile(excelFile.toPath());
    assertTrue(store.containsTable("pet report with parameters"));
    assertTrue(excelContents.length > 0);
  }

  @Test
  public void testCsvApi_csvTableMetadataUpdate() throws IOException {

    // fresh schema for testing
    db.dropCreateSchema(CSV_TEST_SCHEMA);

    // full table header present in exported table metadata
    String header =
        "tableName,tableExtends,tableType,columnName,columnType,key,required,refSchema,refTable,refLink,refBack,refLabel,validation,visible,computed,semantics,label,description\r\n";

    // add new table with description and semantics as metadata
    addUpdateTableAndCompare(
        header,
        "tableName,description,semantics\r\nTestMetaTable,TestDesc,TestSem",
        "TestMetaTable,,,,,,,,,,,,,,,TestSem,,TestDesc\r\n");

    // update table without new description or semantics, values should be untouched
    addUpdateTableAndCompare(
        header, "tableName\r\nTestMetaTable", "TestMetaTable,,,,,,,,,,,,,,,TestSem,,TestDesc\r\n");

    // update only description, semantics should be untouched
    addUpdateTableAndCompare(
        header,
        "tableName,description\r\nTestMetaTable,NewTestDesc",
        "TestMetaTable,,,,,,,,,,,,,,,TestSem,,NewTestDesc\r\n");

    // make semantics empty by not supplying a value, description  should be untouched
    addUpdateTableAndCompare(
        header,
        "tableName,semantics\r\nTestMetaTable,",
        "TestMetaTable,,,,,,,,,,,,,,,,,NewTestDesc\r\n");

    // make description empty while also adding a new value for semantics
    addUpdateTableAndCompare(
        header,
        "tableName,description,semantics\r\nTestMetaTable,,NewTestSem",
        "TestMetaTable,,,,,,,,,,,,,,,NewTestSem,,\r\n");

    // empty both description and semantics
    addUpdateTableAndCompare(
        header,
        "tableName,description,semantics\r\nTestMetaTable,,",
        "TestMetaTable,,,,,,,,,,,,,,,,,\r\n");

    // add description value, and string array value for semantics
    addUpdateTableAndCompare(
        header,
        "tableName,description,semantics\r\nTestMetaTable,TestDesc,\"TestSem1,TestSem2\"",
        "TestMetaTable,,,,,,,,,,,,,,,\"TestSem1,TestSem2\",,TestDesc\r\n");
  }

  /**
   * Helper function to prevent code duplication
   *
   * @param header
   * @param tableMeta
   * @param expected
   * @throws IOException
   */
  private void addUpdateTableAndCompare(String header, String tableMeta, String expected)
      throws IOException {
    byte[] addUpdateTable = tableMeta.getBytes(StandardCharsets.UTF_8);
    File addUpdateTableFile = createTempFile(addUpdateTable, ".csv");
    acceptFileUpload(addUpdateTableFile, "molgenis");
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

    // create tmp files for csv metadata and data
    File contentsMetaFile = createTempFile(contentsMeta, ".csv");
    File contentsCategoryDataFile = createTempFile(contentsCategoryData, ".csv");
    File contentsOrderDataFile = createTempFile(contentsOrderData, ".csv");
    File contentsPetDataFile = createTempFile(contentsPetData, ".csv");
    File contentsUserDataFile = createTempFile(contentsUserData, ".csv");
    File contentsTagDataFile = createTempFile(contentsTagData, ".csv");

    // upload csv metadata and data into the new schema
    // here we use 'body' (instead of 'multiPart' in e.g. testCsvApi_zipUploadDownload) because csv,
    // json and yaml import is submitted in the request body
    acceptFileUpload(contentsMetaFile, "molgenis");
    acceptFileUpload(contentsCategoryDataFile, "Category");
    acceptFileUpload(contentsTagDataFile, "Tag");
    acceptFileUpload(contentsPetDataFile, "Pet");
    acceptFileUpload(contentsOrderDataFile, "Order");
    acceptFileUpload(contentsUserDataFile, "User");

    // download csv from the new schema
    String contentsMetaNew = getContentAsString("/api/csv");
    String contentsCategoryDataNew = getContentAsString("/api/csv/Category");
    String contentsOrderDataNew = getContentAsString("/api/csv/Order");
    String contentsPetDataNew = getContentAsString("/api/csv/Pet");
    String contentsUserDataNew = getContentAsString("/api/csv/User");
    String contentsTagDataNew = getContentAsString("/api/csv/Tag");

    // test if existing and new schema are equal
    assertEquals(new String(contentsMeta), contentsMetaNew);
    assertEquals(new String(contentsCategoryData), contentsCategoryDataNew);
    assertEquals(new String(contentsOrderData), contentsOrderDataNew);
    assertEquals(new String(contentsPetData), contentsPetDataNew);
    assertEquals(new String(contentsUserData), contentsUserDataNew);
    assertEquals(new String(contentsTagData), contentsTagDataNew);
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

  private void acceptFileUpload(File content, String table) {
    given()
        .sessionId(SESSION_ID)
        .body(content)
        .header("fileName", table)
        .when()
        .post("/" + CSV_TEST_SCHEMA + "/api/csv")
        .then()
        .statusCode(200);
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
  @Disabled("gives many false positive errors")
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

    assertEquals(schemaJson, schemaJson2.replace("pet store json", "pet store"));

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

    assertEquals(schemaYaml, schemaYaml2.replace("pet store yaml", "pet store"));
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

    assertFalse(poll.body().asString().contains("FAILED"));

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
    assertTrue(result.contains("green,,colors"));

    String update = "name,parent\r\nyellow,colors\r\n";
    given().sessionId(SESSION_ID).body(update).when().post(path).then().statusCode(200);

    result = given().sessionId(SESSION_ID).accept(ACCEPT_CSV).when().get(path).asString();
    assertTrue(result.contains("yellow"));

    given().sessionId(SESSION_ID).body(update).when().delete(path).then().statusCode(200);

    result = given().sessionId(SESSION_ID).accept(ACCEPT_CSV).when().get(path).asString();
    assertTrue(result.contains("green,,colors"));
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
  public void testMolgenisWebservice_redirectWhenSlash() {
    given()
        .sessionId(SESSION_ID)
        .redirects()
        .follow(false)
        .expect()
        .statusCode(302)
        .header("Location", is("http://localhost:" + PORT + "/pet store/"))
        .when()
        .get("/pet store");

    given()
        .sessionId(SESSION_ID)
        .redirects()
        .follow(false)
        .expect()
        .statusCode(302)
        .header("Location", is("http://localhost:" + PORT + "/pet store/tables/"))
        .when()
        .get("/pet store/tables");
  }

  @Test
  public void testMolgenisWebservice_redirectToFirstMenuItem() {
    given()
        .redirects()
        .follow(false)
        .expect()
        .statusCode(302)
        .header("Location", is("http://localhost:" + PORT + "/pet store/tables"))
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
        .header("Location", is("http://localhost:" + PORT + "/pet store/blaat2"))
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
        .header("Location", is("http://localhost:" + PORT + "/pet store/blaat"))
        .when()
        .get("/pet store/");

    schema.getMetadata().removeSetting("menu");
    db.becomeAdmin();
  }

  @Test
  public void testTokenBasedAuth() throws JsonProcessingException {

    // check if we can use temporary token
    String result =
        given()
            .body(
                "{\"query\":\"mutation{signin(email:\\\"shopmanager\\\",password:\\\"shopmanager\\\"){message,token}}\"}")
            .when()
            .post("/api/graphql")
            .getBody()
            .asString();
    String token = new ObjectMapper().readTree(result).at("/data/signin/token").textValue();

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
  public void testRdfApi() {
    // skip 'all schemas' test because data is way to big (i.e.
    // get("http://localhost:PORT/api/rdf");)
    given()
        .sessionId(SESSION_ID)
        .expect()
        .statusCode(200)
        .when()
        .get("http://localhost:" + PORT + "/pet store/api/rdf");
    given()
        .sessionId(SESSION_ID)
        .expect()
        .statusCode(200)
        .when()
        .get("http://localhost:" + PORT + "/pet store/api/rdf/Category");
    given()
        .sessionId(SESSION_ID)
        .expect()
        .statusCode(200)
        .when()
        .get("http://localhost:" + PORT + "/pet store/api/rdf/Category/column/name");
    given()
        .sessionId(SESSION_ID)
        .expect()
        .statusCode(200)
        .when()
        .get("http://localhost:" + PORT + "/pet store/api/rdf/Category/cat");
    given()
        .sessionId(SESSION_ID)
        .expect()
        .statusCode(400)
        .when()
        .get("http://localhost:" + PORT + "/pet store/api/rdf/doesnotexist");
  }

  @Test
  public void testFDPDistribution() {
    given()
        .sessionId(SESSION_ID)
        .expect()
        .statusCode(400)
        .when()
        .get("http://localhost:" + PORT + "/api/fdp/distribution/pet store/Category/ttl");
  }

  @Test
  public void testGraphGenome400() {
    given()
        .sessionId(SESSION_ID)
        .expect()
        .statusCode(400)
        .when()
        .get("http://localhost:" + PORT + "/api/graphgenome");
  }

  @Test
  public void downloadCsvTable() {
    Response response = downloadPet("/pet store/api/csv/Pet");
    assertTrue(
        response.getBody().asString().contains("name,category,photoUrls,status,tags,weight"));
    assertTrue(response.getBody().asString().contains("pooky,cat,,available,,9.4"));
    assertFalse(response.getBody().asString().contains("mg_"));
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
    assertEquals("name,category,photoUrls,status,tags,weight", rows.get(0));
    assertEquals("pooky,cat,,available,,9.4", rows.get(1));
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
  @Disabled("unstable")
  public void testScriptExecution() throws JsonProcessingException, InterruptedException {
    // get token for admin
    String result =
        given()
            .body(
                "{\"query\":\"mutation{signin(email:\\\"admin\\\",password:\\\"admin\\\"){message,token}}\"}")
            .when()
            .post("/api/graphql")
            .getBody()
            .asString();
    String token = new ObjectMapper().readTree(result).at("/data/signin/token").textValue();

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

    // get token for admin
    String result =
        given()
            .body(
                "{\"query\":\"mutation{signin(email:\\\"admin\\\",password:\\\"admin\\\"){message,token}}\"}")
            .when()
            .post("/api/graphql")
            .getBody()
            .asString();
    String token = new ObjectMapper().readTree(result).at("/data/signin/token").textValue();

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
                .size()
            == 0,
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
  public void testRedirectOnJSONLDEndpoint() {
    given()
        .sessionId(SESSION_ID)
        .redirects()
        .follow(false)
        .expect()
        .statusCode(302)
        .header("Location", is("/pet store/api/rdf?format=jsonld"))
        .when()
        .get("/pet store/api/jsonld");

    given()
        .sessionId(SESSION_ID)
        .redirects()
        .follow(false)
        .expect()
        .statusCode(302)
        .header("Location", is("/pet store/api/rdf/Pet?format=jsonld"))
        .when()
        .get("/pet store/api/jsonld/Pet");
  }

  @Test
  public void testRedirectOnTTLEndpoint() {
    given()
        .sessionId(SESSION_ID)
        .redirects()
        .follow(false)
        .expect()
        .statusCode(302)
        .header("Location", is("/pet store/api/rdf?format=ttl"))
        .when()
        .get("/pet store/api/ttl");

    given()
        .sessionId(SESSION_ID)
        .redirects()
        .follow(false)
        .expect()
        .statusCode(302)
        .header("Location", is("/pet store/api/rdf/Pet?format=ttl"))
        .when()
        .get("/pet store/api/ttl/Pet");
  }

  @Test
  @Disabled("unstable")
  public void testBeaconApiSmokeTests() {
    // todo: ideally we would here validate the responses against json schemas, are those schemas
    // easily available?
    // todo: can we put in some relevant filter parameters so we really test if they are functional?

    String result = given().get("/api/beacon").getBody().asString();
    assertTrue(result.contains("info"));

    result = given().get("/api/beacon/configuration").getBody().asString();
    assertTrue(result.contains("productionStatus"));

    result = given().get("/api/beacon/map").getBody().asString();
    assertTrue(result.contains("endpointSets"));

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

    result = given().get("/api/beacon/runs").getBody().asString();
    assertTrue(result.contains("datasets"));
  }

  @Test
  public void testFairDataPointSmoke() {
    // todo: enable fdp somehow? I suppose we would need a publid fair data hub for this?

    // String result = given().get("/api/fdp").getBody().asString();
    // assertTrue(result.contains("endpointSets"));

    //    result = given().get("/api/fdp/catalogue/pet store/Pet").getBody().asString();
    //    assertTrue(result.contains("todo"));
    //
    //    result = given().get("/api/fdp/dataset/pet store/Pet").getBody().asString();
    //    assertTrue(result.contains("todo"));
    //
    //    result = given().get("/api/fdp/distribution/pet store/json/json").getBody().asString();
    //    assertTrue(result.contains("todo"));
    //
    //    result = given().get("/api/fdp/profile").getBody().asString();
    //    assertTrue(result.contains("todo"));
    //
    //    result = given().get("/api/fdp/catalogue/profile").getBody().asString();
    //    assertTrue(result.contains("todo"));
    //
    //    result = given().get("/api/fdp/dataset/profile").getBody().asString();
    //    assertTrue(result.contains("todo"));
    //
    //    result = given().get("/api/fdp/distribution/profile").getBody().asString();
    //    assertTrue(result.contains("todo"));
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
}
