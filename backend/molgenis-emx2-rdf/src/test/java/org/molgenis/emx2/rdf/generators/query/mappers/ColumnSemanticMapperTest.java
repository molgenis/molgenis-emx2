package org.molgenis.emx2.rdf.generators.query.mappers;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfPredicate;
import org.junit.jupiter.api.Test;

class ColumnSemanticMapperTest {

  @Test
  void shouldReturnRdfIriForHttpUrl() {
    RdfPredicate result = ColumnSemanticMapper.resolveIri("http://example.com/petstore#hasName");
    assertEquals("<http://example.com/petstore#hasName>", result.getQueryString());
  }

  @Test
  void shouldReturnRdfIriForHttpsUrl() {
    RdfPredicate result = ColumnSemanticMapper.resolveIri("https://example.com/petstore#hasName");
    assertEquals("<https://example.com/petstore#hasName>", result.getQueryString());
  }

  @Test
  void shouldReturnAsIsForPrefixedName() {
    RdfPredicate result = ColumnSemanticMapper.resolveIri("rdfs:label");
    assertEquals("rdfs:label", result.getQueryString());
  }

  @Test
  void shouldReturnAsIsForCustomPrefix() {
    RdfPredicate result = ColumnSemanticMapper.resolveIri("petstore:hasName");
    assertEquals("petstore:hasName", result.getQueryString());
  }
}
