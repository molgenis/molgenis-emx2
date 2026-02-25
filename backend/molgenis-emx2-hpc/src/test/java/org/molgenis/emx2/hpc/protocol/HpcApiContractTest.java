package org.molgenis.emx2.hpc.protocol;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.hpc.model.ArtifactStatus;
import org.molgenis.emx2.hpc.model.HpcJobStatus;

/**
 * Conformance test: asserts that Java code matches the protocol contract defined in
 * protocol/hpc-protocol.json. If this test fails, either the Java code or the protocol spec is out
 * of sync.
 */
class HpcApiContractTest {

  private static JsonNode defs;

  @BeforeAll
  static void loadSpec() throws Exception {
    // The protocol file is at the repo root; during tests, we resolve via a well-known relative
    // path from the module root. Gradle sets the working directory to the module directory.
    // Try classpath first (if copied), then filesystem fallback.
    InputStream is = HpcApiContractTest.class.getResourceAsStream("/hpc-protocol.json");
    if (is == null) {
      // Filesystem fallback: navigate from module dir to repo root
      java.io.File specFile =
          new java.io.File("../../protocol/hpc-protocol.json").getCanonicalFile();
      if (!specFile.exists()) {
        fail(
            "Cannot find protocol/hpc-protocol.json — tried classpath and "
                + specFile.getAbsolutePath());
      }
      is = new java.io.FileInputStream(specFile);
    }
    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(is);
    defs = root.get("definitions");
    assertNotNull(defs, "Missing 'definitions' in protocol spec");
  }

  @Test
  void jobStatusesMatchSpec() {
    JsonNode specEnum = defs.get("HpcJobStatus").get("enum");
    Set<String> specValues = new LinkedHashSet<>();
    specEnum.forEach(node -> specValues.add(node.asText()));

    Set<String> javaValues =
        Arrays.stream(HpcJobStatus.values()).map(Enum::name).collect(Collectors.toSet());

    assertEquals(specValues, javaValues, "HpcJobStatus enum values do not match protocol spec");
  }

  @Test
  void artifactStatusesMatchSpec() {
    JsonNode specEnum = defs.get("ArtifactStatus").get("enum");
    Set<String> specValues = new LinkedHashSet<>();
    specEnum.forEach(node -> specValues.add(node.asText()));

    Set<String> javaValues =
        Arrays.stream(ArtifactStatus.values()).map(Enum::name).collect(Collectors.toSet());

    assertEquals(specValues, javaValues, "ArtifactStatus enum values do not match protocol spec");
  }

  @Test
  void transitionsMatchSpec() {
    JsonNode specTransitions = defs.get("transitions").get("properties");

    for (HpcJobStatus status : HpcJobStatus.values()) {
      JsonNode specTargets = specTransitions.get(status.name());
      assertNotNull(specTargets, "No spec transition entry for " + status.name());

      JsonNode constArray = specTargets.get("const");
      Set<String> specSet = new LinkedHashSet<>();
      constArray.forEach(node -> specSet.add(node.asText()));

      Set<String> javaSet =
          status.allowedTransitions().stream().map(Enum::name).collect(Collectors.toSet());

      assertEquals(
          specSet, javaSet, "Transitions for " + status.name() + " do not match protocol spec");
    }
  }

  @Test
  void apiVersionMatchesSpec() {
    String specVersion = defs.get("apiVersion").get("const").asText();
    assertEquals(
        specVersion, ApiVersion.CURRENT, "ApiVersion.CURRENT does not match protocol spec");
  }

  @Test
  void terminalStatusesMatchSpec() {
    JsonNode specTerminal = defs.get("terminalStatuses").get("const");
    Set<String> specSet = new LinkedHashSet<>();
    specTerminal.forEach(node -> specSet.add(node.asText()));

    Set<String> javaTerminal =
        Arrays.stream(HpcJobStatus.values())
            .filter(HpcJobStatus::isTerminal)
            .map(Enum::name)
            .collect(Collectors.toSet());

    assertEquals(specSet, javaTerminal, "Terminal statuses do not match protocol spec");
  }
}
