package org.molgenis.emx2.web.hpc;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.web.ApiTestBase;

@Tag("slow")
class HpcGraphqlGuardE2ETest extends ApiTestBase {

  private static final String HPC_ENABLED_SETTING = "MOLGENIS_HPC_ENABLED";
  private static final String HPC_CREDENTIALS_KEY_SETTING = "MOLGENIS_HPC_CREDENTIALS_KEY";
  private static final String TEST_CREDENTIALS_KEY =
      "hpc-graphql-guard-key-0123456789abcdef0123456789ab";
  private static String previousEnabled;
  private static String previousCredentialsKey;

  @BeforeAll
  static void setup() {
    previousEnabled = database.getSetting(HPC_ENABLED_SETTING);
    previousCredentialsKey = database.getSetting(HPC_CREDENTIALS_KEY_SETTING);
    database.setSetting(HPC_ENABLED_SETTING, "true");
    database.setSetting(HPC_CREDENTIALS_KEY_SETTING, TEST_CREDENTIALS_KEY);
    login(database.getAdminUserName(), "admin");

    // Ensure HPC schema artifacts are initialized in _SYSTEM_.
    HpcTestkit.hpcRequest(sessionId).when().get("/api/hpc/jobs").then().statusCode(200);
  }

  @AfterAll
  static void teardown() {
    if (previousEnabled == null || previousEnabled.isBlank()) {
      database.removeSetting(HPC_ENABLED_SETTING);
    } else {
      database.setSetting(HPC_ENABLED_SETTING, previousEnabled);
    }
    if (previousCredentialsKey == null || previousCredentialsKey.isBlank()) {
      database.removeSetting(HPC_CREDENTIALS_KEY_SETTING);
    } else {
      database.setSetting(HPC_CREDENTIALS_KEY_SETTING, previousCredentialsKey);
    }
  }

  @Test
  void systemGraphqlBlocksHpcMutations() {
    given()
        .sessionId(sessionId)
        .contentType("application/json")
        .body(
            """
            {"query":"mutation{insert(HpcJobs:{processor:\\"x\\"}){message}}"}
            """)
        .when()
        .post("/_SYSTEM_/graphql")
        .then()
        .statusCode(403);
  }

  @Test
  void systemGraphqlStillAllowsQueries() {
    given()
        .sessionId(sessionId)
        .contentType("application/json")
        .body(
            """
            {"query":"{__typename}"}
            """)
        .when()
        .post("/_SYSTEM_/graphql")
        .then()
        .statusCode(200)
        .body("data.__typename", equalTo("Query"));
  }
}
