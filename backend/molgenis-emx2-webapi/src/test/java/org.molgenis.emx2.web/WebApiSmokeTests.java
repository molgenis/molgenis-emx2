package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN_PW_DEFAULT;
import static org.molgenis.emx2.sql.SqlDatabase.ANONYMOUS;
import static org.molgenis.emx2.web.Constants.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.Assert;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.PetStoreLoader;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.utils.EnvironmentProperty;

/* this is a smoke test for the integration of web api with the database layer. So not complete coverage of all services but only a few essential requests to pass most endpoints */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WebApiSmokeTests {

  public static final String DATA_PET_STORE = "/pet store/api/csv";
  public static final String PET_SHOP_OWNER = "pet_shop_owner";
  public static String SESSION_ID; // to toss around a session for the tests
  private static Database db;
  private static Schema schema;
  final String CSV_TEST_SCHEMA = "pet store csv";

  @BeforeClass
  public static void before() throws IOException {

    // setup test schema
    db = TestDatabaseFactory.getTestDatabase();
    schema = db.dropCreateSchema("pet store");
    new PetStoreLoader().load(schema, true);

    // grant a user permission
    schema.addMember(PET_SHOP_OWNER, Privileges.OWNER.toString());
    schema.addMember(ANONYMOUS, Privileges.VIEWER.toString());
    db.grantCreateSchema(PET_SHOP_OWNER);

    // start web service for testing
    MolgenisWebservice.start(8080);

    // set default rest assured settings
    RestAssured.port = Integer.valueOf(8080);
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
  }

  @AfterClass
  public static void after() {
    MolgenisWebservice.stop();
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
  public void testCsvApi_csvTableMetadataUpdate() throws IOException {

    // fresh schema for testing
    db.dropCreateSchema(CSV_TEST_SCHEMA);

    // full table header present in exported table metadata
    String header =
        "tableName,tableExtends,tableType,columnName,columnType,key,required,refSchema,refTable,refLink,refBack,validation,semantics,description\r\n";

    // add new table with description and semantics as metadata
    addUpdateTableAndCompare(
        header,
        "tableName,description,semantics\r\nTestMetaTable,TestDesc,TestSem",
        "TestMetaTable,,,,,,,,,,,,TestSem,TestDesc\r\n");

    // update table without new description or semantics, values should be untouched
    addUpdateTableAndCompare(
        header, "tableName\r\nTestMetaTable", "TestMetaTable,,,,,,,,,,,,TestSem,TestDesc\r\n");

    // update only description, semantics should be untouched
    addUpdateTableAndCompare(
        header,
        "tableName,description\r\nTestMetaTable,NewTestDesc",
        "TestMetaTable,,,,,,,,,,,,TestSem,NewTestDesc\r\n");

    // make semantics empty by not supplying a value, description  should be untouched
    addUpdateTableAndCompare(
        header,
        "tableName,semantics\r\nTestMetaTable,",
        "TestMetaTable,,,,,,,,,,,,,NewTestDesc\r\n");

    // make description empty while also adding a new value for semantics
    addUpdateTableAndCompare(
        header,
        "tableName,description,semantics\r\nTestMetaTable,,NewTestSem",
        "TestMetaTable,,,,,,,,,,,,NewTestSem,\r\n");

    // empty both description and semantics
    addUpdateTableAndCompare(
        header,
        "tableName,description,semantics\r\nTestMetaTable,,",
        "TestMetaTable,,,,,,,,,,,,,\r\n");

    // add description value, and string array value for semantics
    addUpdateTableAndCompare(
        header,
        "tableName,description,semantics\r\nTestMetaTable,TestDesc,\"TestSem1,TestSem2\"",
        "TestMetaTable,,,,,,,,,,,,\"TestSem1,TestSem2\",TestDesc\r\n");
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

    assertEquals(schemaJson, schemaJson2);

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

    assertEquals(schemaYaml, schemaYaml2);
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
    assertTrue(result.contains("Error"));

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
    assertTrue(result.contains("Error"));
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
        .header("Location", is("http://localhost:8080/pet store/"))
        .when()
        .get("/pet store");

    given()
        .sessionId(SESSION_ID)
        .redirects()
        .follow(false)
        .expect()
        .statusCode(302)
        .header("Location", is("http://localhost:8080/pet store/tables/"))
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
        .header("Location", is("http://localhost:8080/pet store/tables"))
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
        .header("Location", is("http://localhost:8080/pet store/blaat2"))
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
        .header("Location", is("http://localhost:8080/pet store/blaat"))
        .when()
        .get("/pet store/");

    schema.getMetadata().removeSetting("menu");
    db.becomeAdmin();
  }

  @Test
  public void testMolgenisWebservice_robotsDotTxt() {
    when().get("/robots.txt").then().statusCode(200).body(equalTo("User-agent: *\nAllow: /"));
  }

  @Test
  public void testRdfApi() {
    // skip 'all schemas' test because data is way to big (i.e.
    // get("http://localhost:8080/api/rdf");)
    given()
        .sessionId(SESSION_ID)
        .expect()
        .statusCode(200)
        .when()
        .get("http://localhost:8080/pet store/api/rdf");
    given()
        .sessionId(SESSION_ID)
        .expect()
        .statusCode(200)
        .when()
        .get("http://localhost:8080/pet store/api/rdf/Category");
    given()
        .sessionId(SESSION_ID)
        .expect()
        .statusCode(200)
        .when()
        .get("http://localhost:8080/pet store/api/rdf/Category/cat");
    given()
        .sessionId(SESSION_ID)
        .expect()
        .statusCode(400)
        .when()
        .get("http://localhost:8080/pet store/api/rdf/doesnotexist");
  }

  @Test
  public void testLinkedDataApi() {
    given()
        .sessionId(SESSION_ID)
        .expect()
        .statusCode(200)
        .when()
        .get("http://localhost:8080/pet store/api/jsonld");
    given()
        .sessionId(SESSION_ID)
        .expect()
        .statusCode(200)
        .when()
        .get("http://localhost:8080/pet store/api/ttl");
    given()
        .sessionId(SESSION_ID)
        .expect()
        .statusCode(200)
        .when()
        .get("http://localhost:8080/pet store/api/jsonld/Category");
    given()
        .sessionId(SESSION_ID)
        .expect()
        .statusCode(200)
        .when()
        .get("http://localhost:8080/pet store/api/ttl/Category");
    given()
        .sessionId(SESSION_ID)
        .expect()
        .statusCode(400)
        .when()
        .get("http://localhost:8080/pet store/api/ttl/doesnotexist");
  }

  @Test
  public void testFDPDistribution() {
    given()
        .sessionId(SESSION_ID)
        .expect()
        .statusCode(400)
        .when()
        .get("http://localhost:8080/api/fdp/distribution/pet store/Category/ttl");
  }
}
