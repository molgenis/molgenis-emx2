package org.molgenis.emx2.web;

import io.restassured.RestAssured;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.examples.PetStoreExample;
import org.molgenis.emx2.sql.DatabaseFactory;
import org.molgenis.emx2.utils.MolgenisException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.molgenis.emx2.web.Constants.*;

/* this is a smoke test for the integration of web api with the database layer */
public class TestWebApi {

  public static final String DATA_PET_STORE_EXCEL = "/data/pet store excel";
  public static final String DATA_PET_STORE_ZIP = "/data/pet store zip";
  public static final String DATA_PET_STORE = "/data/pet store";

  @BeforeClass
  public static void before() throws MolgenisException {
    Database db = DatabaseFactory.getTestDatabase("molgenis", "molgenis");

    Schema schema = db.createSchema("pet store");
    PetStoreExample.create(schema.getMetadata());
    PetStoreExample.populate(schema);

    MolgenisWebservice.start(db);

    RestAssured.port = Integer.valueOf(8080);
    RestAssured.baseURI = "http://localhost";
  }

  @Test
  public void testMembership() {
    List members = given().accept(ACCEPT_JSON).when().get("/members/pet store").as(List.class);
    assertEquals(0, members.size());

    String bofke = "[{\"user\":\"bofke\",\"role\":\"Editor\"}]";

    // add bofke
    String result =
        given()
            .contentType(ACCEPT_JSON)
            .body("[{\"user\":\"bofke\",\"role\":\"Editor\"}]")
            .when()
            .post("/members/pet store")
            .asString();

    members = given().accept(ACCEPT_JSON).when().get("/members/pet store").as(List.class);
    assertEquals(1, members.size());

    // update bofke membership
    result =
        given()
            .contentType(ACCEPT_JSON)
            .body("[{\"user\":\"bofke\",\"role\":\"Viewer\"}]")
            .when()
            .post("/members/pet store")
            .asString();
    members = given().accept(ACCEPT_JSON).when().get("/members/pet store").as(List.class);
    assertEquals("Viewer", ((Map) members.get(0)).get("role"));

    // update bofke to nonexisting role should give error
    Map error =
        given()
            .contentType(ACCEPT_JSON)
            .body("[{\"user\":\"bofke\",\"role\":\"FAKEROLE\"}]")
            .when()
            .post("/members/pet store")
            .as(Map.class);
    assertEquals("add_members_failed", error.get("type"));
    // make MolgenisException an unchecked exception?

    // remove unknown user
    error =
        given()
            .contentType(ACCEPT_JSON)
            .body("[{\"user\":\"pofke\",\"role\":\"FAKEROLE\"}]")
            .when()
            .post("/members/pet store")
            .as(Map.class);
    assertEquals("add_members_failed", error.get("type"));

    // remove bofke from membership
    given()
        .contentType(ACCEPT_JSON)
        .body("[{\"user\":\"bofke\",\"role\":\"Viewer\"}]")
        .when()
        .delete("/members/pet store")
        .asString();
    members = given().accept(ACCEPT_JSON).when().get("/members/pet store").as(List.class);
    assertEquals(0, members.size());
  }

  @Test
  public void testSchemaDownloadUploadZip() throws IOException {
    // get original schema
    String schemaJson = given().accept(ACCEPT_JSON).when().get(DATA_PET_STORE).asString();

    // create a new schema for zip
    given()
        .contentType(ACCEPT_JSON)
        .body("{\"name\":\"pet store zip\"}")
        .when()
        .post("/data")
        .then()
        .statusCode(200);

    // download zip contents of old schema
    byte[] zipContents = given().accept(ACCEPT_ZIP).when().get(DATA_PET_STORE).asByteArray();

    // upload zip contents into new schema
    File zipFile = createTempFile(zipContents, ".zip");
    given().multiPart(zipFile).when().post(DATA_PET_STORE_ZIP).then().statusCode(200);

    // check if schema equal using json representation
    String schemaJson2 = given().accept(ACCEPT_JSON).when().get(DATA_PET_STORE_ZIP).asString();
    assertEquals(schemaJson, schemaJson2);

    // delete a new schema for excel
    given().accept(ACCEPT_JSON).when().delete(DATA_PET_STORE_ZIP).then().statusCode(200);

    // check deleted
    String contents = given().contentType(ACCEPT_JSON).when().get("/data").asString();
    assertFalse(contents.contains("zip"));
  }

  @Test
  public void testSchemaDownloadUploadExcel() throws IOException {

    // download json schema
    String schemaJson = given().accept(ACCEPT_JSON).when().get(DATA_PET_STORE).asString();

    // create a new schema for excel
    given()
        .contentType(ACCEPT_JSON)
        .body("{\"name\":\"pet store excel\"}")
        .when()
        .post("/data")
        .then()
        .statusCode(200);

    // download excel contents from schema
    byte[] excelContents = given().accept(ACCEPT_EXCEL).when().get(DATA_PET_STORE).asByteArray();
    File excelFile = createTempFile(excelContents, ".xlsx");

    // upload excel into new schema
    given().multiPart(excelFile).when().post(DATA_PET_STORE_EXCEL).then().statusCode(200);

    // check if schema equal using json representation
    String schemaJson3 = given().accept(ACCEPT_JSON).when().get(DATA_PET_STORE_EXCEL).asString();
    assertEquals(schemaJson, schemaJson3);

    // delete a new schema for excel
    given().accept(ACCEPT_JSON).when().delete(DATA_PET_STORE_EXCEL).then().statusCode(200);

    // check deleted
    String contents = given().contentType(ACCEPT_JSON).when().get("/data").asString();
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
  public void testTableGetPostDeleteJSON() {

    String path = "/data/pet store/Category";

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
  public void testTableGetPostDeleteCSV() {

    String path = "/data/pet store/Tag";

    String exp1 = "name\r\naTag\r\nbTag\r\n";
    String result = given().accept(ACCEPT_CSV).when().get(path).asString();
    assertEquals(exp1, result);

    String update = "name\r\ncTag\r\n";
    given().contentType(ACCEPT_CSV).body(update).when().post(path).then().statusCode(200);

    String exp2 = "name\r\naTag\r\nbTag\r\ncTag\r\n";
    result = given().accept(ACCEPT_CSV).when().get(path).asString();
    assertEquals(exp2, result);

    given().contentType(ACCEPT_CSV).body(update).when().delete(path).then().statusCode(200);

    result = given().accept(ACCEPT_CSV).when().get(path).asString();
    assertEquals(exp1, result);
  }

  @AfterClass
  public static void after() {
    MolgenisWebservice.stop();
  }
}
