package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN_PW_DEFAULT;
import static org.molgenis.emx2.sql.SqlDatabase.ANONYMOUS;

import io.restassured.RestAssured;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.RunMolgenisEmx2;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.DataModels;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.utils.EnvironmentProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Tag("slow")
public class PerformanceTest {

  static final Logger logger = LoggerFactory.getLogger(PerformanceTest.class);
  private static final String BASE_URL =
      "http://localhost"; // "https://umcgresearchdatacatalogue-acc.molgeniscloud.org";
  private static final String SCHEMA_NAME = "performancetest"; // "catalogue-demo"; // "UMCG";
  private static final int PORT = 8081; // other than default so we can see effect
  private static Database database;

  @BeforeAll
  public static void before() throws Exception {

    database = TestDatabaseFactory.getTestDatabase();
    RunMolgenisEmx2.main(new String[] {String.valueOf(PORT)});

    // set default rest assured settings
    RestAssured.port = PORT;
    RestAssured.baseURI = "http://localhost";

    // create an admin session to work with
    String adminPass =
        (String)
            EnvironmentProperty.getParameter(
                org.molgenis.emx2.Constants.MOLGENIS_ADMIN_PW, ADMIN_PW_DEFAULT, STRING);
    String adminSessionId =
        given()
            .body(
                "{\"query\":\"mutation{signin(email:\\\""
                    + database.getAdminUserName()
                    + "\\\",password:\\\""
                    + adminPass
                    + "\\\"){message}}\"}")
            .when()
            .post("api/graphql")
            .sessionId();

    Schema schema = database.dropCreateSchema(SCHEMA_NAME);
    DataModels.Profile.DATA_CATALOGUE.getImportTask(schema, true).run();

    schema.addMember(ANONYMOUS, Privileges.VIEWER.toString());
  }

  @AfterAll
  public static void after() {
    // Always clean up database to avoid instability due to side effects.
    database.dropSchemaIfExists(SCHEMA_NAME);
  }

  //  @Disabled("Disabled for CI, run manually to test performance")
  @Test
  void testPerformance() {
    RestAssured.baseURI = BASE_URL + ":" + PORT;

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
