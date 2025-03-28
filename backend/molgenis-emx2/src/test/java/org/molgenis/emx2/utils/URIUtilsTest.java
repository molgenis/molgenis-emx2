package org.molgenis.emx2.utils;

import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.utils.URIUtils.encodeIRI;

import java.net.URISyntaxException;
import org.apache.commons.text.StringEscapeUtils;
import org.junit.jupiter.api.Test;

class URIUtilsTest {
  // https://www.compart.com/en/unicode/U+0021
  private final String exclamationMark = StringEscapeUtils.unescapeJava("\\u0021"); // "!"
  // https://www.compart.com/en/unicode/U+01C3
  private final String latenLetterRetroflexClick = StringEscapeUtils.unescapeJava("\\u01C3"); // "ǃ"
  // https://www.compart.com/en/unicode/U+00F1
  private final String nWithTilde = StringEscapeUtils.unescapeJava("\\u00F1"); // "ñ"

  @Test
  void testIriEncoding() throws URISyntaxException {
    assertAll(
        () ->
            assertEquals(
                iri("http://example.com/a%20test"), encodeIRI("http://example.com:80/a test")),
        () ->
            assertEquals(
                iri("http://example.com/a!test"),
                encodeIRI("http://example.com:80/a" + exclamationMark + "test")),
        () ->
            assertEquals(
                iri("http://example.com/a%C7%83test"),
                encodeIRI("http://example.com:80/a" + latenLetterRetroflexClick + "test")),
        () ->
            assertEquals(
                iri("http://example.com/a/test"), encodeIRI("http://example.com/a/./test")),
        () ->
            assertEquals(
                iri("http://example.com/%C3%B1"), encodeIRI("http://example.com/" + nWithTilde)),
        () ->
            assertEquals(iri("http://example.com/%C3%B1"), encodeIRI("http://example.com/%c3%b1")));
  }
}
