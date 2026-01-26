package org.molgenis.emx2.fairmapper;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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
    Path configPath = createValidBundle();
    MappingBundle bundle = bundleLoader.load(configPath);

    assertNotNull(bundle);
    assertEquals("test-bundle", bundle.name());
    assertEquals("1.0.0", bundle.version());
    assertFalse(bundle.endpoints().isEmpty());
  }

  @Test
  void testMissingConfigFile() {
    Path nonExistent = tempDir.resolve("nonexistent/fairmapper.yaml");

    FairMapperException ex =
        assertThrows(FairMapperException.class, () -> bundleLoader.load(nonExistent));

    assertTrue(ex.getMessage().contains("Mapping file not found"));
  }

  @Test
  void testMalformedYaml() throws IOException {
    Path configPath = tempDir.resolve("fairmapper.yaml");
    Files.writeString(
        configPath,
        """
        name: test
        endpoints: [invalid: yaml: syntax
        """);

    FairMapperException ex =
        assertThrows(FairMapperException.class, () -> bundleLoader.load(configPath));

    assertTrue(ex.getMessage().contains("Failed to parse fairmapper.yaml"));
  }

  @Test
  void testMissingName() throws IOException {
    Path configPath = tempDir.resolve("fairmapper.yaml");
    Files.writeString(
        configPath,
        """
        version: 1.0.0
        endpoints: []
        """);

    FairMapperException ex =
        assertThrows(FairMapperException.class, () -> bundleLoader.load(configPath));

    assertTrue(ex.getMessage().contains("Missing required field: name"));
  }

  @Test
  void testMissingEndpoints() throws IOException {
    Path configPath = tempDir.resolve("fairmapper.yaml");
    Files.writeString(
        configPath,
        """
        name: test
        version: 1.0.0
        """);

    FairMapperException ex =
        assertThrows(FairMapperException.class, () -> bundleLoader.load(configPath));

    assertTrue(
        ex.getMessage().contains("mappings or endpoints"),
        "Expected error about missing mappings or endpoints");
  }

  @Test
  void testEmptyEndpoints() throws IOException {
    Path configPath = tempDir.resolve("fairmapper.yaml");
    Files.writeString(
        configPath,
        """
        name: test
        version: 1.0.0
        endpoints: []
        """);

    FairMapperException ex =
        assertThrows(FairMapperException.class, () -> bundleLoader.load(configPath));

    assertTrue(ex.getMessage().contains("endpoints (must have at least one)"));
  }

  @Test
  void testMissingTransformFile() throws IOException {
    Path configPath = tempDir.resolve("fairmapper.yaml");
    Files.writeString(
        configPath,
        """
        name: test
        version: 1.0.0
        endpoints:
          - path: /test
            methods: [GET]
            steps:
              - transform: src/nonexistent.jslt
        """);

    FairMapperException ex =
        assertThrows(FairMapperException.class, () -> bundleLoader.load(configPath));

    assertTrue(ex.getMessage().contains("Transform file not found"));
    assertTrue(ex.getMessage().contains("src/nonexistent.jslt"));
  }

  @Test
  void testMissingQueryFile() throws IOException {
    Path configPath = tempDir.resolve("fairmapper.yaml");
    Files.writeString(
        configPath,
        """
        name: test
        version: 1.0.0
        endpoints:
          - path: /test
            methods: [GET]
            steps:
              - query: src/nonexistent.gql
        """);

    FairMapperException ex =
        assertThrows(FairMapperException.class, () -> bundleLoader.load(configPath));

    assertTrue(ex.getMessage().contains("Query file not found"));
    assertTrue(ex.getMessage().contains("src/nonexistent.gql"));
  }

  @Test
  void testInvalidTransformFileExtension() throws IOException {
    Path configPath = tempDir.resolve("fairmapper.yaml");
    Path srcDir = tempDir.resolve("src");
    Files.createDirectories(srcDir);
    Path wrongExtFile = srcDir.resolve("transform.txt");
    Files.writeString(wrongExtFile, "content");

    Files.writeString(
        configPath,
        """
        name: test
        version: 1.0.0
        endpoints:
          - path: /test
            methods: [GET]
            steps:
              - transform: src/transform.txt
        """);

    FairMapperException ex =
        assertThrows(FairMapperException.class, () -> bundleLoader.load(configPath));

    assertTrue(ex.getMessage().contains("Transform file must have .jslt extension"));
    assertTrue(ex.getMessage().contains("src/transform.txt"));
  }

  @Test
  void testInvalidQueryFileExtension() throws IOException {
    Path configPath = tempDir.resolve("fairmapper.yaml");
    Path srcDir = tempDir.resolve("src");
    Files.createDirectories(srcDir);
    Path wrongExtFile = srcDir.resolve("query.txt");
    Files.writeString(wrongExtFile, "content");

    Files.writeString(
        configPath,
        """
        name: test
        version: 1.0.0
        endpoints:
          - path: /test
            methods: [GET]
            steps:
              - query: src/query.txt
        """);

    FairMapperException ex =
        assertThrows(FairMapperException.class, () -> bundleLoader.load(configPath));

    assertTrue(ex.getMessage().contains("Query file must have .gql extension"));
    assertTrue(ex.getMessage().contains("src/query.txt"));
  }

  @Test
  void testStepWithoutTransformOrQuery() throws IOException {
    Path configPath = tempDir.resolve("fairmapper.yaml");
    Files.writeString(
        configPath,
        """
        name: test
        version: 1.0.0
        endpoints:
          - path: /test
            methods: [GET]
            steps:
              - tests: []
        """);

    FairMapperException ex =
        assertThrows(FairMapperException.class, () -> bundleLoader.load(configPath));

    assertTrue(ex.getMessage().contains("Step must have either transform or query defined"));
  }

  @Test
  void testResolvePath() {
    Path bundleBase = Path.of("/bundle/fairmapper.yaml");
    Path resolved = bundleLoader.resolvePath(bundleBase, "src/transform.jslt");

    assertEquals(Path.of("/bundle/src/transform.jslt").normalize(), resolved);
  }

  @Test
  void testE2eMissingInputFile() throws IOException {
    Path configPath = tempDir.resolve("fairmapper.yaml");
    Files.writeString(
        configPath,
        """
        name: test
        version: 1.0.0
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

    FairMapperException ex =
        assertThrows(FairMapperException.class, () -> bundleLoader.load(configPath));

    assertTrue(ex.getMessage().contains("E2e test input file not found"));
    assertTrue(ex.getMessage().contains("test/e2e/request.json"));
  }

  @Test
  void testE2eMissingOutputFile() throws IOException {
    Path configPath = tempDir.resolve("fairmapper.yaml");
    Path testDir = tempDir.resolve("test/e2e");
    Files.createDirectories(testDir);
    Files.writeString(testDir.resolve("request.json"), "{}");

    Files.writeString(
        configPath,
        """
        name: test
        version: 1.0.0
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

    FairMapperException ex =
        assertThrows(FairMapperException.class, () -> bundleLoader.load(configPath));

    assertTrue(ex.getMessage().contains("E2e test output file not found"));
    assertTrue(ex.getMessage().contains("test/e2e/expected.json"));
  }

  @Test
  void testE2eInvalidMethod() throws IOException {
    Path configPath = tempDir.resolve("fairmapper.yaml");
    Path testDir = tempDir.resolve("test/e2e");
    Files.createDirectories(testDir);
    Files.writeString(testDir.resolve("request.json"), "{}");
    Files.writeString(testDir.resolve("expected.json"), "{}");

    Files.writeString(
        configPath,
        """
        name: test
        version: 1.0.0
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

    FairMapperException ex =
        assertThrows(FairMapperException.class, () -> bundleLoader.load(configPath));

    assertTrue(ex.getMessage().contains("PUT"));
  }

  @Test
  void testE2eValidConfiguration() throws IOException {
    Path configPath = tempDir.resolve("fairmapper.yaml");
    Path srcDir = tempDir.resolve("src");
    Path testDir = tempDir.resolve("test/e2e");
    Files.createDirectories(srcDir);
    Files.createDirectories(testDir);

    Files.writeString(srcDir.resolve("transform.jslt"), ".");
    Files.writeString(testDir.resolve("request.json"), "{}");
    Files.writeString(testDir.resolve("expected.json"), "{}");

    Files.writeString(
        configPath,
        """
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

    MappingBundle bundle = bundleLoader.load(configPath);

    assertNotNull(bundle);
    assertNotNull(bundle.endpoints().get(0).e2e());
    assertEquals("testSchema", bundle.endpoints().get(0).e2e().schema());
    assertEquals(1, bundle.endpoints().get(0).e2e().tests().size());
    assertEquals(HttpMethod.POST, bundle.endpoints().get(0).e2e().tests().get(0).method());
  }

  private Path createValidBundle() throws IOException {
    Path configPath = tempDir.resolve("fairmapper.yaml");
    Path srcDir = tempDir.resolve("src");
    Files.createDirectories(srcDir);

    Path transformFile = srcDir.resolve("transform.jslt");
    Files.writeString(transformFile, ".");

    Path queryFile = srcDir.resolve("query.gql");
    Files.writeString(queryFile, "{ test }");

    Files.writeString(
        configPath,
        """
        name: test-bundle
        version: 1.0.0
        endpoints:
          - path: /test
            methods: [GET]
            steps:
              - transform: src/transform.jslt
              - query: src/query.gql
        """);

    return configPath;
  }
}
