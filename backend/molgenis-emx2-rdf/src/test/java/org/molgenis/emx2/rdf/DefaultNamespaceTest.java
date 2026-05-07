package org.molgenis.emx2.rdf;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DefaultNamespaceTest {

  @Test
  void shouldResolve() {
    assertEquals("http://xmlns.com/foaf/0.1/foo", DefaultNamespace.FOAF.resolve("foo"));
    assertEquals("http://purl.org/dc/terms/foo", DefaultNamespace.DCTERMS.resolve("foo"));
  }
}
