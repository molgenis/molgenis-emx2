package org.molgenis.emx2.fairmapper.cli.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
