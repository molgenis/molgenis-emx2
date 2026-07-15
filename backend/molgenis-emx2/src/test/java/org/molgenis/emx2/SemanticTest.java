package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
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
                List.of("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>"),
                new Semantic("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>")
                    .getSequencePath()),
        () -> assertEquals(List.of("rdf:type"), new Semantic("rdf:type").getSequencePath()),
        () -> assertEquals(List.of(":test"), new Semantic(":test").getSequencePath()),
        () -> assertThrows(MolgenisException.class, () -> new Semantic("<invalid>")),
        () -> assertThrows(MolgenisException.class, () -> new Semantic("rdf:")),
        () -> assertThrows(MolgenisException.class, () -> new Semantic("test")),
        () -> assertThrows(MolgenisException.class, () -> new Semantic(":")),
        () -> assertThrows(MolgenisException.class, () -> new Semantic("urn:test")),
        () -> assertThrows(MolgenisException.class, () -> new Semantic("http:test")),
        () -> assertThrows(MolgenisException.class, () -> new Semantic("https:test")),
        () -> assertThrows(MolgenisException.class, () -> new Semantic("tag:test")));
  }

  /** Ensure {@link #toString()} reproduces input exactly. */
  @Test
  void testToString() {
    String semanticString =
        "<http://purl.org/dc/terms/temporal>/time:hasBeginning/<http://www.w3.org/2006/time#inXSDDate>";
    assertEquals(semanticString, new Semantic(semanticString).toString());
  }
}
