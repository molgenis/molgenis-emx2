package org.molgenis.emx2.fairmapper.rdf;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.molgenis.emx2.fairmapper.dcat.DcatHarvestException;

class RdfFetcherTest {

  @Test
  void parsesTurtleContent() throws IOException {
    InputStream is =
        getClass().getResourceAsStream("/org/molgenis/emx2/fairmapper/dcat/catalog.ttl");
    assertNotNull(is);
    String turtle = new String(is.readAllBytes(), StandardCharsets.UTF_8);

    RdfFetcher fetcher = new RdfFetcher();
    Model model = fetcher.parseTurtle(turtle);

    assertNotNull(model);
    assertFalse(model.isEmpty());
    assertTrue(model.size() > 10);
  }

  @Test
  void rejectsOversizedContent() {
    RdfFetcher fetcher = new RdfFetcher(10L);
    assertThrows(DcatHarvestException.class, () -> fetcher.parseTurtle("x".repeat(100)));
  }

  @Test
  void rejectsNonHttpSchemeOnFetch() {
    RdfFetcher fetcher = new RdfFetcher();
    assertThrows(DcatHarvestException.class, () -> fetcher.fetch("ftp://example.org/rdf"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "RDF fetch failed with status 503",
        "connection timed out",
        "Connection reset by peer",
        "Connection refused"
      })
  void isTransientErrorReturnsTrueForTransientMessages(String message) {
    RdfFetcher fetcher = new RdfFetcher();
    assertTrue(fetcher.isTransientError(new IOException(message)));
  }

  @ParameterizedTest
  @ValueSource(strings = {"RDF fetch failed with status 404", "some other error"})
  void isTransientErrorReturnsFalseForNonTransientMessages(String message) {
    RdfFetcher fetcher = new RdfFetcher();
    assertFalse(fetcher.isTransientError(new IOException(message)));
  }

  @Test
  void isTransientErrorReturnsFalseForNullMessage() {
    RdfFetcher fetcher = new RdfFetcher();
    assertFalse(fetcher.isTransientError(new IOException((String) null)));
  }

  @Test
  void extractObjectUrisFiltersToSameHost() {
    RdfFetcher fetcher = new RdfFetcher();
    Model model = new TreeModel();
    IRI subject = Values.iri("http://example.org/catalog");
    IRI predicate = Values.iri("http://www.w3.org/ns/dcat#dataset");
    model.add(subject, predicate, Values.iri("http://example.org/dataset/1"));
    model.add(subject, predicate, Values.iri("http://other.org/dataset/2"));
    model.add(subject, predicate, Values.literal("a literal object"));

    Set<String> result = fetcher.extractObjectUris(model, Set.of(), "example.org");

    assertEquals(Set.of("http://example.org/dataset/1"), result);
  }

  @Test
  void extractObjectUrisExcludesAlreadyFetched() {
    RdfFetcher fetcher = new RdfFetcher();
    Model model = new TreeModel();
    IRI subject = Values.iri("http://example.org/catalog");
    IRI predicate = Values.iri("http://www.w3.org/ns/dcat#dataset");
    model.add(subject, predicate, Values.iri("http://example.org/dataset/1"));
    model.add(subject, predicate, Values.iri("http://example.org/dataset/2"));

    Set<String> result =
        fetcher.extractObjectUris(model, Set.of("http://example.org/dataset/1"), "example.org");

    assertEquals(Set.of("http://example.org/dataset/2"), result);
  }

  @Test
  void rejectsUrlWithNullScheme() {
    RdfFetcher fetcher = new RdfFetcher();
    assertThrows(DcatHarvestException.class, () -> fetcher.fetch("//example.org/rdf"));
  }

  @Test
  void rejectsInvalidUrlSyntax() {
    RdfFetcher fetcher = new RdfFetcher();
    assertThrows(DcatHarvestException.class, () -> fetcher.fetch("http://[invalid"));
  }

  @Test
  void parseTurtleThrowsOnInvalidContent() {
    RdfFetcher fetcher = new RdfFetcher();
    assertThrows(IOException.class, () -> fetcher.parseTurtle("this is not valid turtle !!!"));
  }
}
