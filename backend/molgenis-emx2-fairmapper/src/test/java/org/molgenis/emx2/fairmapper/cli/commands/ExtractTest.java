package org.molgenis.emx2.fairmapper.cli.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.molgenis.emx2.MolgenisException;
import picocli.CommandLine;

class ExtractTest {

  @Test
  void shouldWriteExtractedRdfAsTurtleToOutputFile(@TempDir Path tempDir) throws IOException {
    String rootUri = writeRootTtl(tempDir);
    Path outputFile = tempDir.resolve("output.ttl");

    int exitCode =
        new CommandLine(new Extract()).execute("--rdf=" + rootUri, "--output=" + outputFile);
    assertEquals(0, exitCode);
    assertTrue(Files.exists(outputFile));
    assertStatementCount(outputFile, 1);
    assertHasStatement(outputFile, Values.iri(rootUri), DCTERMS.TITLE, Values.literal("root"));
  }

  @Test
  void shouldThrowWhenEndpointCannotBeFetched(@TempDir Path tempDir) {
    Path missingFile = tempDir.resolve("does-not-exist.ttl");
    String missingUri = missingFile.toUri().toString();
    Path outputFile = tempDir.resolve("output.ttl");

    Extract extract = new Extract();
    new CommandLine(extract).parseArgs("--rdf=" + missingUri, "--output=" + outputFile);

    assertThrows(MolgenisException.class, extract::run);
  }

  @Test
  void shouldWrapIOExceptionWhenOutputPathIsInvalid(@TempDir Path tempDir) throws IOException {
    String rootUri = writeRootTtl(tempDir);
    String invalidOutputPath = tempDir.resolve("missing-dir").resolve("output.ttl").toString();

    Extract extract = new Extract();
    new CommandLine(extract).parseArgs("--rdf=" + rootUri, "--output=" + invalidOutputPath);

    assertThrows(RuntimeException.class, extract::run);
  }

  @Test
  void givenPipelineFile_whenExtracting_thenFetchUrlFromExtractStep(@TempDir Path tempDir)
      throws IOException {
    String rootUri = writeRootTtl(tempDir);
    Path config = writePipelineFile(tempDir, "{ url: \"%s\" }".formatted(rootUri));
    Path outputFile = tempDir.resolve("output.ttl");

    int exitCode =
        new CommandLine(new Extract()).execute("--config=" + config, "--output=" + outputFile);

    assertEquals(0, exitCode);
    assertStatementCount(outputFile, 1);
    assertHasStatement(outputFile, Values.iri(rootUri), DCTERMS.TITLE, Values.literal("root"));
  }

  @Test
  void givenPipelineFileWithCrawlSteps_whenExtracting_thenFollowCrawlSteps(@TempDir Path tempDir)
      throws IOException {
    String childUri = writeTtl(tempDir, "child.ttl", "<%s> dcterms:title \"child\" .");
    String rootUri = writeTtl(tempDir, "root.ttl", "<%s> dcat:dataset <" + childUri + "> .");
    Path config =
        writePipelineFile(
            tempDir,
            "{ url: \"%s\", crawl: [{ name: dataset, predicate: dcat:dataset }] }"
                .formatted(rootUri));
    Path outputFile = tempDir.resolve("output.ttl");

    int exitCode =
        new CommandLine(new Extract())
            .execute("-c", config.toString(), "-o", outputFile.toString());

    assertEquals(0, exitCode);
    assertHasStatement(outputFile, Values.iri(childUri), DCTERMS.TITLE, Values.literal("child"));
  }

  @Test
  void givenPipelineFileNotStrict_whenCrawledResourceIsMissing_thenKeepGoing(@TempDir Path tempDir)
      throws IOException {
    String missingUri = tempDir.resolve("missing.ttl").toUri().toString();
    String rootUri = writeTtl(tempDir, "root.ttl", "<%s> dcat:dataset <" + missingUri + "> .");
    Path config =
        writePipelineFile(
            tempDir,
            "{ url: \"%s\", strict: false, crawl: [{ name: dataset, predicate: dcat:dataset }] }"
                .formatted(rootUri));
    Path outputFile = tempDir.resolve("output.ttl");

    int exitCode =
        new CommandLine(new Extract())
            .execute("-c", config.toString(), "-o", outputFile.toString());

    assertEquals(0, exitCode);
    assertStatementCount(outputFile, 1);
  }

  @Test
  void givenInvalidPipelineFile_whenExtracting_thenWriteNothing(@TempDir Path tempDir)
      throws IOException {
    Path config = tempDir.resolve("pipeline.yaml");
    Files.writeString(config, "steps: []\n");
    Path outputFile = tempDir.resolve("output.ttl");

    Extract extract = new Extract();
    new CommandLine(extract).parseArgs("-c", config.toString(), "-o", outputFile.toString());

    assertThrows(MolgenisException.class, extract::run);
    assertFalse(Files.exists(outputFile));
  }

  @Test
  void givenBothRdfAndPipelineFile_whenExtracting_thenRejectArguments(@TempDir Path tempDir) {
    int exitCode =
        new CommandLine(new Extract())
            .execute(
                "-r",
                "https://fdp.example.org",
                "-c",
                tempDir.resolve("pipeline.yaml").toString(),
                "-o",
                tempDir.resolve("output.ttl").toString());

    assertNotEquals(0, exitCode);
  }

  @Test
  void givenNeitherRdfNorPipelineFile_whenExtracting_thenRejectArguments(@TempDir Path tempDir) {
    int exitCode =
        new CommandLine(new Extract()).execute("-o", tempDir.resolve("output.ttl").toString());

    assertNotEquals(0, exitCode);
  }

  /** Writes a valid pipeline file with the given extract step options. */
  private static Path writePipelineFile(Path tempDir, String extractOptions) throws IOException {
    Path config = tempDir.resolve("pipeline.yaml");
    Files.writeString(
        config,
        """
        schema: catalogue
        tables: [Collections]
        emx2: { endpoint: http://localhost:8080, token: token }
        steps:
          - extract: %s
          - transform
        """
            .formatted(extractOptions));
    return config;
  }

  /** Writes a Turtle file whose content may refer to its own URI as {@code %s}. */
  private static String writeTtl(Path tempDir, String fileName, String content) throws IOException {
    Path file = tempDir.resolve(fileName);
    String uri = file.toUri().toString();
    Files.writeString(
        file,
        """
        @prefix dcterms: <http://purl.org/dc/terms/> .
        @prefix dcat: <http://www.w3.org/ns/dcat#> .

        """
            + content.formatted(uri)
            + "\n");
    return uri;
  }

  private static String writeRootTtl(Path tempDir) throws IOException {
    Path rootFile = tempDir.resolve("root.ttl");
    String rootUri = rootFile.toUri().toString();
    Files.writeString(
        rootFile,
        """
        @prefix dcterms: <http://purl.org/dc/terms/> .

        <%s> dcterms:title "root" .
        """
            .formatted(rootUri));
    return rootUri;
  }

  private static void assertStatementCount(Path turtleFile, long expectedCount) throws IOException {
    Repository repository = new SailRepository(new MemoryStore());
    try (RepositoryConnection connection = repository.getConnection()) {
      connection.add(turtleFile.toFile());
      long count = connection.getStatements(null, null, null).stream().count();
      assertEquals(expectedCount, count);
    }
  }

  private static void assertHasStatement(
      Path turtleFile,
      org.eclipse.rdf4j.model.Resource subject,
      org.eclipse.rdf4j.model.IRI predicate,
      org.eclipse.rdf4j.model.Value object)
      throws IOException {
    Repository repository = new SailRepository(new MemoryStore());
    try (RepositoryConnection connection = repository.getConnection()) {
      connection.add(turtleFile.toFile());
      assertTrue(connection.hasStatement(subject, predicate, object, false));
    }
  }
}
