package org.molgenis.emx2.rdf;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.MolgenisException;

class RdfFetcherTest {

  @Test
  void parseTurtleFromInputStream() throws Exception {
    String turtle =
        """
        @prefix dcat: <http://www.w3.org/ns/dcat#> .
        @prefix dcterms: <http://purl.org/dc/terms/> .
        <https://example.org/ds1> a dcat:Dataset ;
          dcterms:title "Test" .
        """;
    InMemoryRDFHandler handler = new InMemoryRDFHandler(false);
    RdfFetcher.parse(
        new ByteArrayInputStream(turtle.getBytes(StandardCharsets.UTF_8)), ".ttl", handler);
    assertFalse(handler.resources.isEmpty());
  }

  @Test
  void parseJsonLdFromInputStream() throws Exception {
    String jsonld =
        """
        {
          "@context": {"dcat": "http://www.w3.org/ns/dcat#", "dcterms": "http://purl.org/dc/terms/"},
          "@type": "dcat:Dataset",
          "@id": "https://example.org/ds1",
          "dcterms:title": "Test"
        }
        """;
    InMemoryRDFHandler handler = new InMemoryRDFHandler(false);
    RdfFetcher.parse(
        new ByteArrayInputStream(jsonld.getBytes(StandardCharsets.UTF_8)),
        "application/ld+json",
        handler);
    assertFalse(handler.resources.isEmpty());
  }

  @Test
  void parseTestDcatCatalogFile() throws Exception {
    InputStream is = getClass().getClassLoader().getResourceAsStream("test-dcat-catalog.ttl");
    InMemoryRDFHandler handler = new InMemoryRDFHandler(false);
    RdfFetcher.parse(is, ".ttl", handler);
    assertTrue(handler.resources.size() >= 5);
  }

  @Test
  void invalidRdfThrowsException() {
    String invalid = "this is not valid RDF";
    assertThrows(
        MolgenisException.class,
        () ->
            RdfFetcher.parse(
                new ByteArrayInputStream(invalid.getBytes()),
                ".ttl",
                new InMemoryRDFHandler(false)));
  }

  @Test
  void defaultsToTurtleForUnknownFormat() throws Exception {
    String turtle =
        """
        @prefix dcat: <http://www.w3.org/ns/dcat#> .
        <https://example.org/ds1> a dcat:Dataset .
        """;
    InMemoryRDFHandler handler = new InMemoryRDFHandler(false);
    RdfFetcher.parse(
        new ByteArrayInputStream(turtle.getBytes(StandardCharsets.UTF_8)), "unknown", handler);
    assertFalse(handler.resources.isEmpty());
  }
}
