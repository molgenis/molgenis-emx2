package org.molgenis.emx2.web;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.restassured.RestAssured;
import java.util.Date;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerformanceTest {

  static final Logger logger = LoggerFactory.getLogger(PerformanceTest.class);
  private static final String BASE_URL =
      "http://localhost:8080"; // "https://umcgresearchdatacatalogue-acc.molgeniscloud.org";
  private static final String SCHEMA_NAME = "catalogue-demo"; // "UMCG";

  @Disabled("Disabled for CI, run manually to test performance")
  @Test
  void testPerformance() {
    RestAssured.baseURI = BASE_URL;

    long t0 = new Date().getTime();

    for (int i = 1; i <= 10000; i++) {
      long t1 = new Date().getTime();
      String resp = doDemoRequest();
      long t2 = new Date().getTime();
      assertTrue(resp.contains("Contacts"), "Response does not contain expected data: " + resp);
      logger.info(
          "Response time: " + (t2 - t1) + " ms, duration : " + (t2 - t0) + " ms, request: " + i);
      assertTrue(t2 - t1 < 1000, "Request took too long: " + (t2 - t1) + " ms for request " + i);
    }

    long tn = new Date().getTime();
  }

  private String doDemoRequest() {
    return RestAssured.given()
        .body("{\"query\":\"{Contacts{lastName}}\"}")
        .post("/" + SCHEMA_NAME + "/graphql")
        .asString();
  }
}
