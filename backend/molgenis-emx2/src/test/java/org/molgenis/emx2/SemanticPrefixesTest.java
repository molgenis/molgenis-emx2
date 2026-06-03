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
            Values.namespace("dcat", "http://www.w3.org/ns/dcat#"),
            Values.namespace("dcterms", "http://purl.org/dc/terms/"),
//            Values.namespace("time", "http://www.w3.org/2006/time#"),
            Values.namespace("http", "http://example.com/fromPrefix#"));

    List<IRI> rdfExpected = List.of(Values.iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));
    List<IRI> httpIriExpected = List.of(valueFactory.createIRI("http://example.com/fromIri#test"));
    List<IRI> httpPrefixExpected = List.of(Values.iri("http://example.com/fromPrefix#test"));

    List<IRI> rdfLength2Expected =
        List.of(
            Values.iri("http://purl.org/dc/terms/temporal"),
            Values.iri("http://www.w3.org/ns/dcat#startDate"));

//    List<IRI> rdfLength3Expected =
//        List.of(
//            Values.iri("http://purl.org/dc/terms/temporal"),
//            Values.iri("http://www.w3.org/2006/time#hasBeginning"),
//            Values.iri("http://www.w3.org/2006/time#inXSDDate"));

    assertAll(
        // Backwards compatibility
        //        () ->
        //            assertEquals(
        //                rdfExpected,
        // prefixes.map("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")),
        //        () -> assertEquals(httpIriExpected,
        // prefixes.map("http://example.com/fromIri#test")),

        // Semantic containing a sequence length of 1
        () ->
            assertEquals(
                rdfExpected, prefixes.map("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>")),
        () -> assertEquals(rdfExpected, prefixes.map("rdf:type")),
        () -> assertEquals(httpIriExpected, prefixes.map("<http://example.com/fromIri#test>")),
        () -> assertEquals(httpPrefixExpected, prefixes.map("http:test")),
        // Semantic containing a sequence length of 2
        () ->
            assertEquals(
                rdfLength2Expected,
                prefixes.map(
                    "<http://purl.org/dc/terms/temporal>/<http://www.w3.org/ns/dcat#startDate>")),
        () -> assertEquals(rdfLength2Expected, prefixes.map("dcterms:temporal/dcat:startDate")),
        () ->
            assertEquals(
                rdfLength2Expected,
                prefixes.map("<http://purl.org/dc/terms/temporal>/dcat:startDate")),
        () ->
            assertEquals(
                rdfLength2Expected,
                prefixes.map("dcterms:temporal/<http://www.w3.org/ns/dcat#startDate>")),
        () -> assertThrows(MolgenisException.class, () -> prefixes.map(":temporal/dcat:startDate")),
        () -> assertThrows(MolgenisException.class, () -> prefixes.map("dcterms:/dcat:startDate")),
        () ->
            assertThrows(
                MolgenisException.class, () -> prefixes.map("dcterms:temporal/:startDate")),
        () -> assertThrows(MolgenisException.class, () -> prefixes.map("dcterms:temporal/dcat:")),
        () -> assertThrows(MolgenisException.class, () -> prefixes.map(":/dcat:startDate")),
        () -> assertThrows(MolgenisException.class, () -> prefixes.map("dcterms:temporal/:")),
        () -> assertThrows(MolgenisException.class, () -> prefixes.map("temporal/dcat:startDate")),
        () ->
            assertThrows(MolgenisException.class, () -> prefixes.map("dcterms:temporal/startDate")),
        () ->
            assertThrows(
                MolgenisException.class,
                () ->
                    prefixes.map(
                        "http://purl.org/dc/terms/temporal>/<http://www.w3.org/ns/dcat#startDate>")),
        () ->
            assertThrows(
                MolgenisException.class,
                () ->
                    prefixes.map(
                        "<http://purl.org/dc/terms/temporal/<http://www.w3.org/ns/dcat#startDate>")),
        () ->
            assertThrows(
                MolgenisException.class,
                () ->
                    prefixes.map(
                        "<http://purl.org/dc/terms/temporal>/http://www.w3.org/ns/dcat#startDate>")),
        () ->
            assertThrows(
                MolgenisException.class,
                () ->
                    prefixes.map(
                        "<http://purl.org/dc/terms/temporal>/<http://www.w3.org/ns/dcat#startDate")),
        () ->
            assertThrows(
                MolgenisException.class,
                () ->
                    prefixes.map(
                        "<http://purl.org/dc/terms/temporal><http://www.w3.org/ns/dcat#startDate>")),
        () ->
            assertThrows(
                MolgenisException.class,
                () -> prefixes.map("<http://purl.org/dc/terms/temporal>/startDate")),
        () ->
            assertThrows(
                MolgenisException.class,
                () -> prefixes.map("<http://purl.org/dc/terms/temporal/<startDate>")),

        // Semantic containing a sequence length of 3

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
