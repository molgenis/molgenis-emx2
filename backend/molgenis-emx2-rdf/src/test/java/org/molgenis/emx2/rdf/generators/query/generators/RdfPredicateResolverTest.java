package org.molgenis.emx2.rdf.generators.query.generators;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfPredicate;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.rdf.RdfUtils;
import org.molgenis.emx2.rdf.generators.query.RdfPredicateResolver;
import org.molgenis.emx2.rdf.mappers.NamespaceMapper;

class RdfPredicateResolverTest {

  private static final NamespaceMapper MAPPER = new NamespaceMapper(new SchemaMetadata("petstore"));

  @Test
  void shouldReturnRdfIriForHttpUrl() {
    RdfPredicate result =
        RdfPredicateResolver.resolve("http://example.com/petstore#hasName", MAPPER);
    assertEquals("<http://example.com/petstore#hasName>", result.getQueryString());
  }

  @Test
  void shouldReturnRdfIriForHttpsUrl() {
    RdfPredicate result =
        RdfPredicateResolver.resolve("https://example.com/petstore#hasName", MAPPER);
    assertEquals("<https://example.com/petstore#hasName>", result.getQueryString());
  }

  @Test
  void shouldReturnRdfIriForUrnUri() {
    RdfPredicate result =
        RdfPredicateResolver.resolve("urn:uuid:f81d4fae-7dec-11d0-a765-00a0c91e6bf6", MAPPER);
    assertEquals("<urn:uuid:f81d4fae-7dec-11d0-a765-00a0c91e6bf6>", result.getQueryString());
  }

  @Test
  void shouldReturnAsIsForPrefixedName() {
    RdfPredicate result = RdfPredicateResolver.resolve("rdfs:label", MAPPER);
    assertEquals("rdfs:label", result.getQueryString());
  }

  @Test
  void shouldReturnAsIsForCustomPrefix() {
    SchemaMetadata schema =
        new SchemaMetadata("petstore")
            .setSetting(
                RdfUtils.SETTING_SEMANTIC_PREFIXES,
                "custom-prefix,http://example.com/customprefix#");
    NamespaceMapper mapper = new NamespaceMapper(schema);
    RdfPredicate result = RdfPredicateResolver.resolve("custom-prefix:hasName", mapper);
    assertEquals("custom-prefix:hasName", result.getQueryString());
  }
}
