package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;

class SemanticPrefixesTest {
  static final ValueFactory valueFactory = SimpleValueFactory.getInstance();

  @Test
  void testSemanticMapping() {
    SemanticPrefixes prefixes =
        new SemanticPrefixes(
            Values.namespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
            Values.namespace("http", "http://example.com/fromPrefix#"));

    List<IRI> rdfExpected =
        List.of(valueFactory.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));
    List<IRI> httpIriExpected = List.of(valueFactory.createIRI("http://example.com/fromIri#test"));
    List<IRI> httpPrefixExpected =
        List.of(valueFactory.createIRI("http://example.com/fromPrefix#test"));

    assertAll(
        // Backwards compatibility
        () ->
            assertEquals(
                rdfExpected, prefixes.map("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")),
        () -> assertEquals(httpIriExpected, prefixes.map("http://example.com/fromIri#test")),

        // Semantic containing a sequence length of 1
        () ->
            assertEquals(
                rdfExpected, prefixes.map("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>")),
        () -> assertEquals(rdfExpected, prefixes.map("rdf:type")),
        () -> assertEquals(httpIriExpected, prefixes.map("<http://example.com/fromIri#test>")),
        () -> assertEquals(httpPrefixExpected, prefixes.map("http:test")),
        // TODO: Semantic containing a sequence length of 2

        // Invalid legacy IRI
        () -> assertThrows(MolgenisException.class, () -> prefixes.map("http://invalid")),
        // Invalid IRI
        () -> assertThrows(MolgenisException.class, () -> prefixes.map("<invalid>")),
        // Invalid prefixed name
        () -> assertThrows(MolgenisException.class, () -> prefixes.map("undefinedPrefix:test")),
        () -> assertThrows(MolgenisException.class, () -> prefixes.map("rdf:")),
        () -> assertThrows(MolgenisException.class, () -> prefixes.map(":test")),
        () -> assertThrows(MolgenisException.class, () -> prefixes.map("test")),
        () -> assertThrows(MolgenisException.class, () -> prefixes.map(":")));
  }

  @Test
  void retrieveSemanticPrefixesFromSchema() {
    SchemaMetadata schema =
        new SchemaMetadata("mySchema")
            .setSetting(
                Constants.SETTING_SEMANTIC_PREFIXES,
                """
rdf,http://www.w3.org/1999/02/22-rdf-syntax-ns#
rdfs,http://www.w3.org/2000/01/rdf-schema#
""");
    SemanticPrefixes prefixes = new SemanticPrefixes(schema);

    assertEquals(
        Set.of(
            Values.namespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
            Values.namespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#")),
        prefixes.getAllNamespaces());
  }
}
