package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN_PW_DEFAULT;
import static org.molgenis.emx2.web.Constants.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.Assert;
import io.restassured.RestAssured;
import java.io.*;
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
import org.molgenis.emx2.examples.PetStoreExample;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.utils.EnvironmentProperty;

/* this is a smoke test for the integration of web api with the database layer. So not complete coverage of all services but only a few essential requests to pass most endpoints */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WebApiSmokeTests {

  public static final String DATA_PET_STORE = "/pet store/api/csv";
  public static final String PET_SHOP_OWNER = "pet_shop_owner";
  private static Database db;
  private static Schema schema;
  public static String SESSION_ID; // to toss around a session for the tests

  @BeforeClass
  public static void before() throws IOException {

    // setup test schema
    db = TestDatabaseFactory.getTestDatabase();
    schema = db.dropCreateSchema("pet store");
    PetStoreExample.create(schema.getMetadata());
    PetStoreExample.populate(schema);

    // grant a user permission
    schema.addMember(PET_SHOP_OWNER, Privileges.OWNER.toString());
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

  @Test
  public void testCsvApi_zipUploadDownload() throws IOException {
    // get original schema
    String schemaCsv =
        given().sessionId(SESSION_ID).accept(ACCEPT_CSV).when().get(DATA_PET_STORE).asString();

    // create a new schema for zip
    db.dropCreateSchema("pet store zip");

    // download zip contents of old schema
    byte[] zipContents =
        given()
            .sessionId(SESSION_ID)
            .accept(ACCEPT_ZIP)
            .when()
            .get("/pet store/api/zip")
            .asByteArray();

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
    byte[] excelContents =
        given()
            .sessionId(SESSION_ID)
            .accept(ACCEPT_EXCEL)
            .when()
            .get("/pet store/api/excel")
            .asByteArray();
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

    // check if in tasks list
    assertTrue(
        given()
            .sessionId(SESSION_ID)
            .multiPart(excelFile)
            .when()
            .get("/pet store/api/tasks")
            .asString()
            .contains(id));

    // poll task until complete
    String poll = given().sessionId(SESSION_ID).when().get(url).asString();
    int count = 0;
    while (poll.contains("RUNNING")) {
      if (count++ > 100) {
        throw new MolgenisException("failed: polling took too long");
      }
      poll = given().sessionId(SESSION_ID).when().get(url).asString();
      Thread.sleep(500);
    }

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
    assertTrue(result.contains("green,colors"));

    String update = "name,parent\r\nyellow,colors\r\n";
    given().sessionId(SESSION_ID).body(update).when().post(path).then().statusCode(200);

    result = given().sessionId(SESSION_ID).accept(ACCEPT_CSV).when().get(path).asString();
    assertTrue(result.contains("yellow"));

    given().sessionId(SESSION_ID).body(update).when().delete(path).then().statusCode(200);

    result = given().sessionId(SESSION_ID).accept(ACCEPT_CSV).when().get(path).asString();
    assertTrue(result.contains("green,colors"));
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
    String css = given().when().get("/pet store/tables/theme.css?primary=123123").asString();
    Assert.assertTrue(css.contains("123123"));

    // should fail
    css = given().when().get("/pet store/tables/theme.css?primary=pink").asString();
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
    db.clearActiveUser();
  }

  @AfterClass
  public static void after() {
    MolgenisWebservice.stop();
  }
}
