package org.molgenis.emx2.fairmapper.extractors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class FdpRdfExtractorTest {

  @Disabled("This test can be used to test against actual FDP endpoints")
  @Test
  void liveTest() {
    String endpoint = "FDP ENDPOINT HERE";
    Repository extract = new SailRepository(new MemoryStore());
    new FdpRdfExtractor(new RemoteRdfExtractor(), URI.create(endpoint)).addRdfToRepository(extract);
    try (RepositoryConnection connection = extract.getConnection()) {
      connection.getStatements(null, null, null).forEach(System.out::println);
    }
  }

  @Test
  void shouldExtractCatalogsAndDatasets() {
    SailRepository fdpRepository = new SailRepository(new MemoryStore());
    addFileRdf(fdpRepository, "fdp.rdf");

    StaticFileRdfExtractor staticFileRdfExtractor =
        new StaticFileRdfExtractor(
            Map.of(
                URI.create("https://example.org/fdp-api"),
                "fdp.rdf",
                URI.create("https://example.org/fdp-api/catalog/1"),
                "catalog1.rdf",
                URI.create("https://example.org/fdp-api/catalog/2"),
                "catalog2.rdf",
                URI.create("https://example.org/fdp-api/dataset/1"),
                "dataset.rdf"));

    SailRepository extracted = new SailRepository(new MemoryStore());
    new FdpRdfExtractor(staticFileRdfExtractor, URI.create("https://example.org/fdp-api"))
        .addRdfToRepository(extracted);

    try (RepositoryConnection connection = extracted.getConnection()) {
      long nrStatements = connection.getStatements(null, null, null).stream().count();
      assertEquals(6, nrStatements);
      assertHasStatements(
          connection,
          Values.iri("https://example.org/fdp-api"),
          Values.iri("https://w3id.org/fdp/fdp-o#metadataCatalog"),
          Values.iri("https://example.org/fdp-api/catalog/1"));
      assertHasStatements(
          connection,
          Values.iri("https://example.org/fdp-api"),
          Values.iri("https://w3id.org/fdp/fdp-o#metadataCatalog"),
          Values.iri("https://example.org/fdp-api/catalog/2"));
      assertHasStatements(
          connection,
          Values.iri("https://example.org/fdp-api/catalog/1"),
          DCTERMS.TITLE,
          Values.literal("catalog 1"));
      assertHasStatements(
          connection,
          Values.iri("https://example.org/fdp-api/catalog/1"),
          Values.iri("http://www.w3.org/ns/dcat#dataset"),
          Values.iri("https://example.org/fdp-api/dataset/1"));
      assertHasStatements(
          connection,
          Values.iri("https://example.org/fdp-api/dataset/1"),
          DCTERMS.TITLE,
          Values.literal("dataset"));
      assertHasStatements(
          connection,
          Values.iri("https://example.org/fdp-api/catalog/2"),
          DCTERMS.TITLE,
          Values.literal("catalog 2"));
    }
  }

  private void assertHasStatements(
      RepositoryConnection connection, Resource subject, IRI predicate, Value object) {
    assertTrue(connection.hasStatement(subject, predicate, object, false));
  }

  private static final class StaticFileRdfExtractor implements RdfExtractor {

    private final Map<URI, String> fileMappings;

    private StaticFileRdfExtractor(Map<URI, String> fileMappings) {
      this.fileMappings = fileMappings;
    }

    @Override
    public void addRdfToRepository(Repository repository, URI rootToAdd) {
      String file = fileMappings.get(rootToAdd);
      addFileRdf(repository, file);
    }
  }

  private static void addFileRdf(Repository repository, String file) {
    try (RepositoryConnection conn = repository.getConnection()) {
      conn.add(file(file));
      conn.commit();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static File file(String filename) {
    return new File(
        Objects.requireNonNull(FdpRdfExtractorTest.class.getResource(filename)).getFile());
  }
}
