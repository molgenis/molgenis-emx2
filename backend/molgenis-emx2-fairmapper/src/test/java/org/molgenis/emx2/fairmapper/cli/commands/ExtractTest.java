package org.molgenis.emx2.fairmapper.cli.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.rdf4j.model.util.Values;
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
    Path rootFile = tempDir.resolve("root.ttl");
    String rootUri = rootFile.toUri().toString();
    Files.writeString(
        rootFile,
        """
        @prefix dcterms: <http://purl.org/dc/terms/> .

        <%s> dcterms:title "root" .
        """
            .formatted(rootUri));

    Path outputFile = tempDir.resolve("output.ttl");

    int exitCode = new CommandLine(new Extract()).execute(rootUri, outputFile.toString());

    assertEquals(0, exitCode);
    assertTrue(Files.exists(outputFile));
    assertStatementCount(outputFile, 1);
    assertHasStatement(
        outputFile,
        Values.iri(rootUri),
        Values.iri("http://purl.org/dc/terms/title"),
        Values.literal("root"));
  }

  @Test
  void shouldFollowFdpCatalogDatasetDistributionCsvwChainAcrossLocalFiles(@TempDir Path tempDir)
      throws IOException {
    Path rootFile = tempDir.resolve("root.ttl");
    Path catalogFile = tempDir.resolve("catalog.ttl");
    Path datasetFile = tempDir.resolve("dataset.ttl");
    Path distributionFile = tempDir.resolve("distribution.ttl");
    Path csvwFile = tempDir.resolve("csvw.ttl");

    String rootUri = rootFile.toUri().toString();
    String catalogUri = catalogFile.toUri().toString();
    String datasetUri = datasetFile.toUri().toString();
    String distributionUri = distributionFile.toUri().toString();
    String csvwUri = csvwFile.toUri().toString();

    Files.writeString(
        rootFile,
        """
        @prefix fdpo: <https://w3id.org/fdp/fdp-o#> .
        @prefix dcterms: <http://purl.org/dc/terms/> .

        <%s> dcterms:title "root" ;
            fdpo:metadataCatalog <%s> .
        """
            .formatted(rootUri, catalogUri));

    Files.writeString(
        catalogFile,
        """
        @prefix dcat: <http://www.w3.org/ns/dcat#> .
        @prefix dcterms: <http://purl.org/dc/terms/> .

        <%s> dcterms:title "catalog" ;
            dcat:dataset <%s> .
        """
            .formatted(catalogUri, datasetUri));

    Files.writeString(
        datasetFile,
        """
        @prefix dcat: <http://www.w3.org/ns/dcat#> .
        @prefix dcterms: <http://purl.org/dc/terms/> .

        <%s> dcterms:title "dataset" ;
            dcat:distribution <%s> .
        """
            .formatted(datasetUri, distributionUri));

    Files.writeString(
        distributionFile,
        """
        @prefix dcat: <http://www.w3.org/ns/dcat#> .
        @prefix dcterms: <http://purl.org/dc/terms/> .

        <%s> dcterms:title "distribution" ;
            dcat:downloadURL <%s> .
        """
            .formatted(distributionUri, csvwUri));

    Files.writeString(
        csvwFile,
        """
        @prefix dcterms: <http://purl.org/dc/terms/> .

        <%s> dcterms:title "csvw" .
        """
            .formatted(csvwUri));

    Path outputFile = tempDir.resolve("output.ttl");

    int exitCode = new CommandLine(new Extract()).execute(rootUri, outputFile.toString());

    assertEquals(0, exitCode);
    assertStatementCount(outputFile, 9);
    assertHasStatement(
        outputFile,
        Values.iri(rootUri),
        Values.iri("https://w3id.org/fdp/fdp-o#metadataCatalog"),
        Values.iri(catalogUri));
    assertHasStatement(
        outputFile,
        Values.iri(catalogUri),
        Values.iri("http://www.w3.org/ns/dcat#dataset"),
        Values.iri(datasetUri));
    assertHasStatement(
        outputFile,
        Values.iri(datasetUri),
        Values.iri("http://www.w3.org/ns/dcat#distribution"),
        Values.iri(distributionUri));
    assertHasStatement(
        outputFile,
        Values.iri(distributionUri),
        Values.iri("http://www.w3.org/ns/dcat#downloadURL"),
        Values.iri(csvwUri));
    assertHasStatement(
        outputFile,
        Values.iri(csvwUri),
        Values.iri("http://purl.org/dc/terms/title"),
        Values.literal("csvw"));
  }

  @Test
  void shouldThrowWhenEndpointCannotBeFetched(@TempDir Path tempDir) {
    Path missingFile = tempDir.resolve("does-not-exist.ttl");
    String missingUri = missingFile.toUri().toString();
    Path outputFile = tempDir.resolve("output.ttl");

    Extract extract = new Extract();
    new CommandLine(extract).parseArgs(missingUri, outputFile.toString());

    assertThrows(MolgenisException.class, extract::run);
  }

  @Test
  void shouldWrapIOExceptionWhenOutputPathIsInvalid(@TempDir Path tempDir) throws IOException {
    Path rootFile = tempDir.resolve("root.ttl");
    String rootUri = rootFile.toUri().toString();
    Files.writeString(
        rootFile,
        """
        @prefix dcterms: <http://purl.org/dc/terms/> .

        <%s> dcterms:title "root" .
        """
            .formatted(rootUri));

    String invalidOutputPath = tempDir.resolve("missing-dir").resolve("output.ttl").toString();

    Extract extract = new Extract();
    new CommandLine(extract).parseArgs(rootUri, invalidOutputPath);

    assertThrows(RuntimeException.class, extract::run);
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
