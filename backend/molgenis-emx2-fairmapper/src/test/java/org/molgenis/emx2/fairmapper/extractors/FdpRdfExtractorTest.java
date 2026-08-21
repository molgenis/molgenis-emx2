package org.molgenis.emx2.fairmapper.extractors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class FdpRdfExtractorTest {

  @TempDir static Path tempDir;

  private static URI rootUri;
  private static URI catalog1Uri;
  private static URI catalog2Uri;
  private static URI datasetUri;
  private static URI distributionUri;
  private static URI csvwUri;

  private static SailRepository extracted;
  private static ValueFactory valueFactory;

  @BeforeAll
  static void setUp() throws IOException {
    rootUri = tempDir.resolve("root.ttl").toUri();
    catalog1Uri = tempDir.resolve("catalog1.ttl").toUri();
    catalog2Uri = tempDir.resolve("catalog2.ttl").toUri();
    datasetUri = tempDir.resolve("dataset.ttl").toUri();
    distributionUri = tempDir.resolve("distribution.ttl").toUri();
    csvwUri = tempDir.resolve("csvw.ttl").toUri();

    Files.writeString(
        Path.of(rootUri),
        """
        @prefix fdpo: <https://w3id.org/fdp/fdp-o#> .
        @prefix dcterms: <http://purl.org/dc/terms/> .
        <%s> dcterms:title "root" ;
            fdpo:metadataCatalog <%s> ;
            fdpo:metadataCatalog <%s> .
        """
            .formatted(rootUri, catalog1Uri, catalog2Uri));

    Files.writeString(
        Path.of(catalog1Uri),
        """
        @prefix dcat: <http://www.w3.org/ns/dcat#> .
        @prefix dcterms: <http://purl.org/dc/terms/> .
        <%s> dcterms:title "catalog-1" ;
            dcat:dataset <%s> .
        """
            .formatted(catalog1Uri, datasetUri));

    Files.writeString(
        Path.of(catalog2Uri),
        """
        @prefix dcterms: <http://purl.org/dc/terms/> .
        <%s> dcterms:title "catalog-2" .
        """
            .formatted(catalog2Uri));

    Files.writeString(
        Path.of(datasetUri),
        """
        @prefix dcat: <http://www.w3.org/ns/dcat#> .
        @prefix dcterms: <http://purl.org/dc/terms/> .
        <%s> dcterms:title "dataset" ;
            dcat:distribution <%s> .
        """
            .formatted(datasetUri, distributionUri));

    Files.writeString(
        Path.of(distributionUri),
        """
        @prefix dcat: <http://www.w3.org/ns/dcat#> .
        @prefix dcterms: <http://purl.org/dc/terms/> .
        <%s> dcterms:title "distribution 1" ;
            dcat:downloadURL <%s> .
        """
            .formatted(distributionUri, csvwUri));

    Files.writeString(
        Path.of(csvwUri),
        """
        @prefix dcterms: <http://purl.org/dc/terms/> .
        <%s> dcterms:title "csvw 1" .
        """
            .formatted(csvwUri));

    extracted = new SailRepository(new MemoryStore());
    valueFactory = extracted.getValueFactory();
    new FdpRdfExtractor(new RemoteRdfExtractor(), rootUri).addRdfToRepository(extracted);
  }

  @Test
  void shouldHaveNoMoreStatementsThanNeeded() {
    try (RepositoryConnection connection = extracted.getConnection()) {
      long nrStatements = connection.getStatements(null, null, null).stream().count();
      assertEquals(11, nrStatements);
    }
  }

  @Test
  void shouldExtractCatalogs() {
    assertHasStatements(
        statement(Values.iri(rootUri.toString()), DCTERMS.TITLE, Values.literal("root")),
        statement(
            Values.iri(rootUri.toString()),
            Values.iri("https://w3id.org/fdp/fdp-o#metadataCatalog"),
            Values.iri(catalog1Uri.toString())),
        statement(
            Values.iri(rootUri.toString()),
            Values.iri("https://w3id.org/fdp/fdp-o#metadataCatalog"),
            Values.iri(catalog2Uri.toString())),
        statement(Values.iri(catalog1Uri.toString()), DCTERMS.TITLE, Values.literal("catalog-1")),
        statement(Values.iri(catalog2Uri.toString()), DCTERMS.TITLE, Values.literal("catalog-2")),
        statement(
            Values.iri(catalog1Uri.toString()),
            Values.iri("http://www.w3.org/ns/dcat#dataset"),
            Values.iri(datasetUri.toString())));
  }

  @Test
  void shouldExtractDatasets() {
    assertHasStatements(
        statement(Values.iri(datasetUri.toString()), DCTERMS.TITLE, Values.literal("dataset")),
        statement(
            Values.iri(datasetUri.toString()),
            Values.iri("http://www.w3.org/ns/dcat#distribution"),
            Values.iri(distributionUri.toString())));
  }

  @Test
  void shouldExtractDistributions() {
    assertHasStatements(
        statement(
            Values.iri(distributionUri.toString()),
            DCTERMS.TITLE,
            Values.literal("distribution 1")),
        statement(
            Values.iri(distributionUri.toString()),
            Values.iri("http://www.w3.org/ns/dcat#downloadURL"),
            Values.iri(csvwUri.toString())));
  }

  @Test
  void shouldExtractCsvw() {
    assertHasStatements(
        statement(Values.iri(csvwUri.toString()), DCTERMS.TITLE, Values.literal("csvw 1")));
  }

  private static Statement statement(Resource resource, IRI predicate, Value object) {
    return valueFactory.createStatement(resource, predicate, object);
  }

  private static void assertHasStatements(Statement... statements) {
    try (RepositoryConnection connection = extracted.getConnection()) {
      assertHasStatements(connection, statements);
    }
  }

  private static void assertHasStatements(
      RepositoryConnection connection, Statement... statements) {
    for (Statement statement : statements) {
      assertTrue(connection.hasStatement(statement, false));
    }
  }

  @Disabled("This test can be used to test against actual FDP endpoints")
  @Test
  void liveTest() {
    String endpoint = "FDP ENDPOINT HERE";
    Repository extract = new SailRepository(new MemoryStore());
    Assertions.assertDoesNotThrow(
        () ->
            new FdpRdfExtractor(new RemoteRdfExtractor(), URI.create(endpoint))
                .addRdfToRepository(extract));
    try (RepositoryConnection connection = extract.getConnection()) {
      connection.getStatements(null, null, null).forEach(System.out::println);
    }
  }

  @Test
  void shouldRemoveTrailingSlash(@TempDir Path tempDir) throws IOException {
    SailRepository repository = new SailRepository(new MemoryStore());

    Path root = tempDir.resolve("root.ttl");
    Files.writeString(
        root,
        """
            @prefix dcterms: <http://purl.org/dc/terms/> .
            <https://example.org> dcterms:title "root" .
            """);

    new FdpRdfExtractor(new RemoteRdfExtractor(), URI.create(root.toUri() + "/"))
        .addRdfToRepository(repository, root.toUri());

    try (RepositoryConnection connection = repository.getConnection()) {
      assertHasStatements(
          connection,
          statement(Values.iri("https://example.org"), DCTERMS.TITLE, Values.literal("root")));
    }
  }
}
