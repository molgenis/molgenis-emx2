package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.TableMetadata.table;

import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Schema;

class ModelApiTest extends ApiTestBase {

  private static final String VERSION_SCHEMA = "ModelApiTestVersion";
  private static final String ROUNDTRIP_SCHEMA = "ModelApiTestRoundtrip";
  private static final String VALIDATOR_SCHEMA = "ModelApiTestValidator";

  @BeforeAll
  static void setup() {
    login("admin", "admin");
  }

  private static Schema createPersonSchema(String schemaName) {
    Schema schema = database.dropCreateSchema(schemaName);
    schema.create(table("Person", column("id").setKey(1), column("name", STRING)));
    return schema;
  }

  private static String modelPath(String schemaName) {
    return "/" + schemaName + "/api/model";
  }

  private static String getModel(String schemaName) {
    return given().sessionId(sessionId).when().get(modelPath(schemaName)).asString();
  }

  private static Response putModel(String schemaName, String body, String query) {
    return given().sessionId(sessionId).body(body).when().put(modelPath(schemaName) + query);
  }

  private static String withVersion(String schemaName, String version) {
    String model = getModel(schemaName).replaceFirst("version: .*\n", "");
    return model.replaceFirst("formatVersion: 1\n", "formatVersion: 1\nversion: " + version + "\n");
  }

  @Test
  void versionLifecycle() {
    createPersonSchema(VERSION_SCHEMA);

    // initial export carries no version
    assertFalse(getModel(VERSION_SCHEMA).contains("version:"));

    // apply stamps the version on the schema, readable via GET
    putModel(VERSION_SCHEMA, withVersion(VERSION_SCHEMA, "1.0.0"), "").then().statusCode(200);
    assertTrue(getModel(VERSION_SCHEMA).contains("version: 1.0.0"));

    // dry-run of a newer version does NOT stamp it
    putModel(VERSION_SCHEMA, withVersion(VERSION_SCHEMA, "2.0.0"), "?dryRun=true")
        .then()
        .statusCode(200);
    assertTrue(getModel(VERSION_SCHEMA).contains("version: 1.0.0"));

    // applying an older version is refused without force, leaving the stored version untouched
    putModel(VERSION_SCHEMA, withVersion(VERSION_SCHEMA, "0.9.0"), "").then().statusCode(400);
    assertTrue(getModel(VERSION_SCHEMA).contains("version: 1.0.0"));

    // force applies the downgrade
    putModel(VERSION_SCHEMA, withVersion(VERSION_SCHEMA, "0.9.0"), "?force=true")
        .then()
        .statusCode(200);
    assertTrue(getModel(VERSION_SCHEMA).contains("version: 0.9.0"));
  }

  @Test
  void getDryRunApplyRoundTrip() {
    createPersonSchema(ROUNDTRIP_SCHEMA);

    // the live model exports without the email column
    assertFalse(getModel(ROUNDTRIP_SCHEMA).contains("email"));

    String desired =
        """
        formatVersion: 1
        version: 2.0.0
        tables:
        - name: Person
          columns:
          - name: id
            key: 1
          - name: name
          - name: email
        """;

    // dry-run reports the added column but changes nothing
    Response dryRun = putModel(ROUNDTRIP_SCHEMA, desired, "?dryRun=true");
    dryRun.then().statusCode(200);
    assertTrue(dryRun.body().asString().contains("email"));
    assertFalse(getModel(ROUNDTRIP_SCHEMA).contains("email"));

    // apply persists the change and stamps the new version
    putModel(ROUNDTRIP_SCHEMA, desired, "").then().statusCode(200);
    String applied = getModel(ROUNDTRIP_SCHEMA);
    assertTrue(applied.contains("email"));
    assertTrue(applied.contains("version: 2.0.0"));
  }

  @Test
  void putValidatorErrorCarriesDocumentPathAndPosition() {
    createPersonSchema(VALIDATOR_SCHEMA);

    String invalid =
        """
        formatVersion: 1
        tables:
        - name: Person
          columns:
          - name: id
            bogusKey: nope
        """;

    Response response = putModel(VALIDATOR_SCHEMA, invalid, "");
    response.then().statusCode(400);
    String body = response.body().asString();
    assertTrue(body.contains("molgenis.yaml"));
    assertTrue(body.contains("line"));
    assertTrue(body.contains("column"));
    assertEquals(400, response.getStatusCode());
  }
}
