package org.molgenis.emx2.web.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EncodingHelpersTest {

  @Test
  void encodePathSegment() {
    assertEquals("test", EncodingHelpers.encodePathSegment("test"));
    assertEquals("test%20with%20spaces", EncodingHelpers.encodePathSegment("test with spaces"));
    assertEquals("test%2Fwith%2Fslashes", EncodingHelpers.encodePathSegment("test/with/slashes"));
    assertEquals("test%3Awith%3Acolons", EncodingHelpers.encodePathSegment("test:with:colons"));
    assertEquals("test%40with%40at", EncodingHelpers.encodePathSegment("test@with@at"));
    assertEquals(
        "test%20with%20spaces%0Aand%20newlines",
        EncodingHelpers.encodePathSegment("test with spaces\nand newlines"));
  }

  @Test
  void encodeQueryParam() {
    assertEquals("test", EncodingHelpers.encodeQueryParam("test"));
    assertEquals("test+with+spaces", EncodingHelpers.encodeQueryParam("test with spaces"));
    assertEquals("test%2Fwith%2Fslashes", EncodingHelpers.encodeQueryParam("test/with/slashes"));
    assertEquals("test%3Awith%3Acolons", EncodingHelpers.encodeQueryParam("test:with:colons"));
    assertEquals("test%40with%40at", EncodingHelpers.encodeQueryParam("test@with@at"));
    assertEquals(
        "test+with+spaces%0Aand+newlines",
        EncodingHelpers.encodeQueryParam("test with spaces\nand newlines"));
  }
}
