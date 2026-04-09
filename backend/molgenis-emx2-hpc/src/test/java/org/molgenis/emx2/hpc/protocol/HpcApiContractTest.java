package org.molgenis.emx2.hpc.protocol;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.hpc.HpcException;
import org.molgenis.emx2.hpc.model.ArtifactStatus;
import org.molgenis.emx2.hpc.model.HpcJobStatus;

/**
 * Protocol conformance and compatibility checks backed by protocol/hpc-protocol.json.
 *
 * <p>If this test fails, Java implementation and protocol spec drifted out of sync.
 */
class HpcApiContractTest {

  private static JsonNode defs;

  @BeforeAll
  static void loadSpec() throws Exception {
    InputStream is = HpcApiContractTest.class.getResourceAsStream("/hpc-protocol.json");
    if (is == null) {
      File specFile = new File("../../protocol/hpc-protocol.json").getCanonicalFile();
      if (!specFile.exists()) {
        fail(
            "Cannot find protocol/hpc-protocol.json — tried classpath and "
                + specFile.getAbsolutePath());
      }
      is = new FileInputStream(specFile);
    }
    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(is);
    defs = root.get("definitions");
    assertNotNull(defs, "Missing 'definitions' in protocol spec");
  }

  @Test
  void apiVersionMatchesSpec() {
    String specVersion = defs.get("apiVersion").get("const").asText();
    assertEquals(ApiVersion.CURRENT, specVersion, "ApiVersion.CURRENT does not match protocol");
  }

  @Test
  void jobStatusesMatchSpec() {
    Set<String> specValues = asStringSet(defs.get("HpcJobStatus").get("enum"));
    Set<String> javaValues =
        Arrays.stream(HpcJobStatus.values()).map(Enum::name).collect(Collectors.toSet());
    assertEquals(specValues, javaValues, "HpcJobStatus enum values do not match protocol");
  }

  @Test
  void artifactStatusesMatchSpec() {
    Set<String> specValues = asStringSet(defs.get("ArtifactStatus").get("enum"));
    Set<String> javaValues =
        Arrays.stream(ArtifactStatus.values()).map(Enum::name).collect(Collectors.toSet());
    assertEquals(specValues, javaValues, "ArtifactStatus enum values do not match protocol");
  }

  @Test
  void transitionsMatchSpec() {
    JsonNode specTransitions = defs.get("transitions").get("properties");
    for (HpcJobStatus status : HpcJobStatus.values()) {
      JsonNode specTargets = specTransitions.get(status.name());
      assertNotNull(specTargets, "No spec transition entry for " + status.name());
      Set<String> specSet = asStringSet(specTargets.get("const"));
      Set<String> javaSet =
          status.allowedTransitions().stream().map(Enum::name).collect(Collectors.toSet());
      assertEquals(specSet, javaSet, "Transitions for " + status.name() + " do not match spec");
    }
  }

  @Test
  void terminalStatusesMatchSpec() {
    Set<String> specSet = asStringSet(defs.get("terminalStatuses").get("const"));
    Set<String> javaSet =
        Arrays.stream(HpcJobStatus.values())
            .filter(HpcJobStatus::isTerminal)
            .map(Enum::name)
            .collect(Collectors.toSet());
    assertEquals(specSet, javaSet, "Terminal statuses do not match protocol");
  }

  @Test
  void headerContractMatchesJavaConstants() {
    Set<String> requiredHeaders = asStringSet(defs.get("requiredHeaders").get("const"));
    Set<String> optionalHeaders = asStringSet(defs.get("optionalHeaders").get("const"));
    Set<String> binaryHeaders = asStringSet(defs.get("binaryBodyHeaders").get("const"));

    assertEquals(
        Set.of(ApiVersion.HEADER_NAME, HpcHeaders.REQUEST_ID, HpcHeaders.TIMESTAMP),
        requiredHeaders,
        "Required protocol headers drifted");
    assertEquals(
        Set.of(HpcHeaders.TRACE_ID, HpcHeaders.NONCE, HpcHeaders.WORKER_ID),
        optionalHeaders,
        "Optional protocol headers drifted");
    assertEquals(Set.of(HpcHeaders.CONTENT_SHA256), binaryHeaders, "Binary headers drifted");
  }

  @Test
  void problemDetailShapeMatchesSpec() {
    Set<String> required = asStringSet(defs.get("ProblemDetail").get("required"));

    Map<String, Object> withRequestId =
        HpcException.conflict("Cannot transition job", "req-123").toProblemDetail();
    for (String key : required) {
      assertTrue(withRequestId.containsKey(key), "Missing required ProblemDetail key: " + key);
    }
    assertEquals("urn:request:req-123", withRequestId.get("instance"));

    Map<String, Object> withoutRequestId =
        HpcException.badRequest("bad input", null).toProblemDetail();
    for (String key : required) {
      assertTrue(withoutRequestId.containsKey(key), "Missing required ProblemDetail key: " + key);
    }
    assertFalse(
        withoutRequestId.containsKey("instance"), "instance must be omitted when no request id");
  }

  @Test
  void jobLinkRelationsMatchSpec() {
    JsonNode expected = defs.get("jobLinksByStatus").get("properties");
    for (HpcJobStatus status : HpcJobStatus.values()) {
      Set<String> expectedRels = asStringSet(expected.get(status.name()).get("const"));
      Set<String> actualRels = LinkBuilder.forJob("job-id", status).keySet();
      assertEquals(expectedRels, actualRels, "Job links for " + status.name() + " drifted");
    }
  }

  @Test
  void artifactLinkRelationsMatchSpec() {
    JsonNode expected = defs.get("artifactLinksByStatus").get("properties");
    for (ArtifactStatus status : ArtifactStatus.values()) {
      Set<String> expectedRels = asStringSet(expected.get(status.name()).get("const"));
      Set<String> actualRels = LinkBuilder.forArtifact("artifact-id", status).keySet();
      assertEquals(expectedRels, actualRels, "Artifact links for " + status.name() + " drifted");
    }
  }

  @Test
  void hmacVectorsMatchSpec() {
    String secret = defs.get("hmacFixtureSecret").get("const").asText();
    JsonNode vectors = defs.get("hmacVectors").get("const");
    HmacVerifier verifier = new HmacVerifier(secret);

    for (JsonNode vector : vectors) {
      String name = vector.get("name").asText();
      String method = vector.get("method").asText();
      String path = vector.get("path").asText();
      String body = vector.get("body").asText();
      String timestamp = vector.get("timestamp").asText();
      String nonce = vector.get("nonce").asText();
      String expectedSig = vector.get("expected_signature").asText();
      String contentSha =
          vector.hasNonNull("content_sha256") ? vector.get("content_sha256").asText() : null;

      String actual = verifier.computeSignature(method, path, body, timestamp, nonce, contentSha);
      assertEquals(expectedSig, actual, "HMAC vector mismatch: " + name);
    }
  }

  private static Set<String> asStringSet(JsonNode node) {
    Set<String> values = new LinkedHashSet<>();
    node.forEach(v -> values.add(v.asText()));
    return values;
  }
}
