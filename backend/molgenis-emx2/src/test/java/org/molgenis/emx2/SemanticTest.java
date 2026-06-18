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
  void testSemanticStringToSequencePath() {
    assertAll(
        // Length 1: valid
        () ->
            assertEquals(
                List.of("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>"),
                new Semantic("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>")
                    .getSequencePath()),
        () -> assertEquals(List.of("rdf:type"), new Semantic("rdf:type").getSequencePath()),
        () -> assertEquals(List.of(":test"), new Semantic(":test").getSequencePath()),

        // Length 1: invalid IRI
        () -> assertThrows(MolgenisException.class, () -> new Semantic("<invalid>")),
        // Length 1: invalid prefixed name
        () -> assertThrows(MolgenisException.class, () -> new Semantic("rdf:")),
        () -> assertThrows(MolgenisException.class, () -> new Semantic("test")),
        () -> assertThrows(MolgenisException.class, () -> new Semantic(":")),

        // Length 2: valid
        () ->
            assertEquals(
                List.of(
                    "<http://purl.org/dc/terms/temporal>", "<http://www.w3.org/ns/dcat#startDate>"),
                new Semantic(
                        "<http://purl.org/dc/terms/temporal>/<http://www.w3.org/ns/dcat#startDate>")
                    .getSequencePath()),
        () ->
            assertEquals(
                List.of("dcterms:temporal", "dcat:startDate"),
                new Semantic("dcterms:temporal/dcat:startDate").getSequencePath()),
        () ->
            assertEquals(
                List.of("<http://purl.org/dc/terms/temporal>", "dcat:startDate"),
                new Semantic("<http://purl.org/dc/terms/temporal>/dcat:startDate")
                    .getSequencePath()),
        () ->
            assertEquals(
                List.of("dcterms:temporal", "<http://www.w3.org/ns/dcat#startDate>"),
                new Semantic("dcterms:temporal/<http://www.w3.org/ns/dcat#startDate>")
                    .getSequencePath()),
        // Length 2: valid (with empty prefix)
        () ->
            assertEquals(
                List.of(":temporal", "dcat:startDate"),
                new Semantic(":temporal/dcat:startDate").getSequencePath()),
        () ->
            assertEquals(
                List.of("dcterms:temporal", ":startDate"),
                new Semantic("dcterms:temporal/:startDate").getSequencePath()),
        // Length 2: incorrect prefixed name
        () -> assertThrows(MolgenisException.class, () -> new Semantic("dcterms:/dcat:startDate")),
        () -> assertThrows(MolgenisException.class, () -> new Semantic("dcterms:temporal/dcat:")),
        () -> assertThrows(MolgenisException.class, () -> new Semantic(":/dcat:startDate")),
        () -> assertThrows(MolgenisException.class, () -> new Semantic("dcterms:temporal/:")),
        () -> assertThrows(MolgenisException.class, () -> new Semantic("temporal/dcat:startDate")),
        () ->
            assertThrows(MolgenisException.class, () -> new Semantic("dcterms:temporal/startDate")),
        // Length 2: incorrect IRI
        () ->
            assertThrows(
                MolgenisException.class,
                () ->
                    new Semantic(
                        "http://purl.org/dc/terms/temporal>/<http://www.w3.org/ns/dcat#startDate>")),
        () ->
            assertThrows(
                MolgenisException.class,
                () ->
                    new Semantic(
                        "<http://purl.org/dc/terms/temporal/<http://www.w3.org/ns/dcat#startDate>")),
        () ->
            assertThrows(
                MolgenisException.class,
                () ->
                    new Semantic(
                        "<http://purl.org/dc/terms/temporal>/http://www.w3.org/ns/dcat#startDate>")),
        () ->
            assertThrows(
                MolgenisException.class,
                () ->
                    new Semantic(
                        "<http://purl.org/dc/terms/temporal>/<http://www.w3.org/ns/dcat#startDate")),
        () ->
            assertThrows(
                MolgenisException.class,
                () ->
                    new Semantic(
                        "<http://purl.org/dc/terms/temporal><http://www.w3.org/ns/dcat#startDate>")),
        () ->
            assertThrows(
                MolgenisException.class,
                () -> new Semantic("<http://purl.org/dc/terms/temporal>/startDate")),

        // Length 3: valid
        () ->
            assertEquals(
                List.of(
                    "<http://purl.org/dc/terms/temporal>",
                    "time:hasBeginning",
                    "<http://www.w3.org/2006/time#inXSDDate>"),
                new Semantic(
                        "<http://purl.org/dc/terms/temporal>/time:hasBeginning/<http://www.w3.org/2006/time#inXSDDate>")
                    .getSequencePath()),
        () ->
            assertEquals(
                List.of(
                    "dcterms:temporal",
                    "<http://www.w3.org/2006/time#hasBeginning>",
                    "time:inXSDDate"),
                new Semantic(
                        "dcterms:temporal/<http://www.w3.org/2006/time#hasBeginning>/time:inXSDDate")
                    .getSequencePath()),
        () ->
            assertEquals(
                List.of("dcterms:temporal", "time:hasBeginning", "time:inXSDDate"),
                new Semantic("dcterms:temporal/time:hasBeginning/time:inXSDDate")
                    .getSequencePath()),
        () ->
            assertEquals(
                List.of(
                    "<http://purl.org/dc/terms/temporal>",
                    "<http://www.w3.org/2006/time#hasBeginning>",
                    "<http://www.w3.org/2006/time#inXSDDate>"),
                new Semantic(
                        "<http://purl.org/dc/terms/temporal>/<http://www.w3.org/2006/time#hasBeginning>/<http://www.w3.org/2006/time#inXSDDate>")
                    .getSequencePath()));
  }
}
