package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;

class SemanticPrefixesTest {
  SemanticPrefixes prefixesSimple =
      new SemanticPrefixes(
          List.of(
              Values.namespace("dcterms", "http://purl.org/dc/terms/"),
              Values.namespace("time", "http://www.w3.org/2006/time#")));

  @Test
  void testMapping() {
    IRI expectedIri = Values.iri("http://purl.org/dc/terms/temporal");

    assertAll(
        () ->
            assertEquals(
                expectedIri,
                prefixesSimple.mapAsIri(new Semantic("<http://purl.org/dc/terms/temporal>"))),
        () -> assertEquals(expectedIri, prefixesSimple.mapAsIri(new Semantic("dcterms:temporal"))));
  }

  @Test
  void testSemanticMappingString() {
    // As most logic is shared with mapAsIri, only validate the output format.
    assertEquals(
        "<http://purl.org/dc/terms/temporal>",
        prefixesSimple.mapAsString(new Semantic("<http://purl.org/dc/terms/temporal>")));
  }

  @Test
  void testRetrieveSemanticPrefixesFromSchema() {
    SchemaMetadata schema =
        new SchemaMetadata("mySchema")
            .setSetting(
                Constants.SETTING_SEMANTIC_PREFIXES,
"""
rdf,http://www.w3.org/1999/02/22-rdf-syntax-ns#
,http://www.w3.org/ns/dcat#
rdfs,http://www.w3.org/2000/01/rdf-schema#
""");
    SemanticPrefixes prefixes = new SemanticPrefixes(schema);

    assertEquals(
        Set.of(
            Values.namespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
            Values.namespace("", "http://www.w3.org/ns/dcat#"),
            Values.namespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#")),
        prefixes.getAllNamespaces());
  }

  @Test
  void testInvalidDuplicateCustomSemanticPrefixes() {
    assertThrows(
        MolgenisException.class,
        () ->
            new SemanticPrefixes(
                List.of(
                    Values.namespace("myPrefix", "http://purl.org/dc/terms/"),
                    Values.namespace("myPrefix", "http://www.w3.org/2006/time#"))));
  }

  @Test
  void testInvalidHttpCustomSemanticPrefixes() {
    assertThrows(
        MolgenisException.class,
        () ->
            new SemanticPrefixes(
                List.of(
                    Values.namespace("myPrefix", "http://purl.org/dc/terms/"),
                    Values.namespace("http", "http://example.com/"))));
  }
}
