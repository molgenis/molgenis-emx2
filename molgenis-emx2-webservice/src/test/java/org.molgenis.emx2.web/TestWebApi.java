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
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.*;
import static org.molgenis.emx2.web.Constants.*;
import static org.molgenis.emx2.web.MolgenisSessionManager.MOLGENIS_TOKEN;

/* this is a smoke test for the integration of web api with the database layer */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestWebApi {

  public static final String DATA_PET_STORE = "/api/json/pet store";
  public static final String PET_SHOP_OWNER = "pet_shop_owner";

  @BeforeClass
  public static void before() throws SQLException, IOException {

    // create data source
    HikariDataSource dataSource = new HikariDataSource();
    String url = "jdbc:postgresql:molgenis";
    dataSource.setJdbcUrl(url);
    dataSource.setUsername("molgenis");
    dataSource.setPassword("molgenis");

    // setup test schema
    Database db = TestDatabaseFactory.getTestDatabase(dataSource);
    Schema schema = db.createSchema("pet store");
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
  public void test1Membership() {
    String path = "/api/members/pet store";
    List members = given().accept(ACCEPT_JSON).when().get(path).as(List.class);
    assertEquals(2, members.size());

    String bofke = "[{\"user\":\"bofke\",\"role\":\"Editor\"}]";

    // add bofke
    String result =
        given()
            .contentType(ACCEPT_JSON)
            .body("[{\"user\":\"bofke\",\"role\":\"Editor\"}]")
            .when()
            .post(path)
            .asString();

    members = given().accept(ACCEPT_JSON).when().get(path).as(List.class);
    assertEquals(3, members.size());

    // update bofke membership
    result =
        given()
            .contentType(ACCEPT_JSON)
            .body("[{\"user\":\"bofke\",\"role\":\"Viewer\"}]")
            .when()
            .post("/members/pet store")
            .asString();

    members = given().accept(ACCEPT_JSON).when().get(path).as(List.class);

    // update bofke to nonexisting role should give error
    Map error =
        given()
            .contentType(ACCEPT_JSON)
            .body("[{\"user\":\"bofke\",\"role\":\"FAKEROLE\"}]")
            .when()
            .post(path)
            .as(Map.class);
    assertTrue(error.get("errors").toString().contains("doesn't exist"));
    // make MolgenisException an unchecked exception?

    // remove unknown user
    error =
        given()
            .contentType(ACCEPT_JSON)
            .body("[{\"user\":\"pofke\",\"role\":\"FAKEROLE\"}]")
            .when()
            .post(path)
            .as(Map.class);
    assertTrue(error.get("errors").toString().contains("doesn't exist"));

    // remove bofke from membership
    given()
        .contentType(ACCEPT_JSON)
        .body("[{\"user\":\"bofke\",\"role\":\"Viewer\"}]")
        .when()
        .delete(path)
        .asString();
    members = given().accept(ACCEPT_JSON).when().get(path).as(List.class);
    assertEquals(2, members.size());
  }

  @Test
  public void test2SchemaDownloadUploadZip() throws IOException {
    // get original schema
    String schemaJson = given().accept(ACCEPT_JSON).when().get(DATA_PET_STORE).asString();

    // create a new schema for zip
    given()
        .contentType(ACCEPT_JSON)
        .body("{\"name\":\"pet store zip\"}")
        .when()
        .post("/api/json")
        .then()
        .statusCode(200);

    // download zip contents of old schema
    byte[] zipContents = given().accept(ACCEPT_ZIP).when().get("/api/zip/pet store").asByteArray();

    // upload zip contents into new schema
    File zipFile = createTempFile(zipContents, ".zip");
    given().multiPart(zipFile).when().post("/api/zip/pet store zip").then().statusCode(200);

    // check if schema equal using json representation
    String schemaJson2 =
        given().accept(ACCEPT_JSON).when().get("/api/json/pet store zip").asString();
    assertEquals(schemaJson, schemaJson2);

    // delete the new schema
    given().accept(ACCEPT_JSON).when().delete("/api/json/pet store zip").then().statusCode(200);

    // check deleted
    String contents = given().contentType(ACCEPT_JSON).when().get("/api/json").asString();
    assertFalse(contents.contains("zip"));
  }

  @Test
  public void test3SchemaDownloadUploadExcel() throws IOException {

    // download json schema
    String schemaJson = given().accept(ACCEPT_JSON).when().get("/api/json/pet store").asString();

    // create a new schema for excel
    given()
        .contentType(ACCEPT_JSON)
        .body("{\"name\":\"pet store excel\"}")
        .when()
        .post("/api/json")
        .then()
        .statusCode(200);

    // download excel contents from schema
    byte[] excelContents =
        given().accept(ACCEPT_EXCEL).when().get("/api/excel/pet store").asByteArray();
    File excelFile = createTempFile(excelContents, ".xlsx");

    // upload excel into new schema
    given().multiPart(excelFile).when().post("/api/excel/pet store excel").then().statusCode(200);

    // check if schema equal using json representation
    String schemaJson3 =
        given().accept(ACCEPT_JSON).when().get("/api/json/pet store excel").asString();
    // todo cant compare because random ordering assertEquals(schemaJson, schemaJson3);

    // delete a new schema for excel
    given().accept(ACCEPT_JSON).when().delete("/api/json/pet store excel").then().statusCode(200);

    // check deleted
    String contents = given().contentType(ACCEPT_JSON).when().get("/api/json").asString();
    assertFalse(contents.contains("excel"));
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
  public void test4TableGetPostDeleteJSON() {

    String path = "/api/json/pet store/Category";

    List result = given().accept(ACCEPT_JSON).when().get(path).as(List.class);
    assertEquals(2, result.size());

    String body = "[{\"name\":\"cCategory\"}]";

    given().contentType(ACCEPT_JSON).body(body).when().post(path).then().statusCode(200);

    result = given().accept(ACCEPT_JSON).when().get(path).as(List.class);
    assertEquals(3, result.size());

    given().contentType(ACCEPT_JSON).body(body).when().delete(path).then().statusCode(200);

    result = given().accept(ACCEPT_JSON).when().get(path).as(List.class);
    assertEquals(2, result.size());
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
    String result = given().when().get("/apps/molgenis-app-reports/dist/index.html").asString();

    // some unique text content
    assertTrue(result.contains("molgenis-catalogue-templates"));
  }

  @AfterClass
  public static void after() {
    MolgenisWebservice.stop();
  }
}
