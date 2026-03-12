package org.molgenis.emx2.web;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

import io.restassured.response.Response;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.*;

/** E2E tests for HPC artifact lifecycle: creation, file upload, commit, and immutability. */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("slow")
class HpcApiArtifactE2ETest extends HpcApiTestBase {

  @Test
  @Order(60)
  void artifactLifecycle() {
    // Create artifact (type must match HpcArtifactType ontology)
    Response createResp =
        hpcRequest()
            .body(
                """
                {"type": "dataset", "format": "csv", "residence": "managed"}
                """)
            .when()
            .post("/api/hpc/artifacts");

    createResp.then().statusCode(201).body("id", notNullValue()).body("type", equalTo("dataset"));

    String artifactId = createResp.jsonPath().getString("id");

    // Upload file via PUT (JSON metadata-only mode)
    hpcRequest()
        .body(
            """
            {
              "sha256": "abc123def456",
              "size_bytes": 1024,
              "content_type": "text/csv"
            }
            """)
        .when()
        .put("/api/hpc/artifacts/{id}/files/{path}", artifactId, "results/output.csv")
        .then()
        .statusCode(201)
        .body("artifact_id", equalTo(artifactId))
        .body("path", equalTo("results/output.csv"));

    // List files
    hpcRequest()
        .when()
        .get("/api/hpc/artifacts/{id}/files", artifactId)
        .then()
        .statusCode(200)
        .body("count", equalTo(1))
        .body("items[0].path", equalTo("results/output.csv"));

    // Commit artifact -- sha256 must match the single file's sha256
    hpcRequest()
        .body(
            """
            {"sha256": "abc123def456", "size_bytes": 1024}
            """)
        .when()
        .post("/api/hpc/artifacts/{id}/commit", artifactId)
        .then()
        .statusCode(200)
        .body("status", equalTo("COMMITTED"));
  }

  @Test
  @Order(61)
  void artifactFileEndpointAcceptsLiteralNestedPaths() {
    String artifactId =
        hpcRequest()
            .body(
                """
                {"type": "dataset", "residence": "managed"}
                """)
            .when()
            .post("/api/hpc/artifacts")
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getString("id");

    hpcRequest()
        .body(
            """
            {
              "sha256": "nested123",
              "size_bytes": 11,
              "content_type": "text/plain"
            }
            """)
        .when()
        .put("/api/hpc/artifacts/{id}/files/results/nested/output.txt", artifactId)
        .then()
        .statusCode(201)
        .body("path", equalTo("results/nested/output.txt"));

    hpcRequest()
        .when()
        .head("/api/hpc/artifacts/{id}/files/results/nested/output.txt", artifactId)
        .then()
        .statusCode(200)
        .header("X-Content-SHA256", equalTo("nested123"))
        .header("Content-Length", equalTo("11"));

    hpcRequest()
        .when()
        .delete("/api/hpc/artifacts/{id}/files/results/nested/output.txt", artifactId)
        .then()
        .statusCode(204);
  }

  @Test
  @Order(615)
  void listFilesUsesDeterministicDatabasePaginationOrder() {
    String artifactId =
        hpcRequest()
            .body(
                """
                {"type": "dataset", "residence": "managed"}
                """)
            .when()
            .post("/api/hpc/artifacts")
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getString("id");

    for (String path : new String[] {"c/output.txt", "a/output.txt", "b/output.txt"}) {
      hpcRequest()
          .body(
              """
              {
                "sha256": "hash-%s",
                "size_bytes": 10,
                "content_type": "text/plain"
              }
              """
                  .formatted(path.replace('/', '-')))
          .when()
          .put("/api/hpc/artifacts/{id}/files/{path}", artifactId, path)
          .then()
          .statusCode(201);
    }

    hpcRequest()
        .queryParam("limit", "2")
        .queryParam("offset", "1")
        .when()
        .get("/api/hpc/artifacts/{id}/files", artifactId)
        .then()
        .statusCode(200)
        .body("total_count", equalTo(3))
        .body("count", equalTo(2))
        .body("items[0].path", equalTo("b/output.txt"))
        .body("items[1].path", equalTo("c/output.txt"));
  }

  @Test
  @Order(62)
  void committedArtifactRejectsFurtherMutation() {
    String artifactId = createCommittedArtifact("dataset", "immutable-e2e");

    // Cannot delete files from a committed artifact.
    hpcRequest()
        .when()
        .delete("/api/hpc/artifacts/{id}/files/{path}", artifactId, "test.txt")
        .then()
        .statusCode(409)
        .body("title", equalTo("Conflict"));

    // Cannot overwrite file content/metadata after commit.
    Response overwrite =
        hpcRequest()
            .body(
                """
                {"sha256": "new-hash", "size_bytes": 99, "content_type": "text/plain"}
                """)
            .when()
            .put("/api/hpc/artifacts/{id}/files/{path}", artifactId, "test.txt");
    Assertions.assertTrue(
        overwrite.statusCode() >= 400, "Committed artifact mutation must be rejected");

    // Cannot add new files after commit either.
    Response addFile =
        hpcRequest()
            .body(
                """
                {"sha256": "new-file-hash", "size_bytes": 21, "content_type": "text/plain"}
                """)
            .when()
            .put("/api/hpc/artifacts/{id}/files/{path}", artifactId, "new-file.txt");
    Assertions.assertTrue(addFile.statusCode() >= 400, "Committed artifact must reject new files");

    // Existing file metadata remains unchanged.
    hpcRequest()
        .when()
        .head("/api/hpc/artifacts/{id}/files/{path}", artifactId, "test.txt")
        .then()
        .statusCode(200)
        .header("X-Content-SHA256", equalTo("abc123"))
        .header("Content-Length", equalTo("12"));
  }

  @Test
  @Order(63)
  void binaryUploadRequiresContentSha256Header() {
    String artifactId =
        hpcRequest()
            .body(
                """
                {"type": "dataset", "residence": "managed"}
                """)
            .when()
            .post("/api/hpc/artifacts")
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getString("id");

    hpcRequest()
        .contentType("application/octet-stream")
        .body("hello".getBytes(StandardCharsets.UTF_8))
        .when()
        .put("/api/hpc/artifacts/{id}/files/{path}", artifactId, "blob.bin")
        .then()
        .statusCode(400)
        .body("detail", equalTo("Content-SHA256 header is required for binary uploads"));
  }

  @Test
  @Order(64)
  void binaryUploadAcceptsValidContentSha256Header() throws Exception {
    String artifactId =
        hpcRequest()
            .body(
                """
                {"type": "dataset", "residence": "managed"}
                """)
            .when()
            .post("/api/hpc/artifacts")
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getString("id");

    byte[] payload = "hello".getBytes(StandardCharsets.UTF_8);
    String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload));

    hpcRequest()
        .contentType("application/octet-stream")
        .header("Content-SHA256", sha256)
        .body(payload)
        .when()
        .put("/api/hpc/artifacts/{id}/files/{path}", artifactId, "blob.bin")
        .then()
        .statusCode(201)
        .body("path", equalTo("blob.bin"))
        .body("sha256", equalTo(sha256))
        .body("size_bytes", equalTo(payload.length));
  }
}
