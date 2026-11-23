package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

class ZipApiTest extends ApiTestBase {

  private static final String NON_EXISTING_SCHEMA = "nonexisting";

  @Test
  void generateReportsToStoreForUnknownSchema_isRejected() {
    // the zip endpoint opens the output stream before generating the reports, so the error
    // response can only be checked via the excel endpoint, which also uses generateReportsToStore
    Response response =
        given().when().get("/" + NON_EXISTING_SCHEMA + "/api/reports/excel?id=report1");

    assertEquals(400, response.getStatusCode());
    assertEquals(
        """
        {
          "errors" : [
            {
              "message" : "Schema nonexisting unknown"
            }
          ]
        }""",
        response.getBody().asString());
  }
}
