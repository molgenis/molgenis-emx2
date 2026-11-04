package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

class CafeVariomeApiTest extends ApiTestBase {

  private static final String NON_EXISTING_SCHEMA = "nonexisting";

  @Test
  void postRecordForUnknownSchema_isRejected() {
    Response response =
        given().body("{}").when().post("/" + NON_EXISTING_SCHEMA + "/api/cafevariome/record");

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
