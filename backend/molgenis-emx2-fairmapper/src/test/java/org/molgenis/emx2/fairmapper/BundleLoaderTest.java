package org.molgenis.emx2.fairmapper;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.fairmapper.model.HttpMethod;
import org.molgenis.emx2.fairmapper.model.MappingBundle;

class BundleLoaderTest {
  private BundleLoader bundleLoader;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    bundleLoader = new BundleLoader();
  }

  @Test
  void testLoadValidBundle() throws IOException {
    Path mappingYaml = createValidBundle();
    MappingBundle bundle = bundleLoader.load(mappingYaml);

    assertNotNull(bundle);
    assertEquals("molgenis.org/v1", bundle.apiVersion());
    assertEquals("FairMapperBundle", bundle.kind());
    assertEquals("test-bundle", bundle.metadata().name());
    assertFalse(bundle.endpoints().isEmpty());
  }

  @Test
  void testMissingMappingFile() {
    Path nonExistent = tempDir.resolve("nonexistent/mapping.yaml");

    MolgenisException ex =
        assertThrows(MolgenisException.class, () -> bundleLoader.load(nonExistent));

    assertTrue(ex.getMessage().contains("Mapping file not found"));
  }

  @Test
  void testMalformedYaml() throws IOException {
    Path mappingYaml = tempDir.resolve("mapping.yaml");
    Files.writeString(
        mappingYaml,
        """
        apiVersion: molgenis.org/v1
        kind: [invalid: yaml: syntax
        """);

    MolgenisException ex =
        assertThrows(MolgenisException.class, () -> bundleLoader.load(mappingYaml));

    assertTrue(ex.getMessage().contains("Failed to parse mapping.yaml"));
  }

  @Test
  void testMissingApiVersion() throws IOException {
    Path mappingYaml = tempDir.resolve("mapping.yaml");
    Files.writeString(
        mappingYaml,
        """
        kind: FairMapperBundle
        metadata:
          name: test
        endpoints: []
        """);

    MolgenisException ex =
        assertThrows(MolgenisException.class, () -> bundleLoader.load(mappingYaml));

    assertTrue(ex.getMessage().contains("Missing required field: apiVersion"));
  }

  @Test
  void testMissingKind() throws IOException {
    Path mappingYaml = tempDir.resolve("mapping.yaml");
    Files.writeString(
        mappingYaml,
        """
        apiVersion: molgenis.org/v1
        metadata:
          name: test
        endpoints: []
        """);

    MolgenisException ex =
        assertThrows(MolgenisException.class, () -> bundleLoader.load(mappingYaml));

    assertTrue(ex.getMessage().contains("Missing required field: kind"));
  }

  @Test
  void testMissingMetadata() throws IOException {
    Path mappingYaml = tempDir.resolve("mapping.yaml");
    Files.writeString(
        mappingYaml,
        """
        apiVersion: molgenis.org/v1
        kind: FairMapperBundle
        endpoints: []
        """);

    MolgenisException ex =
        assertThrows(MolgenisException.class, () -> bundleLoader.load(mappingYaml));

    assertTrue(ex.getMessage().contains("Missing required field: metadata"));
  }

  @Test
  void testMissingEndpoints() throws IOException {
    Path mappingYaml = tempDir.resolve("mapping.yaml");
    Files.writeString(
        mappingYaml,
        """
        apiVersion: molgenis.org/v1
        kind: FairMapperBundle
        metadata:
          name: test
        """);

    MolgenisException ex =
        assertThrows(MolgenisException.class, () -> bundleLoader.load(mappingYaml));

    assertTrue(ex.getMessage().contains("Missing required field: endpoints"));
  }

  @Test
  void testEmptyEndpoints() throws IOException {
    Path mappingYaml = tempDir.resolve("mapping.yaml");
    Files.writeString(
        mappingYaml,
        """
        apiVersion: molgenis.org/v1
        kind: FairMapperBundle
        metadata:
          name: test
        endpoints: []
        """);

    MolgenisException ex =
        assertThrows(MolgenisException.class, () -> bundleLoader.load(mappingYaml));

    assertTrue(ex.getMessage().contains("endpoints (must have at least one)"));
  }

  @Test
  void testMissingTransformFile() throws IOException {
    Path mappingYaml = tempDir.resolve("mapping.yaml");
    Files.writeString(
        mappingYaml,
        """
        apiVersion: molgenis.org/v1
        kind: FairMapperBundle
        metadata:
          name: test
        endpoints:
          - path: /test
            methods: [GET]
            steps:
              - transform: src/nonexistent.jslt
        """);

    MolgenisException ex =
        assertThrows(MolgenisException.class, () -> bundleLoader.load(mappingYaml));

    assertTrue(ex.getMessage().contains("Transform file not found"));
    assertTrue(ex.getMessage().contains("src/nonexistent.jslt"));
  }

  @Test
  void testMissingQueryFile() throws IOException {
    Path mappingYaml = tempDir.resolve("mapping.yaml");
    Files.writeString(
        mappingYaml,
        """
        apiVersion: molgenis.org/v1
        kind: FairMapperBundle
        metadata:
          name: test
        endpoints:
          - path: /test
            methods: [GET]
            steps:
              - query: src/nonexistent.gql
        """);

    MolgenisException ex =
        assertThrows(MolgenisException.class, () -> bundleLoader.load(mappingYaml));

    assertTrue(ex.getMessage().contains("Query file not found"));
    assertTrue(ex.getMessage().contains("src/nonexistent.gql"));
  }

  @Test
  void testInvalidTransformFileExtension() throws IOException {
    Path mappingYaml = tempDir.resolve("mapping.yaml");
    Path srcDir = tempDir.resolve("src");
    Files.createDirectories(srcDir);
    Path wrongExtFile = srcDir.resolve("transform.txt");
    Files.writeString(wrongExtFile, "content");

    Files.writeString(
        mappingYaml,
        """
        apiVersion: molgenis.org/v1
        kind: FairMapperBundle
        metadata:
          name: test
        endpoints:
          - path: /test
            methods: [GET]
            steps:
              - transform: src/transform.txt
        """);

    MolgenisException ex =
        assertThrows(MolgenisException.class, () -> bundleLoader.load(mappingYaml));

    assertTrue(ex.getMessage().contains("Transform file must have .jslt extension"));
    assertTrue(ex.getMessage().contains("src/transform.txt"));
  }

  @Test
  void testInvalidQueryFileExtension() throws IOException {
    Path mappingYaml = tempDir.resolve("mapping.yaml");
    Path srcDir = tempDir.resolve("src");
    Files.createDirectories(srcDir);
    Path wrongExtFile = srcDir.resolve("query.txt");
    Files.writeString(wrongExtFile, "content");

    Files.writeString(
        mappingYaml,
        """
        apiVersion: molgenis.org/v1
        kind: FairMapperBundle
        metadata:
          name: test
        endpoints:
          - path: /test
            methods: [GET]
            steps:
              - query: src/query.txt
        """);

    MolgenisException ex =
        assertThrows(MolgenisException.class, () -> bundleLoader.load(mappingYaml));

    assertTrue(ex.getMessage().contains("Query file must have .gql extension"));
    assertTrue(ex.getMessage().contains("src/query.txt"));
  }

  @Test
  void testStepWithoutTransformOrQuery() throws IOException {
    Path mappingYaml = tempDir.resolve("mapping.yaml");
    Files.writeString(
        mappingYaml,
        """
        apiVersion: molgenis.org/v1
        kind: FairMapperBundle
        metadata:
          name: test
        endpoints:
          - path: /test
            methods: [GET]
            steps:
              - tests: []
        """);

    MolgenisException ex =
        assertThrows(MolgenisException.class, () -> bundleLoader.load(mappingYaml));

    assertTrue(ex.getMessage().contains("Step must have either transform or query defined"));
  }

  @Test
  void testResolvePath() {
    Path bundleBase = Path.of("/bundle/mapping.yaml");
    Path resolved = bundleLoader.resolvePath(bundleBase, "src/transform.jslt");

    assertEquals(Path.of("/bundle/src/transform.jslt").normalize(), resolved);
  }

  @Test
  void testE2eMissingInputFile() throws IOException {
    Path mappingYaml = tempDir.resolve("mapping.yaml");
    Files.writeString(
        mappingYaml,
        """
        apiVersion: molgenis.org/v1
        kind: FairMapperBundle
        metadata:
          name: test
        endpoints:
          - path: /test
            methods: [GET]
            steps: []
            e2e:
              schema: testSchema
              tests:
                - method: POST
                  input: test/e2e/request.json
                  output: test/e2e/expected.json
        """);

    MolgenisException ex =
        assertThrows(MolgenisException.class, () -> bundleLoader.load(mappingYaml));

    assertTrue(ex.getMessage().contains("E2e test input file not found"));
    assertTrue(ex.getMessage().contains("test/e2e/request.json"));
  }

  @Test
  void testE2eMissingOutputFile() throws IOException {
    Path mappingYaml = tempDir.resolve("mapping.yaml");
    Path testDir = tempDir.resolve("test/e2e");
    Files.createDirectories(testDir);
    Files.writeString(testDir.resolve("request.json"), "{}");

    Files.writeString(
        mappingYaml,
        """
        apiVersion: molgenis.org/v1
        kind: FairMapperBundle
        metadata:
          name: test
        endpoints:
          - path: /test
            methods: [GET]
            steps: []
            e2e:
              schema: testSchema
              tests:
                - method: POST
                  input: test/e2e/request.json
                  output: test/e2e/expected.json
        """);

    MolgenisException ex =
        assertThrows(MolgenisException.class, () -> bundleLoader.load(mappingYaml));

    assertTrue(ex.getMessage().contains("E2e test output file not found"));
    assertTrue(ex.getMessage().contains("test/e2e/expected.json"));
  }

  @Test
  void testE2eInvalidMethod() throws IOException {
    Path mappingYaml = tempDir.resolve("mapping.yaml");
    Path testDir = tempDir.resolve("test/e2e");
    Files.createDirectories(testDir);
    Files.writeString(testDir.resolve("request.json"), "{}");
    Files.writeString(testDir.resolve("expected.json"), "{}");

    Files.writeString(
        mappingYaml,
        """
        apiVersion: molgenis.org/v1
        kind: FairMapperBundle
        metadata:
          name: test
        endpoints:
          - path: /test
            methods: [GET]
            steps: []
            e2e:
              schema: testSchema
              tests:
                - method: PUT
                  input: test/e2e/request.json
                  output: test/e2e/expected.json
        """);

    MolgenisException ex =
        assertThrows(MolgenisException.class, () -> bundleLoader.load(mappingYaml));

    assertTrue(ex.getMessage().contains("PUT"));
  }

  @Test
  void testE2eValidConfiguration() throws IOException {
    Path mappingYaml = tempDir.resolve("mapping.yaml");
    Path srcDir = tempDir.resolve("src");
    Path testDir = tempDir.resolve("test/e2e");
    Files.createDirectories(srcDir);
    Files.createDirectories(testDir);

    Files.writeString(srcDir.resolve("transform.jslt"), ".");
    Files.writeString(testDir.resolve("request.json"), "{}");
    Files.writeString(testDir.resolve("expected.json"), "{}");

    Files.writeString(
        mappingYaml,
        """
        apiVersion: molgenis.org/v1
        kind: FairMapperBundle
        metadata:
          name: test
          version: 1.0.0
        endpoints:
          - path: /test
            methods: [POST]
            steps:
              - transform: src/transform.jslt
            e2e:
              schema: testSchema
              tests:
                - method: POST
                  input: test/e2e/request.json
                  output: test/e2e/expected.json
        """);

    MappingBundle bundle = bundleLoader.load(mappingYaml);

    assertNotNull(bundle);
    assertNotNull(bundle.endpoints().get(0).e2e());
    assertEquals("testSchema", bundle.endpoints().get(0).e2e().schema());
    assertEquals(1, bundle.endpoints().get(0).e2e().tests().size());
    assertEquals(HttpMethod.POST, bundle.endpoints().get(0).e2e().tests().get(0).method());
  }

  private Path createValidBundle() throws IOException {
    Path mappingYaml = tempDir.resolve("mapping.yaml");
    Path srcDir = tempDir.resolve("src");
    Files.createDirectories(srcDir);

    Path transformFile = srcDir.resolve("transform.jslt");
    Files.writeString(transformFile, ".");

    Path queryFile = srcDir.resolve("query.gql");
    Files.writeString(queryFile, "{ test }");

    Files.writeString(
        mappingYaml,
        """
        apiVersion: molgenis.org/v1
        kind: FairMapperBundle
        metadata:
          name: test-bundle
          version: 1.0.0
        endpoints:
          - path: /test
            methods: [GET]
            steps:
              - transform: src/transform.jslt
              - query: src/query.gql
        """);

    return mappingYaml;
  }
}
