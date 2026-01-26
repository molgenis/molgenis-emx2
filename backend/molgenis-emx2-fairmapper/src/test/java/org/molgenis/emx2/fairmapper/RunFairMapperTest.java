package org.molgenis.emx2.fairmapper;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RunFairMapperTest {
  private PrintStream originalOut;
  private PrintStream originalErr;
  private ByteArrayOutputStream outContent;
  private ByteArrayOutputStream errContent;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    originalOut = System.out;
    originalErr = System.err;
    outContent = new ByteArrayOutputStream();
    errContent = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outContent));
    System.setErr(new PrintStream(errContent));
  }

  @AfterEach
  void tearDown() {
    System.setOut(originalOut);
    System.setErr(originalErr);
  }

  @Test
  void testNoArgs_showsUsage() {
    int exitCode = RunFairMapper.execute();
    assertEquals(0, exitCode);
    assertTrue(outContent.toString().contains("fairmapper"));
    assertTrue(outContent.toString().contains("validate"));
  }

  @Test
  void testHelp() {
    int exitCode = RunFairMapper.execute("--help");
    assertEquals(0, exitCode);
    assertTrue(outContent.toString().contains("Create API adapters"));
  }

  @Test
  void testVersion() {
    int exitCode = RunFairMapper.execute("--version");
    assertEquals(0, exitCode);
    assertTrue(outContent.toString().contains("MOLGENIS FAIRmapper"));
  }

  @Test
  void testValidate_missingBundle() {
    int exitCode = RunFairMapper.execute("validate", "/nonexistent/path");
    assertEquals(1, exitCode);
    assertTrue(errContent.toString().contains("not found"));
  }

  @Test
  void testValidate_validBundle() throws Exception {
    createValidBundle();
    int exitCode = RunFairMapper.execute("validate", tempDir.toString());
    assertEquals(0, exitCode);
    assertTrue(outContent.toString().contains("Bundle valid"));
  }

  @Test
  void testTest_validBundle() throws Exception {
    createValidBundle();
    int exitCode = RunFairMapper.execute("test", tempDir.toString());
    assertEquals(0, exitCode);
    assertTrue(outContent.toString().contains("passed"));
  }

  @Test
  void testTest_missingBundle() {
    int exitCode = RunFairMapper.execute("test", "/nonexistent/path");
    assertEquals(1, exitCode);
    assertTrue(errContent.toString().contains("not found"));
  }

  @Test
  void testDryRun_missingInputFile() throws Exception {
    createValidBundle();
    int exitCode = RunFairMapper.execute("dry-run", tempDir.toString(), "/nonexistent/input.json");
    assertEquals(1, exitCode);
    assertTrue(errContent.toString().contains("Input file not found"));
  }

  @Test
  void testDryRun_validInputFile() throws Exception {
    createValidBundle();
    Path inputFile = tempDir.resolve("input.json");
    Files.writeString(inputFile, "{\"test\": \"value\"}");

    int exitCode = RunFairMapper.execute("dry-run", tempDir.toString(), inputFile.toString());
    assertEquals(0, exitCode);
    assertTrue(outContent.toString().contains("Input"));
  }

  @Test
  void testE2e_noE2eTestsFound() throws Exception {
    createValidBundle();
    int exitCode =
        RunFairMapper.execute("e2e", tempDir.toString(), "--server", "http://localhost:8080");
    assertEquals(0, exitCode);
    assertTrue(outContent.toString().contains("No e2e tests found"));
  }

  @Test
  void testE2e_missingServer() throws Exception {
    createValidBundle();
    int exitCode = RunFairMapper.execute("e2e", tempDir.toString());
    assertEquals(1, exitCode);
    assertTrue(errContent.toString().contains("Server required"));
  }

  private void createValidBundle() throws Exception {
    Path configPath = tempDir.resolve("fairmapper.yaml");
    Path srcDir = tempDir.resolve("src");
    Files.createDirectories(srcDir);

    Path transformFile = srcDir.resolve("transform.jslt");
    Files.writeString(transformFile, ".");

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
        """);
  }
}
