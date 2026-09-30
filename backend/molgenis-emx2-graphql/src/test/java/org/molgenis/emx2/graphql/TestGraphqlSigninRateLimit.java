package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.graphql.GraphqlExecutor.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.signin.RateLimit;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.tasks.TaskServiceInMemory;

class TestGraphqlSigninRateLimit {

  private static final String WRONG_PASSWORD_USER = "rateLimitWrongPassword";
  private static final String UNKNOWN_USER = "rateLimitUnknownUser";
  private static final String PASSWORD = "rateLimit123";

  public static final ObjectMapper MAPPER = new ObjectMapper();

  private static GraphqlExecutor graphql;

  @BeforeAll
  static void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    if (database.hasUser(WRONG_PASSWORD_USER)) {
      database.removeUser(WRONG_PASSWORD_USER);
    }

    database.addUser(WRONG_PASSWORD_USER);
    database.setUserPassword(WRONG_PASSWORD_USER, PASSWORD);
    if (database.hasUser(UNKNOWN_USER)) {
      database.removeUser(UNKNOWN_USER);
    }

    graphql = new GraphqlExecutor(database, new TaskServiceInMemory());
  }

  @Test
  void givenWrongPassword_whenRetryingStraightAway_thenBlocked() throws IOException {
    assertFalse(isRateLimited(WRONG_PASSWORD_USER));
    assertTrue(signin(WRONG_PASSWORD_USER, "wrong").contains("user or password unknown"));
    assertTrue(isRateLimited(WRONG_PASSWORD_USER));
    assertTrue(signin(WRONG_PASSWORD_USER, PASSWORD).contains("too many attempts"));
  }

  @Test
  void givenUnknownUser_whenRetryingStraightAway_thenBlocked() throws IOException {
    assertFalse(isRateLimited(UNKNOWN_USER));
    assertTrue(signin(UNKNOWN_USER, "wrong").contains("user or password unknown"));
    assertTrue(isRateLimited(UNKNOWN_USER));
    assertTrue(signin(UNKNOWN_USER, "wrong").contains("too many attempts"));
  }

  private boolean isRateLimited(String email) {
    return RateLimit.getInstance().getRateLimit(email).isPresent();
  }

  private String signin(String email, String password) throws IOException {
    String query =
        "mutation{signin(email:\"%s\",password:\"%s\"){message}}".formatted(email, password);
    return MAPPER
        .readTree(
            convertExecutionResultToJson(
                graphql.execute(query, Map.of(), new GraphqlExecutor.DummySessionHandler())))
        .at("/data/signin/message")
        .textValue();
  }
}
