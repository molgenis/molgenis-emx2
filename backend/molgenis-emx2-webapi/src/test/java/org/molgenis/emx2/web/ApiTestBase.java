package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.molgenis.emx2.Constants.MOLGENIS_METRICS_ENABLED;

import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

/**
 * Abstract base class for API integration tests that require a running Molgenis web service.
 *
 * <p>This class handles the lifecycle of a test web service instance, including:
 *
 * <ul>
 *   <li>Setting up a test database
 *   <li>Starting the Molgenis web service on port 8081
 *   <li>Providing authentication utilities
 * </ul>
 *
 * <p>The web service is started once before all tests in the test class and stopped after all tests
 * complete, improving test performance by avoiding repeated service startups.
 *
 * <p><b>Usage:</b>
 *
 * <pre>{@code
 * public class MyApiTest extends ApiTestBase {
 *
 *   @Test
 *   void testSomething() {
 *     login("admin", "admin");
 *     // Make API calls using RestAssured
 *   }
 * }
 * }</pre>
 */
@ExtendWith(SystemStubsExtension.class)
public abstract class ApiTestBase {

  protected static String sessionId;
  protected static Database database;
  protected static final int PORT = 8081; // other than default so we can see effect
  private static MolgenisWebservice service;

  @BeforeAll
  static void setupService() throws Exception {
    database = TestDatabaseFactory.getTestDatabase();

    // start web service for testing, including env variables
    new EnvironmentVariables(MOLGENIS_METRICS_ENABLED, Boolean.TRUE.toString())
        .execute(
            () -> {
              service = new MolgenisWebservice();
              service.start(PORT);
            });

    // set default rest assured settings
    RestAssured.port = PORT;
    RestAssured.baseURI = "http://localhost";
  }

  @AfterAll
  static void tearDownService() {
    service.stop();
  }

  protected static void login(String username, String password) {
    sessionId =
        given()
            .body(
                """
                {
                  "query":"mutation{signin(email:\\"%s\\",password:\\"%s\\"){message}}"
                }
                """
                    .formatted(username, password))
            .when()
            .post("api/graphql")
            .sessionId();
  }
}
