package org.molgenis.emx2.fairmapper.extractors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FdpRdfExtractorTest {

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
  void shouldExtractCatalogsAndDatasets(@TempDir Path tempDir) throws IOException {
    Path root = tempDir.resolve("root.ttl");
    Path catalog1 = tempDir.resolve("catalog1.ttl");
    Path catalog2 = tempDir.resolve("catalog2.ttl");
    Path dataset = tempDir.resolve("dataset.ttl");

    URI rootUri = root.toUri();
    URI catalog1Uri = catalog1.toUri();
    URI catalog2Uri = catalog2.toUri();
    URI datasetUri = dataset.toUri();

    Files.writeString(
        root,
        """
        @prefix fdpo: <https://w3id.org/fdp/fdp-o#> .
        @prefix dcterms: <http://purl.org/dc/terms/> .

        <%s> dcterms:title "root" ;
            fdpo:metadataCatalog <%s> ;
            fdpo:metadataCatalog <%s> .
        """
            .formatted(rootUri, catalog1Uri, catalog2Uri));

    Files.writeString(
        catalog1,
        """
        @prefix dcat: <http://www.w3.org/ns/dcat#> .
        @prefix dcterms: <http://purl.org/dc/terms/> .

        <%s> dcterms:title "catalog-1" ;
            dcat:dataset <%s> .
        """
            .formatted(catalog1Uri, datasetUri));

    Files.writeString(
        catalog2,
        """
        @prefix dcterms: <http://purl.org/dc/terms/> .

        <%s> dcterms:title "catalog-2" .
        """
            .formatted(catalog2Uri));

    Files.writeString(
        dataset,
        """
        @prefix dcat: <http://www.w3.org/ns/dcat#> .
        @prefix dcterms: <http://purl.org/dc/terms/> .

        <%s> dcterms:title "dataset" .
        """
            .formatted(datasetUri));

    SailRepository extracted = new SailRepository(new MemoryStore());
    new FdpRdfExtractor(new RemoteRdfExtractor(), rootUri).addRdfToRepository(extracted);

    try (RepositoryConnection connection = extracted.getConnection()) {
      long nrStatements = connection.getStatements(null, null, null).stream().count();
      assertEquals(7, nrStatements);
      assertHasStatement(
          connection, Values.iri(rootUri.toString()), DCTERMS.TITLE, Values.literal("root"));
      assertHasStatement(
          connection,
          Values.iri(rootUri.toString()),
          Values.iri("https://w3id.org/fdp/fdp-o#metadataCatalog"),
          Values.iri(catalog1Uri.toString()));
      assertHasStatement(
          connection,
          Values.iri(rootUri.toString()),
          Values.iri("https://w3id.org/fdp/fdp-o#metadataCatalog"),
          Values.iri(catalog2Uri.toString()));
      assertHasStatement(
          connection,
          Values.iri(catalog1Uri.toString()),
          DCTERMS.TITLE,
          Values.literal("catalog-1"));
      assertHasStatement(
          connection,
          Values.iri(catalog2Uri.toString()),
          DCTERMS.TITLE,
          Values.literal("catalog-2"));
      assertHasStatement(
          connection,
          Values.iri(catalog1Uri.toString()),
          Values.iri("http://www.w3.org/ns/dcat#dataset"),
          Values.iri(datasetUri.toString()));
      assertHasStatement(
          connection, Values.iri(datasetUri.toString()), DCTERMS.TITLE, Values.literal("dataset"));
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
      assertHasStatement(
          connection, Values.iri("https://example.org"), DCTERMS.TITLE, Values.literal("root"));
    }
  }

  private void assertHasStatement(
      RepositoryConnection connection, Resource subject, IRI predicate, Value object) {
    assertTrue(connection.hasStatement(subject, predicate, object, false));
  }
}
