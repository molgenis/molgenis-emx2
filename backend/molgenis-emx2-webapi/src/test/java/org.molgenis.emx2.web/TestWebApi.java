package org.molgenis.emx2.web;

import com.zaxxer.hikari.HikariDataSource;
import io.restassured.RestAssured;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.DefaultRoles;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.examples.PetStoreExample;
import org.molgenis.emx2.sql.TestDatabaseFactory;

import java.io.*;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.molgenis.emx2.web.Constants.*;
import static org.molgenis.emx2.web.MolgenisSessionManager.MOLGENIS_TOKEN;

/* this is a smoke test for the integration of web api with the database layer */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestWebApi {

  public static final String DATA_PET_STORE = "/api/csv/pet store";
  public static final String PET_SHOP_OWNER = "pet_shop_owner";
  private static Database db;

  @BeforeClass
  public static void before() throws IOException {

    // create data source
    HikariDataSource dataSource = new HikariDataSource();
    String url = "jdbc:postgresql:molgenis";
    dataSource.setJdbcUrl(url);
    dataSource.setUsername("molgenis");
    dataSource.setPassword("molgenis");

    // setup test schema
    db = TestDatabaseFactory.getTestDatabase(dataSource, false);
    Schema schema = db.dropCreateSchema("pet store");
    PetStoreExample.create(schema.getMetadata());
    PetStoreExample.populate(schema);

    // grant a user permission
    schema.addMember(PET_SHOP_OWNER, DefaultRoles.OWNER.toString());
    db.grantCreateSchema(PET_SHOP_OWNER);
    // start web service for testing
    MolgenisWebservice.start(dataSource);

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
    byte[] zipContents = given().accept(ACCEPT_ZIP).when().get("/api/zip/pet store").asByteArray();

    // upload zip contents into new schema
    File zipFile = createTempFile(zipContents, ".zip");
    given().multiPart(zipFile).when().post("/api/zip/pet store zip").then().statusCode(200);

    // check if schema equal using json representation
    String schemaCsv2 = given().accept(ACCEPT_CSV).when().get("/api/csv/pet store zip").asString();
    assertEquals(schemaCsv, schemaCsv2);

    // delete the new schema
    db.dropSchema("pet store zip");
  }

  @Test
  public void test3SchemaDownloadUploadExcel() throws IOException {

    // download json schema
    String schemaCSV = given().accept(ACCEPT_CSV).when().get("/api/csv/pet store").asString();

    // create a new schema for excel
    db.dropCreateSchema("pet store excel");

    // download excel contents from schema
    byte[] excelContents =
        given().accept(ACCEPT_EXCEL).when().get("/api/excel/pet store").asByteArray();
    File excelFile = createTempFile(excelContents, ".xlsx");

    // upload excel into new schema
    given().multiPart(excelFile).when().post("/api/excel/pet store excel").then().statusCode(200);

    // check if schema equal using json representation
    String schemaCSV2 =
        given().accept(ACCEPT_CSV).when().get("/api/csv/pet store excel").asString();

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

    String path = "/api/csv/pet store/Tag";

    String exp1 = "name\r\nred\r\ngreen\r\n";
    String result = given().accept(ACCEPT_CSV).when().get(path).asString();
    assertEquals(exp1, result);

    String update = "name\r\nyellow\r\n";
    given()
        .multiPart("file", "test.txt", new ByteArrayInputStream(update.getBytes()))
        .when()
        .patch(path)
        .then()
        .statusCode(200);

    String exp2 = "name\r\nred\r\ngreen\r\nyellow\r\n";
    result = given().accept(ACCEPT_CSV).when().get(path).asString();
    assertEquals(exp2, result);

    given()
        .multiPart("file", "test.txt", new ByteArrayInputStream(update.getBytes()))
        .when()
        .delete(path)
        .then()
        .statusCode(200);

    result = given().accept(ACCEPT_CSV).when().get(path).asString();
    assertEquals(exp1, result);
  }

  @Test
  public void test6SmokeTestGraphql() {
    String path = "/api/graphql";
    String result =
        given()
            .body(
                "{\"query\":\"mutation{signin(email:\\\"admin\\\",password:\\\"admin\\\"){message}}\"}")
            .when()
            .post(path)
            .asString();
    assertTrue(result.contains("Signed in"));

    String schemaPath = "/api/graphql/pet store";
    result = given().body("{\"query\":\"{Pet{data{name}}}\"}").when().post(schemaPath).asString();
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

  @AfterClass
  public static void after() {
    MolgenisWebservice.stop();
  }
}
