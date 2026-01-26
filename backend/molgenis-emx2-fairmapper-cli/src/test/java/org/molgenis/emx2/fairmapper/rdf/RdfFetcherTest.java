package org.molgenis.emx2.fairmapper.rdf;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import org.eclipse.rdf4j.model.Model;
import org.junit.jupiter.api.Test;

class RdfFetcherTest {

  private static final String SAMPLE_TURTLE =
      """
      @prefix dcat: <http://www.w3.org/ns/dcat#> .
      @prefix dcterms: <http://purl.org/dc/terms/> .

      <https://example.org/catalog/123>
        a dcat:Catalog ;
        dcterms:title "Test Catalog" ;
        dcterms:description "A test catalog for unit testing" .
      """;

  @Test
  void testParseTurtle() throws IOException {
    RdfFetcher fetcher = new RdfFetcher();

    Model model = fetcher.parseTurtle(SAMPLE_TURTLE);

    assertNotNull(model);
    assertEquals(3, model.size());
  }

  @Test
  void testInvalidTurtle() {
    RdfFetcher fetcher = new RdfFetcher();

    IOException exception =
        assertThrows(IOException.class, () -> fetcher.parseTurtle("invalid turtle content"));

    assertTrue(exception.getMessage().contains("Failed to parse Turtle RDF"));
  }

  @Test
  void testFetchFromRealFdp() throws IOException {
    RdfFetcher fetcher = new RdfFetcher();

    String fdpUrl = "https://fdp.radboudumc.nl/catalog/d7522c39-a774-496f-998a-fdeb262a5c65";

    Model model = fetcher.fetch(fdpUrl);

    assertNotNull(model);
    assertTrue(model.size() > 0, "Expected statements in the model");

    System.out.println("Fetched " + model.size() + " statements from real FDP");
  }

  @Test
  void testConvertToJsonLd() throws IOException {
    RdfFetcher fetcher = new RdfFetcher();
    RdfToJsonLd converter = new RdfToJsonLd();

    Model model = fetcher.parseTurtle(SAMPLE_TURTLE);
    String jsonLd = converter.convert(model);

    assertNotNull(jsonLd);
    assertTrue(jsonLd.contains("\"@id\""));
    assertTrue(jsonLd.contains("https://example.org/catalog/123"));

    System.out.println("Generated JSON-LD:");
    System.out.println(jsonLd);
  }
}
