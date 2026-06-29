package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("slow")
class BeaconApiTest extends ApiTestBase {

  @BeforeAll
  static void setup() {
    login("admin", "admin");
  }

  @Test
  void testBeaconInfoUnknownSchemaReturnsError() {
    given().expect().statusCode(400).when().get("/unknownSchemaThatDoesNotExist/api/beacon/info");
  }

  @Test
  void testBeaconEntryTypeUnknownSchemaReturnsError() {
    given()
        .expect()
        .statusCode(400)
        .when()
        .get("/unknownSchemaThatDoesNotExist/api/beacon/individuals");
  }

  @Test
  void testBeaconEntryTypeGlobalRequestReturnsOk() {
    given().expect().statusCode(200).when().get("/api/beacon/individuals");
  }

  @Test
  void testBeaconConfiguration() {
    getAndAssertContains("/api/beacon/configuration", "productionStatus");
  }

  @Test
  void testBeaconMap() {
    getAndAssertContains("/api/beacon/map", "endpointSets");
  }

  @Test
  void testBeaconEntryTypes() {
    getAndAssertContains("/api/beacon/entry_types", "entry");
  }

  private void getAndAssertContains(String path, String expectedSubstring) {
    database.clearCache();
    String result = given().get(path).getBody().asString();
    ObjectMapper mapper = new ObjectMapper();
    String prettyJson;
    try {
      Object json = mapper.readValue(result, Object.class);
      ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
      prettyJson = writer.writeValueAsString(json);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    assertTrue(
        result.contains(expectedSubstring),
        "expecting:\n" + expectedSubstring + "\nin:\n" + prettyJson);
  }
}
