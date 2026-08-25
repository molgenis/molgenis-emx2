package org.molgenis.emx2.fairmapper.extractors;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.rdf.vocabulary.FDPO;

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
    new FdpRdfExtractor(new RemoteRdfExtractor()).addRdfToRepository(extracted, rootUri);
  }

  @Test
  void shouldHaveNoMoreStatementsThanNeeded() {
    assertStatementCount(extracted, 11);
  }

  @Test
  void shouldExtractCatalogs() {
    assertHasStatements(
        statement(Values.iri(rootUri.toString()), DCTERMS.TITLE, Values.literal("root")),
        statement(
            Values.iri(rootUri.toString()),
            FDPO.METADATA_CATALOG,
            Values.iri(catalog1Uri.toString())),
        statement(
            Values.iri(rootUri.toString()),
            FDPO.METADATA_CATALOG,
            Values.iri(catalog2Uri.toString())),
        statement(Values.iri(catalog1Uri.toString()), DCTERMS.TITLE, Values.literal("catalog-1")),
        statement(Values.iri(catalog2Uri.toString()), DCTERMS.TITLE, Values.literal("catalog-2")),
        statement(
            Values.iri(catalog1Uri.toString()),
            DCAT.HAS_DATASET,
            Values.iri(datasetUri.toString())));
  }

  @Test
  void shouldExtractDatasets() {
    assertHasStatements(
        statement(Values.iri(datasetUri.toString()), DCTERMS.TITLE, Values.literal("dataset")),
        statement(
            Values.iri(datasetUri.toString()),
            DCAT.HAS_DISTRIBUTION,
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
            DCAT.DOWNLOAD_URL,
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

  private static void assertStatementCount(SailRepository repository, int expectedNrStatement) {
    try (SailRepositoryConnection connection = repository.getConnection()) {
      long nrStatements = connection.getStatements(null, null, null, false).stream().count();
      assertEquals(expectedNrStatement, nrStatements);
    }
  }

  @Disabled("This test can be used to test against actual FDP endpoints")
  @Test
  void liveTest() {
    String endpoint = "FDP ENDPOINT HERE";
    Repository extract = new SailRepository(new MemoryStore());
    Assertions.assertDoesNotThrow(
        () ->
            new FdpRdfExtractor(new RemoteRdfExtractor())
                .addRdfToRepository(extract, URI.create(endpoint)));
    try (RepositoryConnection connection = extract.getConnection()) {
      connection.getStatements(null, null, null).forEach(System.out::println);
    }
  }

  /**
   * The pipeline passes the URI the user typed, so the trailing slash has to be stripped from the
   * URI the crawl queries with, not only from the one it fetches.
   */
  @Test
  void shouldCrawlOnWhenRootHasTrailingSlash(@TempDir Path tempDir) throws IOException {
    SailRepository repository = new SailRepository(new MemoryStore());

    Path root = tempDir.resolve("root.ttl");
    Path catalog = tempDir.resolve("catalog.ttl");

    Files.writeString(
        root,
        """
        @prefix fdpo: <https://w3id.org/fdp/fdp-o#> .
        <%s> fdpo:metadataCatalog <%s> .
        """
            .formatted(root.toUri(), catalog.toUri()));
    Files.writeString(
        catalog,
        """
        @prefix dcterms: <http://purl.org/dc/terms/> .
        <%s> dcterms:title "catalog" .
        """
            .formatted(catalog.toUri()));

    new FdpRdfExtractor(new RemoteRdfExtractor())
        .addRdfToRepository(repository, URI.create(root.toUri() + "//"));

    try (RepositoryConnection connection = repository.getConnection()) {
      assertHasStatements(
          connection,
          statement(
              Values.iri(catalog.toUri().toString()), DCTERMS.TITLE, Values.literal("catalog")));
    }
  }

  @Test
  void shouldExecuteStepConfiguredStepsOnly(@TempDir Path tempDir) throws IOException {
    SailRepository repository = new SailRepository(new MemoryStore());
    IRI partOf = Values.iri("https://example.org/ns#part");

    Path root = tempDir.resolve("root.ttl");
    Path part = tempDir.resolve("part.ttl");

    Files.writeString(root, "<%s> <%s> <%s> .%n".formatted(root.toUri(), partOf, part.toUri()));
    Files.writeString(
        part,
        """
        @prefix dcterms: <http://purl.org/dc/terms/> .
        <%s> dcterms:title "part" .
        """
            .formatted(part.toUri()));

    new FdpRdfExtractor(new RemoteRdfExtractor())
        .withCrawlSteps(new CrawlStep("part", partOf))
        .addRdfToRepository(repository, root.toUri());

    try (RepositoryConnection connection = repository.getConnection()) {
      assertHasStatements(
          connection,
          statement(Values.iri(part.toUri().toString()), DCTERMS.TITLE, Values.literal("part")));
    }
  }

  @Nested
  class StrictModeTest {

    private final IRI partOf = Values.iri("https://example.org/ns#part");
    private SailRepository repository;
    private Path root;
    private Path data;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
      repository = new SailRepository(new MemoryStore());
      root = tempDir.resolve("root.ttl");
      data = tempDir.resolve("data.csv");
      Files.writeString(data, "id,name%n1,first%n".formatted());
    }

    @Test
    void whenNotStrictMode_thenContinue() throws IOException {
      Path part = root.resolveSibling("part.ttl");
      Files.writeString(
          root,
          "<%s> <%s> <%s>, <%s> .%n".formatted(root.toUri(), partOf, part.toUri(), data.toUri()));
      Files.writeString(
          part,
          """
          @prefix dcterms: <http://purl.org/dc/terms/> .
          <%s> dcterms:title "part" .
          """
              .formatted(part.toUri()));

      Assertions.assertDoesNotThrow(
          () ->
              new FdpRdfExtractor(new RemoteRdfExtractor())
                  .withCrawlSteps(new CrawlStep("part", partOf))
                  .addRdfToRepository(repository, root.toUri()));

      try (RepositoryConnection connection = repository.getConnection()) {
        assertStatementCount(repository, 3);
        assertHasStatements(
            connection,
            statement(Values.iri(part.toUri().toString()), DCTERMS.TITLE, Values.literal("part")));
      }
    }

    @Test
    void whenStrictMode_thenThrow() throws IOException {
      Files.writeString(root, "<%s> <%s> <%s> .%n".formatted(root.toUri(), partOf, data.toUri()));

      FdpRdfExtractor extractor =
          new FdpRdfExtractor(new RemoteRdfExtractor())
              .withCrawlSteps(new CrawlStep("part", partOf))
              .withStrict();

      URI uri = root.toUri();
      assertThrows(
          MolgenisException.class, () -> extractor.addRdfToRepository(repository, uri));
    }
  }
}
