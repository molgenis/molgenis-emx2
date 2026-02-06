package org.molgenis.emx2.fairmapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ContentNegotiatorTest {

  @Test
  void resolveOutputFormat_nullAcceptHeader_returnsDefault() {
    String result = ContentNegotiator.resolveOutputFormat(null, "json");
    assertEquals("json", result);
  }

  @Test
  void resolveOutputFormat_blankAcceptHeader_returnsDefault() {
    String result = ContentNegotiator.resolveOutputFormat("", "turtle");
    assertEquals("turtle", result);
  }

  @Test
  void resolveOutputFormat_textTurtle_returnsTurtle() {
    String result = ContentNegotiator.resolveOutputFormat("text/turtle", "json");
    assertEquals("turtle", result);
  }

  @Test
  void resolveOutputFormat_applicationJson_returnsJson() {
    String result = ContentNegotiator.resolveOutputFormat("application/json", "turtle");
    assertEquals("json", result);
  }

  @Test
  void resolveOutputFormat_applicationLdJson_returnsJsonld() {
    String result = ContentNegotiator.resolveOutputFormat("application/ld+json", "json");
    assertEquals("jsonld", result);
  }

  @Test
  void resolveOutputFormat_applicationNTriples_returnsNtriples() {
    String result = ContentNegotiator.resolveOutputFormat("application/n-triples", "json");
    assertEquals("ntriples", result);
  }

  @Test
  void resolveOutputFormat_unknownType_returnsDefault() {
    String result = ContentNegotiator.resolveOutputFormat("text/html", "json");
    assertEquals("json", result);
  }

  @Test
  void resolveOutputFormat_multipleTypes_returnsFirstMatch() {
    String result =
        ContentNegotiator.resolveOutputFormat("text/html, text/turtle, application/json", "json");
    assertEquals("turtle", result);
  }

  @Test
  void resolveOutputFormat_caseInsensitive_returnsTurtle() {
    String result = ContentNegotiator.resolveOutputFormat("TEXT/TURTLE", "json");
    assertEquals("turtle", result);
  }

  @Test
  void getMimeType_turtle_returnsTextTurtle() {
    String result = ContentNegotiator.getMimeType("turtle");
    assertEquals("text/turtle", result);
  }

  @Test
  void getMimeType_json_returnsApplicationJson() {
    String result = ContentNegotiator.getMimeType("json");
    assertEquals("application/json", result);
  }

  @Test
  void getMimeType_jsonld_returnsApplicationLdJson() {
    String result = ContentNegotiator.getMimeType("jsonld");
    assertEquals("application/ld+json", result);
  }

  @Test
  void getMimeType_ntriples_returnsApplicationNTriples() {
    String result = ContentNegotiator.getMimeType("ntriples");
    assertEquals("application/n-triples", result);
  }

  @Test
  void getMimeType_unknown_returnsApplicationJson() {
    String result = ContentNegotiator.getMimeType("unknown");
    assertEquals("application/json", result);
  }

  @Test
  void getMimeType_caseInsensitive_returnsCorrectMime() {
    String result = ContentNegotiator.getMimeType("TURTLE");
    assertEquals("text/turtle", result);
  }

  @Test
  void isRdfFormat_turtle_returnsTrue() {
    assertTrue(ContentNegotiator.isRdfFormat("turtle"));
  }

  @Test
  void isRdfFormat_jsonld_returnsTrue() {
    assertTrue(ContentNegotiator.isRdfFormat("jsonld"));
  }

  @Test
  void isRdfFormat_ntriples_returnsTrue() {
    assertTrue(ContentNegotiator.isRdfFormat("ntriples"));
  }

  @Test
  void isRdfFormat_json_returnsFalse() {
    assertFalse(ContentNegotiator.isRdfFormat("json"));
  }

  @Test
  void isRdfFormat_null_returnsFalse() {
    assertFalse(ContentNegotiator.isRdfFormat(null));
  }

  @Test
  void isRdfFormat_unknown_returnsFalse() {
    assertFalse(ContentNegotiator.isRdfFormat("unknown"));
  }

  @Test
  void isRdfFormat_caseInsensitive_returnsTrue() {
    assertTrue(ContentNegotiator.isRdfFormat("TURTLE"));
  }
}
