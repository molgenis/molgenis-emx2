package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.molgenis.emx2.web.Constants.*;
import static org.molgenis.emx2.web.MolgenisSessionManager.MOLGENIS_TOKEN;

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
  private static Database db;

  @BeforeClass
  public static void before() throws IOException {

    // setup test schema
    db = TestDatabaseFactory.getTestDatabase();
    Schema schema = db.dropCreateSchema("pet store");
    PetStoreExample.create(schema.getMetadata());
    PetStoreExample.populate(schema);

    // grant a user permission
    schema.addMember(PET_SHOP_OWNER, Privileges.OWNER.toString());
    db.grantCreateSchema(PET_SHOP_OWNER);
    // start web service for testing
    MolgenisWebservice.start(TestDatabaseFactory.getDataSource(), 8080);

    RestAssured.port = Integer.valueOf(8080);
    RestAssured.baseURI = "http://localhost";
    RestAssured.requestSpecification = given().header(MOLGENIS_TOKEN, "admin");
  }

  @Test
  public void test2SchemaDownloadUploadZip() throws IOException {
    // get original schema
    String schemaCsv = given().accept(ACCEPT_CSV).when().get(DATA_PET_STORE).asString();

    // create a new schema for zip
    db.dropCreateSchema("pet store zip");

    // download zip contents of old schema
    byte[] zipContents = given().accept(ACCEPT_ZIP).when().get("/pet store/api/zip").asByteArray();

    // upload zip contents into new schema
    File zipFile = createTempFile(zipContents, ".zip");
    given().multiPart(zipFile).when().post("/pet store zip/api/zip").then().statusCode(200);

    // check if schema equal using json representation
    String schemaCsv2 = given().accept(ACCEPT_CSV).when().get("/pet store zip/api/csv").asString();
    assertEquals(schemaCsv, schemaCsv2);

    // delete the new schema
    db.dropSchema("pet store zip");
  }

  @Test
  public void testDownloadUploadJsonAndYaml() {
    String schemaJson = given().when().get("/pet store/api/json").asString();

    db.dropCreateSchema("pet store json");

    given().body(schemaJson).when().post("/pet store json/api/json").then().statusCode(200);

    String schemaJson2 = given().when().get("/pet store json/api/json").asString();

    assertEquals(schemaJson, schemaJson2);

    String schemaYaml = given().when().get("/pet store/api/yaml").asString();

    db.dropCreateSchema("pet store yaml");

    given().body(schemaYaml).when().post("/pet store yaml/api/yaml").then().statusCode(200);

    String schemaYaml2 = given().when().get("/pet store yaml/api/yaml").asString();

    assertEquals(schemaYaml, schemaYaml2);
  }

  @Test
  public void test3SchemaDownloadUploadExcel() throws IOException, InterruptedException {

    // download json schema
    String schemaCSV = given().accept(ACCEPT_CSV).when().get("/pet store/api/csv").asString();

    // create a new schema for excel
    db.dropCreateSchema("pet store excel");

    // download excel contents from schema
    byte[] excelContents =
        given().accept(ACCEPT_EXCEL).when().get("/pet store/api/excel").asByteArray();
    File excelFile = createTempFile(excelContents, ".xlsx");

    // upload excel into new schema
    String message =
        given()
            .multiPart(excelFile)
            .when()
            .post("/pet store excel/api/excel?async=true")
            .asString();

    Map<String, String> val = new ObjectMapper().readValue(message, Map.class);
    String url = val.get("url");
    String id = val.get("id");

    // check if in tasks list
    assertTrue(
        given().multiPart(excelFile).when().get("/pet store/api/tasks").asString().contains(id));

    // poll task until complete
    String poll = given().when().get(url).asString();
    int count = 0;
    while (poll.contains("RUNNING")) {
      if (count++ > 100) {
        throw new MolgenisException("failed: polling took too long");
      }
      poll = given().when().get(url).asString();
      Thread.sleep(500);
    }

    // check if schema equal using json representation
    String schemaCSV2 =
        given().accept(ACCEPT_CSV).when().get("/pet store excel/api/csv").asString();

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
    String result = given().accept(ACCEPT_CSV).when().get(path).asString();
    assertEquals(exp1, result);

    String update = "name\r\nyellow\r\n";
    given().body(update).when().post(path).then().statusCode(200);

    String exp2 = "name\r\nred\r\ngreen\r\nyellow\r\n";
    result = given().accept(ACCEPT_CSV).when().get(path).asString();
    assertEquals(exp2, result);

    given().body(update).when().delete(path).then().statusCode(200);

    result = given().accept(ACCEPT_CSV).when().get(path).asString();
    assertEquals(exp1, result);
  }

  @Test
  public void test6SmokeTestGraphql() {
    db.setUserPassword("admin", "admin");
    String path = "/api/graphql";
    String result =
        given()
            .body(
                "{\"query\":\"mutation{signin(email:\\\"admin\\\",password:\\\"admin\\\"){message}}\"}")
            .when()
            .post(path)
            .asString();
    assertTrue(result.contains("Signed in"));

    String schemaPath = "/pet store/api/graphql";
    result = given().body("{\"query\":\"{Pet{name}}\"}").when().post(schemaPath).asString();
    assertTrue(result.contains("spike"));

    result =
        given().body("{\"query\":\"mutation{signout{message}}\"}").when().post(path).asString();
    assertTrue(result.contains("signed out"));

    // login again to make sure other tests work
    result =
        given()
            .body(
                "{\"query\":\"mutation{signin(email:\\\"admin\\\",password:\\\"admin\\\"){message}}\"}")
            .when()
            .post(path)
            .asString();
    assertTrue(result.contains("Signed in"));
  }

  @Test
  public void appProxySmokeTest() throws IOException {
    String result = given().when().get("/plugin/molgenis-app-reports/dist/index.html").asString();

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

  @AfterClass
  public static void after() {
    MolgenisWebservice.stop();
  }
}
