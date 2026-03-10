package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("slow")
class HpcGraphqlGuardE2ETest extends ApiTestBase {

  private static final String HPC_SECRET_SETTING = "MOLGENIS_HPC_SHARED_SECRET";
  private static final String TEST_SHARED_SECRET = "hpc-graphql-guard-secret-0123456789abcdef";
  private static String previousSharedSecret;

  @BeforeAll
  static void setup() {
    previousSharedSecret = database.getSetting(HPC_SECRET_SETTING);
    database.setSetting(HPC_SECRET_SETTING, TEST_SHARED_SECRET);
    login(database.getAdminUserName(), "admin");

    // Ensure HPC schema artifacts are initialized in _SYSTEM_.
    HpcTestkit.hpcRequest(sessionId).when().get("/api/hpc/jobs").then().statusCode(200);
  }

  @AfterAll
  static void teardown() {
    if (previousSharedSecret == null || previousSharedSecret.isBlank()) {
      database.removeSetting(HPC_SECRET_SETTING);
    } else {
      database.setSetting(HPC_SECRET_SETTING, previousSharedSecret);
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
