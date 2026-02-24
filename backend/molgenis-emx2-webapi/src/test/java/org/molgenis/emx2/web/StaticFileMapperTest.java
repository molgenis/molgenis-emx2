package org.molgenis.emx2.web;

import graphql.Assert;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.util.Objects;
import org.junit.jupiter.api.Test;

public class StaticFileMapperTest extends ApiTestBase {

  @Test
  void testCanFetchFileFromPublicHtmlFolder() {
    Response restResponse = RestAssured.get("apps/test-app/test-assets/styling.css");

    String mimeType = restResponse.getHeader("Content-type");
    Assert.assertTrue(Objects.equals(mimeType, "text/css"));

    int status = restResponse.statusCode();
    Assert.assertTrue(status == 200);
  }

  @Test
  void testCanFetchIndexByDefaultFromPublicHtmlFolder() {
    Response restResponse = RestAssured.get("/schema/test-app/");

    String mimeType = restResponse.getHeader("Content-type");
    Assert.assertTrue(Objects.equals(mimeType, "text/html"));

    int status = restResponse.statusCode();
    Assert.assertTrue(status == 200);
  }

  @Test
  void testDefaultRedirectToCentralApps() {
    Response restResponse = RestAssured.given().redirects().follow(false).when().get("/");

    int status = restResponse.getStatusCode();
    Assert.assertTrue(status == 302);

    String locationHeader = restResponse.getHeader("Location");
    Assert.assertTrue(Objects.equals(locationHeader, "/apps/central/"));
  }
}
