package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.*;

/** End-to-end API contract checks backed by protocol/hpc-protocol.json. */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("slow")
class HpcApiProtocolContractE2ETest extends ApiTestBase {

  private static final String HPC_SECRET_SETTING = "MOLGENIS_HPC_SHARED_SECRET";
  private static final String TEST_SHARED_SECRET =
      "hpc-contract-secret-0123456789abcdef0123456789abcdef";
  private static final String CONTRACT_PROCESSOR = "contract-proc";

  private static JsonNode defs;
  private static String previousSharedSecret;
  private static String workerId;

  private static RequestSpecification hpcRequest() {
    return HpcTestkit.hpcRequest(sessionId);
  }

  @BeforeAll
  static void setupContractContext() throws Exception {
    loadSpec();
    previousSharedSecret = database.getSetting(HPC_SECRET_SETTING);
    database.setSetting(HPC_SECRET_SETTING, TEST_SHARED_SECRET);
    login(database.getAdminUserName(), "admin");
    workerId = HpcTestkit.nextName("contract-worker");
  }

  @AfterAll
  static void restoreSecret() {
    if (previousSharedSecret == null || previousSharedSecret.isBlank()) {
      database.removeSetting(HPC_SECRET_SETTING);
    } else {
      database.setSetting(HPC_SECRET_SETTING, previousSharedSecret);
    }
  }

  @Test
  @Order(1)
  void healthResponseMatchesContract() {
    Map<String, Object> body =
        given().when().get("/api/hpc/health").then().statusCode(200).extract().as(Map.class);

    assertContainsRequiredKeys(body, requiredFields("health"));
  }

  @Test
  @Order(2)
  void requiredHeadersAreEnforced() {
    Set<String> required = asStringSet(defs.get("requiredHeaders").get("const"));
    for (String headerToOmit : required) {
      RequestSpecification req = given().sessionId(sessionId).contentType("application/json");
      if (!"X-EMX2-API-Version".equals(headerToOmit)) req.header("X-EMX2-API-Version", "2025-01");
      if (!"X-Request-Id".equals(headerToOmit)) req.header("X-Request-Id", HpcTestkit.nextUuid());
      if (!"X-Timestamp".equals(headerToOmit)) req.header("X-Timestamp", "2026-01-01T00:00:00Z");

      Map<String, Object> problem =
          req.when()
              .get("/api/hpc/jobs")
              .then()
              .statusCode(400)
              .contentType(startsWith("application/problem+json"))
              .extract()
              .as(Map.class);

      assertContainsRequiredKeys(problem, asStringSet(defs.get("ProblemDetail").get("required")));
    }
  }

  @Test
  @Order(3)
  void workerRegistrationShapeMatchesContract() {
    Map<String, Object> worker =
        hpcRequest()
            .body(
                """
                {
                  "worker_id": "%s",
                  "hostname": "contract.test.local",
                  "capabilities": [{"processor":"%s","profile":"any","max_concurrent_jobs":2}]
                }
                """
                    .formatted(workerId, CONTRACT_PROCESSOR))
            .when()
            .post("/api/hpc/workers/register")
            .then()
            .statusCode(200)
            .extract()
            .as(Map.class);

    assertContainsRequiredKeys(worker, requiredFields("workerRegister"));
  }

  @Test
  @Order(4)
  void jobResponsesAndTransitionsMatchContract() {
    String jobId =
        hpcRequest()
            .body(
                """
                {"processor": "%s"}
                """
                    .formatted(CONTRACT_PROCESSOR))
            .when()
            .post("/api/hpc/jobs")
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getString("id");

    Map<String, Object> job =
        hpcRequest()
            .when()
            .get("/api/hpc/jobs/{id}", jobId)
            .then()
            .statusCode(200)
            .extract()
            .as(Map.class);
    assertContainsRequiredKeys(job, requiredFields("job"));
    assertLinkRelations(job, expectedLinks("jobLinksByStatus", "PENDING"));

    Map<String, Object> list =
        hpcRequest()
            .queryParam("status", "PENDING")
            .when()
            .get("/api/hpc/jobs")
            .then()
            .statusCode(200)
            .extract()
            .as(Map.class);
    assertContainsRequiredKeys(list, requiredFields("jobList"));

    hpcRequest()
        .body(
            """
            {"worker_id":"%s"}
            """
                .formatted(workerId))
        .when()
        .post("/api/hpc/jobs/{id}/claim", jobId)
        .then()
        .statusCode(200);
    hpcRequest()
        .body(
            """
            {"status":"SUBMITTED","worker_id":"%s"}
            """
                .formatted(workerId))
        .when()
        .post("/api/hpc/jobs/{id}/transition", jobId)
        .then()
        .statusCode(200);

    Map<String, Object> transitions =
        hpcRequest()
            .when()
            .get("/api/hpc/jobs/{id}/transitions", jobId)
            .then()
            .statusCode(200)
            .extract()
            .as(Map.class);
    assertContainsRequiredKeys(transitions, requiredFields("jobTransitions"));

    Object itemsObj = transitions.get("items");
    assertTrue(itemsObj instanceof java.util.List<?>, "items must be a list");
    java.util.List<?> items = (java.util.List<?>) itemsObj;
    assertFalse(items.isEmpty(), "expected at least one transition");
    assertTrue(items.get(0) instanceof Map<?, ?>, "transition item must be an object");
    @SuppressWarnings("unchecked")
    Map<String, Object> first = (Map<String, Object>) items.get(0);
    assertContainsRequiredKeys(first, requiredItemFields("jobTransition"));
  }

  @Test
  @Order(5)
  void artifactResponsesAndFileShapesMatchContract() {
    String artifactId =
        hpcRequest()
            .body(
                """
                {"type":"dataset","residence":"managed","name":"contract-artifact"}
                """)
            .when()
            .post("/api/hpc/artifacts")
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getString("id");

    Map<String, Object> artifact =
        hpcRequest()
            .when()
            .get("/api/hpc/artifacts/{id}", artifactId)
            .then()
            .statusCode(200)
            .extract()
            .as(Map.class);
    assertContainsRequiredKeys(artifact, requiredFields("artifact"));
    assertLinkRelations(artifact, expectedLinks("artifactLinksByStatus", "CREATED"));

    Map<String, Object> upload =
        hpcRequest()
            .body(
                """
                {"sha256":"abc123","size_bytes":6,"content_type":"text/plain"}
                """)
            .when()
            .put("/api/hpc/artifacts/{id}/files/{path}", artifactId, "results/output.txt")
            .then()
            .statusCode(201)
            .extract()
            .as(Map.class);
    assertContainsRequiredKeys(upload, requiredFields("artifactFileUpload"));

    Map<String, Object> files =
        hpcRequest()
            .when()
            .get("/api/hpc/artifacts/{id}/files", artifactId)
            .then()
            .statusCode(200)
            .extract()
            .as(Map.class);
    assertContainsRequiredKeys(files, requiredFields("artifactFileList"));
    Object itemsObj = files.get("items");
    assertTrue(itemsObj instanceof java.util.List<?>, "items must be a list");
    java.util.List<?> items = (java.util.List<?>) itemsObj;
    assertEquals(1, items.size());
    assertTrue(items.get(0) instanceof Map<?, ?>, "file item must be an object");
    @SuppressWarnings("unchecked")
    Map<String, Object> first = (Map<String, Object>) items.get(0);
    assertContainsRequiredKeys(first, requiredItemFields("artifactFile"));
  }

  @Test
  @Order(6)
  void errorShapeMatchesProblemDetailContract() {
    String jobId =
        hpcRequest()
            .body(
                """
                {"processor":"%s"}
                """
                    .formatted(HpcTestkit.nextName("contract-error")))
            .when()
            .post("/api/hpc/jobs")
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getString("id");

    Response response =
        hpcRequest()
            .body(
                """
                {"status":"COMPLETED","worker_id":"%s"}
                """
                    .formatted(workerId))
            .when()
            .post("/api/hpc/jobs/{id}/transition", jobId);

    Map<String, Object> problem =
        response
            .then()
            .statusCode(409)
            .contentType(startsWith("application/problem+json"))
            .extract()
            .as(Map.class);
    assertContainsRequiredKeys(problem, asStringSet(defs.get("ProblemDetail").get("required")));
  }

  private static void loadSpec() throws Exception {
    InputStream is = HpcApiProtocolContractE2ETest.class.getResourceAsStream("/hpc-protocol.json");
    if (is == null) {
      File specFile = new File("../../protocol/hpc-protocol.json").getCanonicalFile();
      if (!specFile.exists()) {
        fail(
            "Cannot find protocol/hpc-protocol.json — tried classpath and "
                + specFile.getAbsolutePath());
      }
      is = new FileInputStream(specFile);
    }
    defs = new ObjectMapper().readTree(is).get("definitions");
    assertNotNull(defs, "Missing protocol definitions");
  }

  private static Set<String> requiredFields(String section) {
    return asStringSet(
        defs.get("responseRequiredFields").get("properties").get(section).get("const"));
  }

  private static Set<String> requiredItemFields(String section) {
    return asStringSet(
        defs.get("responseItemRequiredFields").get("properties").get(section).get("const"));
  }

  private static Set<String> expectedLinks(String section, String status) {
    return asStringSet(defs.get(section).get("properties").get(status).get("const"));
  }

  private static Set<String> asStringSet(JsonNode node) {
    Set<String> values = new LinkedHashSet<>();
    node.forEach(v -> values.add(v.asText()));
    return values;
  }

  private static void assertContainsRequiredKeys(Map<String, Object> body, Set<String> required) {
    for (String key : required) {
      assertTrue(body.containsKey(key), "Missing required field: " + key);
    }
  }

  @SuppressWarnings("unchecked")
  private static void assertLinkRelations(Map<String, Object> body, Set<String> expected) {
    Object linksObj = body.get("_links");
    assertTrue(linksObj instanceof Map<?, ?>, "_links must be an object");
    Map<String, Object> links = (Map<String, Object>) linksObj;
    assertEquals(expected, links.keySet(), "Unexpected link relations");
  }
}
