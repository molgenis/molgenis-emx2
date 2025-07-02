package org.molgenis.emx2.web;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.restassured.RestAssured;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

    List<Integer> responseTimes = new ArrayList<>();

    long t0 = new Date().getTime();

    int numRequests = 1000;

    for (int i = 1; i <= numRequests; i++) {
      long t1 = new Date().getTime();
      String resp = doDemoRequest();
      long t2 = new Date().getTime();
      assertTrue(resp.contains("Contacts"), "Response does not contain expected data: " + resp);
      logger.info("Response time: " + (t2 - t1) + " ms, ms, request: " + i);
      responseTimes.add((int) (t2 - t1));
    }

    long mean = responseTimes.stream().mapToInt(Integer::intValue).sum() / responseTimes.size();
    logger.info("Mean response time: " + mean + " ms for " + numRequests + " requests");

    long max = responseTimes.stream().mapToInt(Integer::intValue).max().orElse(0);

    logger.info("Max response time: " + max + " ms for " + numRequests + " requests");

    long variance =
        responseTimes.stream()
                .mapToInt(Integer::intValue)
                .map(i -> i - (int) mean)
                .map(i -> i * i)
                .sum()
            / responseTimes.size();

    long stdDev = (long) Math.sqrt(variance);
    logger.info(
        "Standard deviation of response times: "
            + stdDev
            + " ms for  "
            + numRequests
            + "  requests");

    assertTrue(
        mean < 3000,
        "Request took too long: " + mean + " ms (sd: " + stdDev + "ms, max: " + max + "ms)");
  }

  private String doDemoRequest() {
    return RestAssured.given()
        .body("{\"query\":\"{Contacts{lastName}}\"}")
        .post("/" + SCHEMA_NAME + "/graphql")
        .asString();
  }
}
