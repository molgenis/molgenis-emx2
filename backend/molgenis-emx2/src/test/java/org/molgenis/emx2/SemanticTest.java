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
        () ->
            assertEquals(
                "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>",
                new Semantic("http://www.w3.org/1999/02/22-rdf-syntax-ns#type").get()),
        () -> assertEquals("rdf:type", new Semantic("rdf:type").get()),
        () -> assertEquals(":test", new Semantic(":test").get()),
        () -> assertEquals("httpPrefix:test", new Semantic("httpPrefix:test").get()),
        () -> assertThrows(MolgenisException.class, () -> new Semantic("<invalid>")),
        () -> assertThrows(MolgenisException.class, () -> new Semantic("rdf:")),
        () -> assertThrows(MolgenisException.class, () -> new Semantic("test")),
        () -> assertThrows(MolgenisException.class, () -> new Semantic(":")));
  }

  /** Ensure {@link #toString()} reproduces input exactly. */
  @Test
  void testToString() {
    String semanticString = "<http://purl.org/dc/terms/temporal>";
    assertEquals(semanticString, new Semantic(semanticString).toString());
  }
}
