package org.molgenis.emx2.rdf.generators.query.generators;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfPredicate;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.rdf.generators.query.RdfPredicateResolver;

class RdfPredicateResolverTest {

  @Test
  void shouldReturnRdfIriForHttpUrl() {
    RdfPredicate result = RdfPredicateResolver.resolve("http://example.com/petstore#hasName");
    assertEquals("<http://example.com/petstore#hasName>", result.getQueryString());
  }

  @Test
  void shouldReturnRdfIriForHttpsUrl() {
    RdfPredicate result = RdfPredicateResolver.resolve("https://example.com/petstore#hasName");
    assertEquals("<https://example.com/petstore#hasName>", result.getQueryString());
  }

  @Test
  void shouldReturnAsIsForPrefixedName() {
    RdfPredicate result = RdfPredicateResolver.resolve("rdfs:label");
    assertEquals("rdfs:label", result.getQueryString());
  }

  @Test
  void shouldReturnAsIsForCustomPrefix() {
    RdfPredicate result = RdfPredicateResolver.resolve("petstore:hasName");
    assertEquals("petstore:hasName", result.getQueryString());
  }
}
