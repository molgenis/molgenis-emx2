package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.molgenis.emx2.web.Constants.*;
import static org.molgenis.emx2.web.MolgenisSessionManager.MOLGENIS_TOKEN;
import static org.molgenis.emx2.web.MolgenisWebservice.sessionManager;

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

/* this is a smoke test for the integration of web api with the database layer */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestWebApi {

  public static final String DATA_PET_STORE = "/pet store/api/csv";
  public static final String PET_SHOP_OWNER = "pet_shop_owner";
  public static final String ADMIN_SESSION_ID = "admin";
  private static Database db;
  private static Schema schema;

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

    // simulate token until persistent token system is officially implemented
    MolgenisSession session = sessionManager.createSession("admin");
    session.getDatabase().setActiveUser(ADMIN_SESSION_ID);
    session = sessionManager.createSession("shopviewer");
    session.getDatabase().setActiveUser("shopviewer");
    session = sessionManager.createSession("shopmanager");
    session.getDatabase().setActiveUser("shopmanager");

    RestAssured.port = Integer.valueOf(8080);
    RestAssured.baseURI = "http://localhost";
  }

  @Test
  public void test2SchemaDownloadUploadZip() throws IOException {
    // get original schema
    String schemaCsv =
        given()
            .header(MOLGENIS_TOKEN, ADMIN_SESSION_ID)
            .accept(ACCEPT_CSV)
            .when()
            .get(DATA_PET_STORE)
            .asString();

    // create a new schema for zip
    db.dropCreateSchema("pet store zip");

    // download zip contents of old schema
    byte[] zipContents =
        given()
            .header(MOLGENIS_TOKEN, ADMIN_SESSION_ID)
            .accept(ACCEPT_ZIP)
            .when()
            .get("/pet store/api/zip")
            .asByteArray();

    // upload zip contents into new schema
    File zipFile = createTempFile(zipContents, ".zip");
    given()
        .header(MOLGENIS_TOKEN, ADMIN_SESSION_ID)
        .multiPart(zipFile)
        .when()
        .post("/pet store zip/api/zip")
        .then()
        .statusCode(200);

    // check if schema equal using json representation
    String schemaCsv2 =
        given()
            .header(MOLGENIS_TOKEN, ADMIN_SESSION_ID)
            .accept(ACCEPT_CSV)
            .when()
            .get("/pet store zip/api/csv")
            .asString();
    assertEquals(schemaCsv, schemaCsv2);

    // delete the new schema
    db.dropSchema("pet store zip");
  }

  @Test
  public void testDownloadUploadJsonAndYaml() {
    String schemaJson =
        given()
            .header(MOLGENIS_TOKEN, ADMIN_SESSION_ID)
            .when()
            .get("/pet store/api/json")
            .asString();

    db.dropCreateSchema("pet store json");

    given()
        .header(MOLGENIS_TOKEN, ADMIN_SESSION_ID)
        .body(schemaJson)
        .when()
        .post("/pet store json/api/json")
        .then()
        .statusCode(200);

    String schemaJson2 =
        given()
            .header(MOLGENIS_TOKEN, ADMIN_SESSION_ID)
            .when()
            .get("/pet store json/api/json")
            .asString();

    assertEquals(schemaJson, schemaJson2);

    String schemaYaml =
        given()
            .header(MOLGENIS_TOKEN, ADMIN_SESSION_ID)
            .when()
            .get("/pet store/api/yaml")
            .asString();

    db.dropCreateSchema("pet store yaml");

    given()
        .header(MOLGENIS_TOKEN, ADMIN_SESSION_ID)
        .body(schemaYaml)
        .when()
        .post("/pet store yaml/api/yaml")
        .then()
        .statusCode(200);

    String schemaYaml2 =
        given()
            .header(MOLGENIS_TOKEN, ADMIN_SESSION_ID)
            .when()
            .get("/pet store yaml/api/yaml")
            .asString();

    assertEquals(schemaYaml, schemaYaml2);
  }

  @Test
  public void test3SchemaDownloadUploadExcel() throws IOException, InterruptedException {

    // download json schema
    String schemaCSV =
        given()
            .header(MOLGENIS_TOKEN, ADMIN_SESSION_ID)
            .accept(ACCEPT_CSV)
            .when()
            .get("/pet store/api/csv")
            .asString();

    // create a new schema for excel
    db.dropCreateSchema("pet store excel");

    // download excel contents from schema
    byte[] excelContents =
        given()
            .header(MOLGENIS_TOKEN, ADMIN_SESSION_ID)
            .accept(ACCEPT_EXCEL)
            .when()
            .get("/pet store/api/excel")
            .asByteArray();
    File excelFile = createTempFile(excelContents, ".xlsx");

    // upload excel into new schema
    String message =
        given()
            .header(MOLGENIS_TOKEN, ADMIN_SESSION_ID)
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
            .header(MOLGENIS_TOKEN, ADMIN_SESSION_ID)
            .multiPart(excelFile)
            .when()
            .get("/pet store/api/tasks")
            .asString()
            .contains(id));

    // poll task until complete
    String poll = given().header(MOLGENIS_TOKEN, ADMIN_SESSION_ID).when().get(url).asString();
    int count = 0;
    while (poll.contains("RUNNING")) {
      if (count++ > 100) {
        throw new MolgenisException("failed: polling took too long");
      }
      poll = given().header(MOLGENIS_TOKEN, ADMIN_SESSION_ID).when().get(url).asString();
      Thread.sleep(500);
    }

    // check if schema equal using json representation
    String schemaCSV2 =
        given()
            .header(MOLGENIS_TOKEN, ADMIN_SESSION_ID)
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
  public void test5TableGetPostDeleteCSV() {

    String path = "/pet store/api/csv/Tag";

    String exp1 = "name\r\nred\r\ngreen\r\n";
    String result =
        given()
            .header(MOLGENIS_TOKEN, ADMIN_SESSION_ID)
            .accept(ACCEPT_CSV)
            .when()
            .get(path)
            .asString();
    assertEquals(exp1, result);

    String update = "name\r\nyellow\r\n";
    given()
        .header(MOLGENIS_TOKEN, ADMIN_SESSION_ID)
        .body(update)
        .when()
        .post(path)
        .then()
        .statusCode(200);

    String exp2 = "name\r\nred\r\ngreen\r\nyellow\r\n";
    result =
        given()
            .header(MOLGENIS_TOKEN, ADMIN_SESSION_ID)
            .accept(ACCEPT_CSV)
            .when()
            .get(path)
            .asString();
    assertEquals(exp2, result);

    given()
        .header(MOLGENIS_TOKEN, ADMIN_SESSION_ID)
        .body(update)
        .when()
        .delete(path)
        .then()
        .statusCode(200);

    result =
        given()
            .header(MOLGENIS_TOKEN, ADMIN_SESSION_ID)
            .accept(ACCEPT_CSV)
            .when()
            .get(path)
            .asString();
    assertEquals(exp1, result);
  }

  @Test
  public void test6SmokeTestGraphqlWithSession() {
    db.setUserPassword("admin", "admin");
    String path = "/api/graphql";

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

    result =
        given()
            .sessionId(sessionId)
            .body(
                "{\"query\":\"mutation{signin(email:\\\"admin\\\",password:\\\"admin\\\"){message}}\"}")
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
    assertTrue(result.contains("admin"));

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
  }

  @Test
  public void appProxySmokeTest() throws IOException {
    String result =
        given()
            .header(MOLGENIS_TOKEN, ADMIN_SESSION_ID)
            .when()
            .get("/plugin/molgenis-app-reports/dist/index.html")
            .asString();

    // some unique text content
    assertTrue(result.contains("molgenis-catalogue-templates"));
  }

  @Test
  public void testThemeGenerator() {
    // should success
    String css = given().when().get("/pet store/tables/theme.css?primary=123123").asString();
    Assert.assertTrue(css.contains("123123"));

    // should fail
    css = given().when().get("/pet store/tables/theme.css?primary=pink").asString();
    Assert.assertTrue(css.contains("pink"));
  }

  @Test
  public void redirectWhenSlash() {
    given()
        .header(MOLGENIS_TOKEN, ADMIN_SESSION_ID)
        .redirects()
        .follow(false)
        .expect()
        .statusCode(302)
        .header("Location", is("http://localhost:8080/pet store/"))
        .when()
        .get("/pet store");

    given()
        .header(MOLGENIS_TOKEN, ADMIN_SESSION_ID)
        .redirects()
        .follow(false)
        .expect()
        .statusCode(302)
        .header("Location", is("http://localhost:8080/pet store/tables/"))
        .when()
        .get("/pet store/tables");
  }

  @Test
  public void redirectToFirstMenuItem() {
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

    given()
        .redirects()
        .follow(false)
        .header(MOLGENIS_TOKEN, "shopviewer")
        .expect()
        .statusCode(302)
        .header("Location", is("http://localhost:8080/pet store/blaat2"))
        .when()
        .get("/pet store/");

    given()
        .header(MOLGENIS_TOKEN, "shopmanager")
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
