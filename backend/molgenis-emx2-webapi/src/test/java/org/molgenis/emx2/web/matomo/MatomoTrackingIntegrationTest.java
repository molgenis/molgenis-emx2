package org.molgenis.emx2.web.matomo;

import static io.javalin.http.Header.USER_AGENT;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.Constants.MATOMO_URL_ENV;

import io.restassured.http.ContentType;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.web.ApiTestBase;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.mariadb.MariaDBContainer;
import org.testcontainers.utility.MountableFile;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;

class MatomoTrackingIntegrationTest extends ApiTestBase {

  // This token is used to authenticate to the test Matomo test container and is not used outside of
  // this test
  private static final String AUTH_TOKEN = "a29df8564db55bcdfcc696d783db5732";
  private static final Network NETWORK = Network.newNetwork();

  @SystemStub
  static EnvironmentVariables matomoEnv =
      new EnvironmentVariables(MATOMO_URL_ENV, "this-url-is-not-used");

  static MariaDBContainer db =
      new MariaDBContainer("mariadb:11.4")
          .withNetwork(NETWORK)
          .withNetworkAliases("db")
          .withDatabaseName("matomo")
          .withUsername("matomo")
          .withPassword("secret")
          .withCopyFileToContainer(
              MountableFile.forHostPath(resource("gen/dump.sql")),
              "/docker-entrypoint-initdb.d/dump.sql");

  @SuppressWarnings("resource") // started in the static block, stopped in stopContainers()
  static GenericContainer<?> matomo =
      new GenericContainer<>("matomo:5.14.0-apache")
          .withNetwork(NETWORK)
          .withExposedPorts(80)
          .withEnv("MATOMO_DATABASE_HOST", "db")
          .withEnv("MATOMO_DATABASE_ADAPTER", "mysql")
          .withEnv("MATOMO_DATABASE_TABLES_PREFIX", "matomo_")
          .withEnv("MATOMO_DATABASE_USERNAME", "matomo")
          .withEnv("MATOMO_DATABASE_PASSWORD", "secret")
          .withEnv("MATOMO_DATABASE_DBNAME", "matomo")
          .withCopyFileToContainer(
              MountableFile.forHostPath(resource("gen/config.ini.php")),
              "/var/www/html/config/config.ini.php")
          .dependsOn(db)
          .waitingFor(
              Wait.forHttp("/matomo.php")
                  .forStatusCode(200)
                  .withStartupTimeout(Duration.ofMinutes(2)));

  // The containers are started here instead of with @Testcontainers, because the Matomo URL has
  // to be in the environment before ApiTestBase starts the web service, which reads it on startup.
  static {
    Startables.deepStart(db, matomo).join();
    matomoEnv.set(MATOMO_URL_ENV, baseUrl());
  }

  @BeforeAll
  static void setup() {
    login("admin", "admin");
  }

  @AfterAll
  static void stopContainers() {
    matomo.stop();
    db.stop();
    NETWORK.close();
  }

  static String resource(String name) {
    return Objects.requireNonNull(MatomoTrackingIntegrationTest.class.getResource(name)).getPath();
  }

  static String baseUrl() {
    return "http://" + matomo.getHost() + ":" + matomo.getMappedPort(80);
  }

  @Test
  void shouldTrackMatomo() {
    int nrAiRequests = getNrAiChatbotRequests();
    requestWithUserAgent("non-ai-useragent");
    assertTrackingNrRequests(nrAiRequests);

    for (int i = 0; i < RequestClassifier.AI_CHATBOT_UA.size(); i++) {
      requestWithUserAgent(RequestClassifier.AI_CHATBOT_UA.get(i));
      awaitTrackingNrRequests(nrAiRequests + i + 1);
    }
  }

  @Test
  void givenErrorResponse_thenRecordAiChatbotRegardless() {
    int nrAiRequests = getNrAiChatbotRequests();
    given()
        .sessionId(sessionId)
        .header(USER_AGENT, RequestClassifier.AI_CHATBOT_UA.getFirst())
        .post("/non-existing")
        .then()
        .statusCode(404)
        .extract()
        .response();
    awaitTrackingNrRequests(nrAiRequests + 1);
  }

  private static void awaitTrackingNrRequests(int expected) {
    await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertTrackingNrRequests(expected));
  }

  private static int getNrAiChatbotRequests() {
    List<Integer> requestsPerChatbot =
        given()
            .baseUri(baseUrl())
            .contentType(ContentType.URLENC)
            .formParam("module", "API")
            .formParam("method", "BotTracking.getAIChatbotsRealTime")
            .formParam("idSite", 1)
            .formParam("format", "JSON")
            .formParam("token_auth", AUTH_TOKEN)
            .when()
            .post("/index.php")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("requests", Integer.class);
    return requestsPerChatbot.stream().mapToInt(Integer::intValue).sum();
  }

  private static void assertTrackingNrRequests(int expected) {
    assertEquals(expected, getNrAiChatbotRequests());
  }

  private static void requestWithUserAgent(String userAgent) {
    given()
        .sessionId(sessionId)
        .header(USER_AGENT, userAgent)
        .body("{\"query\":\"query { _schemas { id } }\"}")
        .post("/api/graphql")
        .then()
        .statusCode(200)
        .extract()
        .response();
  }
}
