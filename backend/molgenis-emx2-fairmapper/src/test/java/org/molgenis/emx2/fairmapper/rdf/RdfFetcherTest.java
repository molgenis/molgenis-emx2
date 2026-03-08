package org.molgenis.emx2.fairmapper.rdf;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;
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

  @Test
  void isTransientErrorReturnsTrueForServerError() {
    RdfFetcher fetcher = new RdfFetcher();
    assertTrue(fetcher.isTransientError(new IOException("RDF fetch failed with status 503")));
    assertTrue(fetcher.isTransientError(new IOException("connection timed out")));
    assertTrue(fetcher.isTransientError(new IOException("Connection reset by peer")));
    assertTrue(fetcher.isTransientError(new IOException("Connection refused")));
  }

  @Test
  void isTransientErrorReturnsFalseForClientError() {
    RdfFetcher fetcher = new RdfFetcher();
    assertFalse(fetcher.isTransientError(new IOException("RDF fetch failed with status 404")));
    assertFalse(fetcher.isTransientError(new IOException("some other error")));
    assertFalse(fetcher.isTransientError(new IOException((String) null)));
  }

  @Test
  void extractObjectUrisFiltersToSameHost() {
    RdfFetcher fetcher = new RdfFetcher();
    Model model = new TreeModel();
    var subject = Values.iri("http://example.org/catalog");
    var predicate = Values.iri("http://www.w3.org/ns/dcat#dataset");
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
    var subject = Values.iri("http://example.org/catalog");
    var predicate = Values.iri("http://www.w3.org/ns/dcat#dataset");
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
