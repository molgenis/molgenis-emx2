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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.*;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.RunMolgenisEmx2;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.DataModels;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.utils.EnvironmentProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Disabled("Disabled for CI, run manually to test performance")
@Tag("slow")
public class PerformanceTest {

  static final Logger logger = LoggerFactory.getLogger(PerformanceTest.class);
  /// private static final String BASE_URL = "https://emx2.dev.molgenis.org/";
  private static final String BASE_URL = "http://localhost";

  private static final String SCHEMA_NAME = "catalogue-demo";
  //  private static final int PORT = 80;
  private static final int PORT = 8080;
  private static Database database;
  private static final int NUMBER_OF_REQUESTS = 100000;

  private static final int THREAD_COUNT = 10;

  @BeforeAll
  public static void before() throws Exception {

    if (BASE_URL.startsWith("http://localhot")) {
      database = TestDatabaseFactory.getTestDatabase();
      RunMolgenisEmx2.main(new String[] {String.valueOf(PORT)});
    }

    //    RestAssured.port = PORT;
    RestAssured.baseURI = BASE_URL;
    RestAssured.port = PORT;

    // createDemoData();
  }

  @AfterAll
  public static void after() {
    if (database != null) {
      database.dropSchemaIfExists(SCHEMA_NAME);
    }
  }

  @Test
  @Disabled("Disabled for CI, run manually to test performance")
  void testPerformanceSequential() {

    List<Integer> responseTimes = new ArrayList<>();

    for (int i = 1; i <= NUMBER_OF_REQUESTS; i++) {
      long t1 = new Date().getTime();
      String resp = doDemoRequest();
      long t2 = new Date().getTime();
      assertTrue(resp.contains("Contacts"), "Response does not contain expected data: " + resp);
      logger.info("Response time: {} ms, ms, request: {}", t2 - t1, i);
      responseTimes.add((int) (t2 - t1));
    }

    long mean = responseTimes.stream().mapToInt(Integer::intValue).sum() / responseTimes.size();
    logger.info("Mean response time: {} ms for {} requests", mean, NUMBER_OF_REQUESTS);

    long max = responseTimes.stream().mapToInt(Integer::intValue).max().orElse(0);

    logger.info("Max response time: {} ms for {} requests", max, NUMBER_OF_REQUESTS);

    long variance =
        responseTimes.stream()
                .mapToInt(Integer::intValue)
                .map(i -> i - (int) mean)
                .map(i -> i * i)
                .sum()
            / responseTimes.size();

    long stdDev = (long) Math.sqrt(variance);
    logger.info(
        "Standard deviation of response times: {} ms for  {}  requests",
        stdDev,
        NUMBER_OF_REQUESTS);

    assertTrue(
        mean < 3000,
        "Request took too long: " + mean + " ms (sd: " + stdDev + "ms, max: " + max + "ms)");
  }

  @Test
  @Disabled
  void testPerformanceMultiThreaded() throws InterruptedException {
    ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    List<Future<Integer>> futures = new ArrayList<>();

    for (int i = 1; i <= NUMBER_OF_REQUESTS; i++) {
      final int requestNumber = i;
      futures.add(
          executor.submit(
              () -> {
                long t1 = System.currentTimeMillis();
                String resp = doDemoRequest();
                long t2 = System.currentTimeMillis();

                if (!resp.contains("Contacts")) {
                  throw new AssertionError("Response does not contain expected data: " + resp);
                }

                logger.info("Response time: {} ms, request: {}", t2 - t1, requestNumber);
                return (int) (t2 - t1);
              }));
    }

    executor.shutdown();
    executor.awaitTermination(5, TimeUnit.MINUTES);

    List<Integer> responseTimes =
        futures.stream()
            .map(
                future -> {
                  try {
                    return future.get();
                  } catch (Exception e) {
                    throw new RuntimeException("Error getting response time", e);
                  }
                })
            .collect(Collectors.toList());

    analyzeAndAssert(responseTimes);
  }

  private void analyzeAndAssert(List<Integer> responseTimes) {
    long mean = responseTimes.stream().mapToInt(Integer::intValue).sum() / responseTimes.size();
    long max = responseTimes.stream().mapToInt(Integer::intValue).max().orElse(0);
    long variance =
        responseTimes.stream().mapToInt(i -> i - (int) mean).map(i -> i * i).sum()
            / responseTimes.size();
    long stdDev = (long) Math.sqrt(variance);

    logger.info("Mean response time: {} ms for {} requests", mean, responseTimes.size());
    logger.info("Max response time: {} ms for {} requests", max, responseTimes.size());
    logger.info("Standard deviation: {} ms", stdDev);

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

  private static void createDemoData() {
    // create an admin session to work with
    String adminPass =
        (String)
            EnvironmentProperty.getParameter(
                org.molgenis.emx2.Constants.MOLGENIS_ADMIN_PW, ADMIN_PW_DEFAULT, STRING);
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
}
