package org.molgenis.emx2.fairmapper.rdf;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.fairmapper.FairMapperException;

class JsonLdToRdfTest {

  private JsonLdToRdf converter;
  private String sampleJsonLd;

  @BeforeEach
  void setUp() {
    converter = new JsonLdToRdf();
    sampleJsonLd =
        """
        {
          "@context": {"dct": "http://purl.org/dc/terms/"},
          "@id": "http://example.org/cat1",
          "@type": "http://www.w3.org/ns/dcat#Catalog",
          "dct:title": "Test Catalog"
        }
        """;
  }

  @Test
  void testConvertToTurtle() {
    String result = converter.convert(sampleJsonLd, "turtle");

    assertNotNull(result);
    assertTrue(
        result.contains("<http://example.org/cat1>") || result.contains("http://example.org/cat1"));
    assertTrue(result.contains("http://www.w3.org/ns/dcat#Catalog"));
    assertTrue(result.contains("Test Catalog"));
  }

  @Test
  void testConvertToNtriples() {
    String result = converter.convert(sampleJsonLd, "ntriples");

    assertNotNull(result);
    assertTrue(result.contains("<http://example.org/cat1>"));
    assertTrue(result.contains("<http://www.w3.org/ns/dcat#Catalog>"));
    assertTrue(result.contains("\"Test Catalog\""));
  }

  @Test
  void testConvertToJsonLd() {
    String result = converter.convert(sampleJsonLd, "jsonld");

    assertNotNull(result);
    assertTrue(result.contains("http://example.org/cat1"));
    assertTrue(result.contains("Test Catalog"));
  }

  @Test
  void testInvalidJsonLd() {
    String invalidJsonLd = "{not valid json}";

    FairMapperException exception =
        assertThrows(FairMapperException.class, () -> converter.convert(invalidJsonLd, "turtle"));

    assertTrue(exception.getMessage().contains("Failed to convert JSON-LD to RDF format"));
  }

  @Test
  void testUnsupportedFormat() {
    FairMapperException exception =
        assertThrows(
            FairMapperException.class, () -> converter.convert(sampleJsonLd, "unsupported"));

    assertNotNull(exception.getMessage());
  }

  @Test
  void testFormatCaseInsensitive() {
    String turtleUpper = converter.convert(sampleJsonLd, "TURTLE");
    String turtleLower = converter.convert(sampleJsonLd, "turtle");

    assertNotNull(turtleUpper);
    assertNotNull(turtleLower);
  }
}
