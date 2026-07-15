package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Note that these tests only validate the format itself as it does not know the exact prefixes used
 * for the schema.
 */
class SemanticTest {
  @Test
  void testSemanticStringToSequencePathLength1() {
    assertAll(
        () ->
            assertEquals(
                "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>",
                new Semantic("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>").get()),
        () -> assertEquals("rdf:type", new Semantic("rdf:type").get()),
        () -> assertEquals(":test", new Semantic(":test").get()),
        () -> assertEquals("httprefix:test", new Semantic("httprefix:test").get()),
        () -> assertThrows(MolgenisException.class, () -> new Semantic("<invalid>")),
        () -> assertThrows(MolgenisException.class, () -> new Semantic("rdf:")),
        () -> assertThrows(MolgenisException.class, () -> new Semantic("test")),
        () -> assertThrows(MolgenisException.class, () -> new Semantic(":")),
        () -> assertThrows(MolgenisException.class, () -> new Semantic("urn:test")),
        () -> assertThrows(MolgenisException.class, () -> new Semantic("http:")),
        () -> assertThrows(MolgenisException.class, () -> new Semantic("http://example.com")),
        () -> assertThrows(MolgenisException.class, () -> new Semantic("https:test")),
        () -> assertThrows(MolgenisException.class, () -> new Semantic("tag:test")),
        () ->
            assertThrows(
                MolgenisException.class,
                () -> new Semantic("urn:uuid:6c259a64-d605-4841-b482-d9c0ab81cdf5")));
  }

  /** Ensure {@link #toString()} reproduces input exactly. */
  @Test
  void testToString() {
    String semanticString = "<http://purl.org/dc/terms/temporal>";
    assertEquals(semanticString, new Semantic(semanticString).toString());
  }
}
