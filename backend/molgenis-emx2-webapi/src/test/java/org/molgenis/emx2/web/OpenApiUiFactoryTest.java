package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

class OpenApiUiFactoryTest extends ApiTestBase {

  private static final String NON_EXISTING_SCHEMA = "nonexisting";

  @Test
  void getOpenApiUserInterfaceForUnknownSchema_isRejected() {
    Response response = given().when().get("/" + NON_EXISTING_SCHEMA + "/api/openapi");

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
