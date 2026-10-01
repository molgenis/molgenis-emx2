package org.molgenis.emx2.signin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.graphql.GraphqlExecutor.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.CounterSnapshot;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.graphql.GraphqlExecutor;
import org.molgenis.emx2.signin.RateLimit.AttemptResults;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

class TestGraphqlSigninRateLimit {

  private static final String WRONG_PASSWORD_USER = "rateLimitWrongPassword";
  private static final String UNKNOWN_USER = "rateLimitUnknownUser";
  private static final String BLOCKED_COUNT_USER = "rateLimitBlockedCountUser";
  private static final String PASSWORD = "rateLimit123";

  public static final ObjectMapper MAPPER = new ObjectMapper();
  public static final String SIGNIN_QUERY =
      """
      mutation {
        signin(email: "%s", password: "%s") {
          message
        }
      }
      """;

  static Database database;
  static GraphqlExecutor graphql;

  @BeforeAll
  static void beforeAll() {
    database = TestDatabaseFactory.getTestDatabase();
    graphql = new GraphqlExecutor(database, new TaskServiceInMemory());
  }

  @Test
  void givenWrongPassword_whenRetryingStraightAway_thenBlocked() throws IOException {
    if (database.hasUser(WRONG_PASSWORD_USER)) {
      database.removeUser(WRONG_PASSWORD_USER);
    }

    database.addUser(WRONG_PASSWORD_USER);
    database.setUserPassword(WRONG_PASSWORD_USER, PASSWORD);

    assertFalse(isRateLimited(WRONG_PASSWORD_USER));
    assertTrue(signin(WRONG_PASSWORD_USER, "wrong").contains("user or password unknown"));
    assertTrue(isRateLimited(WRONG_PASSWORD_USER));
    assertTrue(signin(WRONG_PASSWORD_USER, PASSWORD).contains("too many attempts"));
  }

  @Test
  void givenUnknownUser_whenRetryingStraightAway_thenBlocked() throws IOException {
    if (database.hasUser(UNKNOWN_USER)) {
      database.removeUser(UNKNOWN_USER);
    }

    assertFalse(isRateLimited(UNKNOWN_USER));
    assertTrue(signin(UNKNOWN_USER, "wrong").contains("user or password unknown"));
    assertTrue(isRateLimited(UNKNOWN_USER));
    assertTrue(signin(UNKNOWN_USER, "wrong").contains("too many attempts"));
  }

  @Test
  void givenSignin_thenReportedToDefaultRegistry() throws IOException {
    if (database.hasUser(BLOCKED_COUNT_USER)) {
      database.removeUser(BLOCKED_COUNT_USER);
    }

    // other tests in this JVM also count sign-ins, so compare against the value before
    signin(BLOCKED_COUNT_USER, "wrong");
    double before = blockedCount();
    assertTrue(signin(BLOCKED_COUNT_USER, "wrong").contains("too many attempts"));
    assertEquals(before + 1, blockedCount());
  }

  private double blockedCount() {
    // counter names are stored without the _total suffix
    CounterSnapshot snapshot =
        (CounterSnapshot)
            PrometheusRegistry.defaultRegistry.scrape().stream()
                .filter(metric -> metric.getMetadata().getName().equals("emx2_signin_attempts"))
                .findFirst()
                .orElseThrow();

    return snapshot.getDataPoints().stream()
        .filter(
            dataPoint -> AttemptResults.BLOCKED.name().equals(dataPoint.getLabels().get("result")))
        .findFirst()
        .orElseThrow()
        .getValue();
  }

  private boolean isRateLimited(String email) {
    return RateLimit.getInstance().getRateLimit(email).isPresent();
  }

  private String signin(String email, String password) throws IOException {
    String query = SIGNIN_QUERY.formatted(email, password);
    ExecutionResult result =
        graphql.execute(query, Map.of(), new GraphqlExecutor.DummySessionHandler());

    return MAPPER
        .readTree(convertExecutionResultToJson(result))
        .at("/data/signin/message")
        .textValue();
  }
}
