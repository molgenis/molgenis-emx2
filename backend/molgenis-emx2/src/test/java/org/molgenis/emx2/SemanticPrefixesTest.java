package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;

class SemanticPrefixesTest {
  SemanticPrefixes prefixes =
      new SemanticPrefixes(
          Values.namespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
          Values.namespace("dcat", "http://www.w3.org/ns/dcat#"),
          Values.namespace("dcterms", "http://purl.org/dc/terms/"),
          Values.namespace("time", "http://www.w3.org/2006/time#"),
          Values.namespace("http", "http://example.com/fromPrefix#"));

  @Test
  void testSemanticMapping() {
    SemanticPrefixes prefixesLegacy =
        new SemanticPrefixes(
            Values.namespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
            Values.namespace("dcat", "http://www.w3.org/ns/dcat#"),
            Values.namespace("dcterms", "http://purl.org/dc/terms/"),
            Values.namespace("time", "http://www.w3.org/2006/time#"));

    List<IRI> rdfExpected = List.of(Values.iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));
    List<IRI> httpIriExpected = List.of(Values.iri("http://example.com/fromIri#test"));
    List<IRI> httpPrefixExpected = List.of(Values.iri("http://example.com/fromPrefix#test"));

    List<IRI> rdfLength2Expected =
        List.of(
            Values.iri("http://purl.org/dc/terms/temporal"),
            Values.iri("http://www.w3.org/ns/dcat#startDate"));

    List<IRI> rdfLength3Expected =
        List.of(
            Values.iri("http://purl.org/dc/terms/temporal"),
            Values.iri("http://www.w3.org/2006/time#hasBeginning"),
            Values.iri("http://www.w3.org/2006/time#inXSDDate"));

    assertAll(
        // Backwards compatibility (no http(s)/urn/tag can be defined as semantic prefix as this
        // disables backwards compatibility due overlap with prefixed names)
        () ->
            assertEquals(
                rdfExpected, prefixesLegacy.map("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")),
        () -> assertEquals(httpIriExpected, prefixesLegacy.map("http://example.com/fromIri#test")),

        // Length 1: valid
        () ->
            assertEquals(
                rdfExpected, prefixes.map("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>")),
        () -> assertEquals(rdfExpected, prefixes.map("rdf:type")),
        () -> assertEquals(httpIriExpected, prefixes.map("<http://example.com/fromIri#test>")),
        () -> assertEquals(httpPrefixExpected, prefixes.map("http:test")),

        // Length 1: invalid legacy IRI
        () -> assertThrows(MolgenisException.class, () -> prefixes.map("http://invalid")),
        // Length 1: invalid IRI
        () -> assertThrows(MolgenisException.class, () -> prefixes.map("<invalid>")),
        // Length 1: invalid prefixed name
        () -> assertThrows(MolgenisException.class, () -> prefixes.map("undefinedPrefix:test")),
        () -> assertThrows(MolgenisException.class, () -> prefixes.map("rdf:")),
        () -> assertThrows(MolgenisException.class, () -> prefixes.map(":test")),
        () -> assertThrows(MolgenisException.class, () -> prefixes.map("test")),
        () -> assertThrows(MolgenisException.class, () -> prefixes.map(":")),

        // Length 2: valid
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
        // Length 2: incorrect prefixed name
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
        // Length 2: incorrect IRI
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

        // Length 3: valid
        () ->
            assertEquals(
                rdfLength3Expected,
                prefixes.map(
                    "<http://purl.org/dc/terms/temporal>/time:hasBeginning/<http://www.w3.org/2006/time#inXSDDate>")),
        () ->
            assertEquals(
                rdfLength3Expected,
                prefixes.map(
                    "dcterms:temporal/<http://www.w3.org/2006/time#hasBeginning>/time:inXSDDate")),
        () ->
            assertEquals(
                rdfLength3Expected,
                prefixes.map("dcterms:temporal/time:hasBeginning/time:inXSDDate")),
        () ->
            assertEquals(
                rdfLength3Expected,
                prefixes.map(
                    "<http://purl.org/dc/terms/temporal>/<http://www.w3.org/2006/time#hasBeginning>/<http://www.w3.org/2006/time#inXSDDate>")));
  }

  @Test
  void testSemanticMappingString() {
    List<String> expected = List.of("<http://purl.org/dc/terms/temporal>", "dcat:startDate");
    assertEquals(
        expected, prefixes.mapAsStrings("<http://purl.org/dc/terms/temporal>/dcat:startDate"));
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
