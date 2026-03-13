package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.Constants.MOLGENIS_ADMIN_PW;
import static org.molgenis.emx2.datamodels.DataModels.Profile.DATA_CATALOGUE;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN_PW_DEFAULT;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.restassured.response.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.*;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.utils.EnvironmentProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestMethodOrder(MethodOrderer.MethodName.class)
@Tag("slow")
class DcatHarvestApiTest extends ApiTestBase {

  private static final Logger logger = LoggerFactory.getLogger(DcatHarvestApiTest.class);
  private static final String SCHEMA_NAME = "DcatHarvestTest";
  private static final String ADMIN_PASS =
      (String) EnvironmentProperty.getParameter(MOLGENIS_ADMIN_PW, ADMIN_PW_DEFAULT, STRING);

  private static Schema schema;
  private static HttpServer turtleServer;
  private static int turtlePort;
  private static String harvestTaskId;
  private static int resourceCountBeforeHarvest;

  @BeforeAll
  static void setup() throws Exception {
    login(database.getAdminUserName(), ADMIN_PASS);
    setupSchema();
    startTurtleServer();
  }

  @AfterAll
  static void cleanup() {
    if (turtleServer != null) {
      turtleServer.stop(0);
    }
    if (database.getSchema(SCHEMA_NAME) != null) {
      database.dropSchema(SCHEMA_NAME);
    }
  }

  private static void setupSchema() {
    database.dropSchemaIfExists(SCHEMA_NAME);
    DATA_CATALOGUE.getImportTask(database, SCHEMA_NAME, "", true).run();
    schema = database.getSchema(SCHEMA_NAME);
    resourceCountBeforeHarvest = schema.getTable("Resources").retrieveRows().size();
    logger.info(
        "Created schema {} with DataCatalogue profile, {} resources before harvest",
        SCHEMA_NAME,
        resourceCountBeforeHarvest);
  }

  private static void startTurtleServer() throws IOException {
    InputStream is =
        DcatHarvestApiTest.class.getResourceAsStream("/org/molgenis/emx2/web/test-catalog.ttl");
    if (is == null) {
      throw new IllegalStateException("Test fixture test-catalog.ttl not found on classpath");
    }
    String turtleContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);

    turtleServer = HttpServer.create(new InetSocketAddress(0), 0);
    turtlePort = turtleServer.getAddress().getPort();

    turtleServer.createContext(
        "/catalog/1",
        exchange -> {
          byte[] response = turtleContent.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "text/turtle");
          exchange.sendResponseHeaders(200, response.length);
          exchange.getResponseBody().write(response);
          exchange.getResponseBody().close();
        });

    turtleServer.start();
    logger.info("Turtle test server started on port {}", turtlePort);
  }

  private static void waitForTask(String taskId) throws Exception {
    int maxWaitMs = 30000;
    int pollIntervalMs = 500;
    int elapsed = 0;
    ObjectMapper mapper = new ObjectMapper();
    while (elapsed < maxWaitMs) {
      String body =
          given()
              .sessionId(sessionId)
              .when()
              .get("/api/tasks/" + taskId)
              .then()
              .statusCode(200)
              .extract()
              .body()
              .asString();
      String status = mapper.readTree(body).at("/status").textValue();
      if ("COMPLETED".equals(status)) {
        logger.info("Task {} finished with status {}", taskId, status);
        return;
      }
      if ("ERROR".equals(status)) {
        throw new AssertionError(
            "Harvest task " + taskId + " failed with ERROR. Response: " + body);
      }
      Thread.sleep(pollIntervalMs);
      elapsed += pollIntervalMs;
    }
    throw new IllegalStateException(
        "Task " + taskId + " did not complete within " + maxWaitMs + "ms");
  }

  @Test
  void t01_harvestReturnsTaskReference() {
    String url = "http://localhost:" + turtlePort + "/catalog/1";

    Response response =
        given()
            .sessionId(sessionId)
            .contentType("application/json")
            .body("{\"url\":\"" + url + "\"}")
            .when()
            .post("/" + SCHEMA_NAME + "/api/harvest/dcat")
            .then()
            .statusCode(202)
            .contentType("application/json")
            .body("id", notNullValue())
            .extract()
            .response();

    harvestTaskId = response.path("id");
    assertNotNull(harvestTaskId, "Task ID must be present in response");
    logger.info("Harvest task submitted with id: {}", harvestTaskId);
  }

  @Test
  void t02_resourcesExistInDatabase() throws Exception {
    assertNotNull(harvestTaskId, "t01 must run before t02");
    waitForTask(harvestTaskId);

    Table resources = schema.getTable("Resources");
    assertNotNull(resources, "Resources table must exist");
    List<Row> rows = resources.retrieveRows();
    assertTrue(
        rows.size() > resourceCountBeforeHarvest,
        "Resources table should have more rows after harvest than before ("
            + resourceCountBeforeHarvest
            + " before, "
            + rows.size()
            + " after)");
    logger.info(
        "Found {} resources after harvest (was {} before)",
        rows.size(),
        resourceCountBeforeHarvest);
  }

  @Test
  void t03_rejectsMissingUrl() {
    given()
        .sessionId(sessionId)
        .contentType("application/json")
        .body("{}")
        .when()
        .post("/" + SCHEMA_NAME + "/api/harvest/dcat")
        .then()
        .statusCode(anyOf(is(400), is(500)));
  }

  @Test
  void t04_rejectsInvalidBody() {
    given()
        .sessionId(sessionId)
        .contentType("application/json")
        .body("not json")
        .when()
        .post("/" + SCHEMA_NAME + "/api/harvest/dcat")
        .then()
        .statusCode(anyOf(is(400), is(500)));
  }
}
