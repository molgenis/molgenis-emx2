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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.web.Constants.*;

/* this is a smoke test for the integration of web api with the database layer */
public class TestWebApi {

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
  public void testSchemaDownloadUploadRoundtrip() throws IOException {

    String schema_path = "/data/pet store";
    // download json schema
    String schemaJson = given().accept(ACCEPT_JSON).when().get(schema_path).asString();

    // download zip file
    // Path tmpFile = Files.createTempFile("some",".zip");

    // todo download excel file

    // ZIP TEST
    // create a new schema for zip
    given()
        .contentType(ACCEPT_JSON)
        .body("{\"name\":\"pet store zip\"}")
        .when()
        .post("/data")
        .then()
        .statusCode(200);
    String schema_zip_path = "/data/pet store zip";
    // download zip contents of old schema
    byte[] zipContents = given().accept(ACCEPT_ZIP).when().get(schema_path).asByteArray();
    // upload zip contents into new schema
    File zipFile = createTempFile(zipContents, ".zip");
    given().multiPart(zipFile).when().post(schema_zip_path).then().statusCode(200);
    // check if schema equal using json representation
    String schemaJson2 = given().accept(ACCEPT_JSON).when().get(schema_zip_path).asString();
    assertEquals(schemaJson, schemaJson2);

    // EXCEL TEST
    // create a new schema for excel
    given()
        .contentType(ACCEPT_JSON)
        .body("{\"name\":\"pet store excel\"}")
        .when()
        .post("/data")
        .then()
        .statusCode(200);
    // download excel contents from schema
    byte[] excelContents = given().accept(ACCEPT_EXCEL).when().get(schema_path).asByteArray();
    File excelFile = createTempFile(excelContents, ".xlsx");
    // upload excel into new schema
    given().multiPart(excelFile).when().post("/data/pet store excel").then().statusCode(200);
    // check if schema equal using json representation
    String schemaJson3 = given().accept(ACCEPT_JSON).when().get("/data/pet store excel").asString();
    assertEquals(schemaJson, schemaJson3);

    // download and upload csv file between schema's
    String csvContents = given().accept(ACCEPT_CSV).when().get(schema_path).asString();
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
