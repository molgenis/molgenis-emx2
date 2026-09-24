package org.molgenis.emx2.rdf.generators.query.generators;

import static org.molgenis.emx2.rdf.generators.query.generators.SparqlQueryTestUtils.*;

import java.util.Map;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.rdf.generators.query.SparqlVariableUtil;

class RefbackColumnSparqlQueryGeneratorTest {

  private static final String SCHEMA = RefbackColumnSparqlQueryGeneratorTest.class.getSimpleName();

  private static final String PERSON_IRI = "https://example.com/person";
  private static final String PET_IRI = "https://example.com/pet";

  @Test
  void shouldWrapPatternInOptional_whenRefbackColumnNotRequired() {
    SchemaMetadata schema =
        new SchemaMetadata(SCHEMA)
            .create(
                personTable("owner", "dcterms:relation"), petTable("owner", ColumnType.REF, false));

    assertQueryAndResults(
        schema.getTableMetadata("Pet"),
        defaultRepository(),
        """
        SELECT ?_subject_ ?name ( GROUP_CONCAT( DISTINCT STR( ?_subject_owner_single ) ; SEPARATOR = '|' ) AS ?_subject_owner )
        WHERE { ?_subject_ ?anyPredicate ?anyObject .
        ?_subject_ foaf:title ?name .
        OPTIONAL { ?_subject_ ^dcterms:relation ?_subject_owner_single . } }
        GROUP BY ?_subject_ ?name
        """,
        Map.of(
            SparqlVariableUtil.SUBJECT_NAME,
            PET_IRI,
            "name",
            "Pip",
            SparqlVariableUtil.SUBJECT_NAME + "owner",
            PERSON_IRI));
  }

  @Test
  void shouldNotWrapPatternInOptional_whenRefbackColumnRequired() {
    SchemaMetadata schema =
        new SchemaMetadata(SCHEMA)
            .create(
                personTable("owner", "dcterms:relation"), petTable("owner", ColumnType.REF, true));

    assertQueryAndResults(
        schema.getTableMetadata("Pet"),
        defaultRepository(),
        """
        SELECT ?_subject_ ?name ( GROUP_CONCAT( DISTINCT STR( ?_subject_owner_single ) ; SEPARATOR = '|' ) AS ?_subject_owner )
        WHERE { ?_subject_ ?anyPredicate ?anyObject .
        ?_subject_ foaf:title ?name .
        ?_subject_ ^dcterms:relation ?_subject_owner_single . }
        GROUP BY ?_subject_ ?name
        """,
        Map.of(
            SparqlVariableUtil.SUBJECT_NAME,
            PET_IRI,
            "name",
            "Pip",
            SparqlVariableUtil.SUBJECT_NAME + "owner",
            PERSON_IRI));
  }

  @Test
  void shouldReturnEmptyPatterns_whenOwningColumnHasNoSemantics() {
    SailRepository repository =
        repository(
            statement(PERSON_IRI, FOAF.FIRST_NAME, "Bau"), statement(PET_IRI, FOAF.TITLE, "Pip"));

    SchemaMetadata schema =
        new SchemaMetadata(SCHEMA)
            .create(personTable("owner"), petTable("owner", ColumnType.REF, true));

    assertQueryAndResults(
        schema.getTableMetadata("Pet"),
        repository,
        """
        SELECT ?_subject_ ?name
        WHERE { ?_subject_ ?anyPredicate ?anyObject .
        ?_subject_ foaf:title ?name . }
        GROUP BY ?_subject_ ?name
        """,
        Map.of(SparqlVariableUtil.SUBJECT_NAME, PET_IRI, "name", "Pip"));
  }

  @Test
  void shouldNormalizeRefbackColumnName() {
    SchemaMetadata schema =
        new SchemaMetadata(SCHEMA)
            .create(
                personTable("owner with space", "dcterms:relation"),
                petTable("owner with space", ColumnType.REF, false));

    assertQueryAndResults(
        schema.getTableMetadata("Pet"),
        defaultRepository(),
        """
        SELECT ?_subject_ ?name ( GROUP_CONCAT( DISTINCT STR( ?_subject_owner___with___space_single ) ; SEPARATOR = '|' ) AS ?_subject_owner___with___space )
        WHERE { ?_subject_ ?anyPredicate ?anyObject .
        ?_subject_ foaf:title ?name .
        OPTIONAL { ?_subject_ ^dcterms:relation ?_subject_owner___with___space_single . } }
        GROUP BY ?_subject_ ?name
        """,
        Map.of(
            SparqlVariableUtil.SUBJECT_NAME,
            PET_IRI,
            "name",
            "Pip",
            SparqlVariableUtil.SUBJECT_NAME + "owner___with___space",
            PERSON_IRI));
  }

  @Test
  void givenOwningColumnWithMultipleSemantics_thenCoalesce() {
    SailRepository repository =
        repository(
            statement(PERSON_IRI, FOAF.FIRST_NAME, "Bau"),
            statement(PERSON_IRI, DCTERMS.ALTERNATIVE, Values.iri(PET_IRI)),
            statement(PET_IRI, FOAF.TITLE, "Pip"));

    SchemaMetadata schema =
        new SchemaMetadata(SCHEMA)
            .create(
                personTable("owner", "dcterms:relation", "dcterms:alternative"),
                petTable("owner", ColumnType.REF, false));

    assertQueryAndResults(
        schema.getTableMetadata("Pet"),
        repository,
        """
        SELECT ?_subject_ ?name ( GROUP_CONCAT( DISTINCT STR( ?_subject_owner_single ) ; SEPARATOR = '|' ) AS ?_subject_owner )
        WHERE { ?_subject_ ?anyPredicate ?anyObject .
        ?_subject_ foaf:title ?name .
        OPTIONAL { ?_subject_ ^dcterms:relation ?_subject_owner_single0 . }
        OPTIONAL { ?_subject_ ^dcterms:alternative ?_subject_owner_single1 . }
        BIND( COALESCE( ?_subject_owner_single0, ?_subject_owner_single1 ) AS ?_subject_owner_single ) }
        GROUP BY ?_subject_ ?name
        """,
        Map.of(
            SparqlVariableUtil.SUBJECT_NAME,
            PET_IRI,
            "name",
            "Pip",
            SparqlVariableUtil.SUBJECT_NAME + "owner",
            PERSON_IRI));
  }

  @Test
  void givenRefArray_whenMultipleReferences_thenConcat() {
    SailRepository repository =
        repository(
            statement(PERSON_IRI, FOAF.FIRST_NAME, "Bau"),
            statement(PERSON_IRI, DCTERMS.RELATION, Values.iri(PET_IRI)),
            statement(PERSON_IRI + 2, DCTERMS.RELATION, Values.iri(PET_IRI)),
            statement(PET_IRI, FOAF.TITLE, "Pip"));

    SchemaMetadata schema =
        new SchemaMetadata(SCHEMA)
            .create(
                personTable("owner", "dcterms:relation"),
                petTable("owner", ColumnType.REF_ARRAY, false));

    assertQueryAndResults(
        schema.getTableMetadata("Pet"),
        repository,
        """
        SELECT ?_subject_ ?name ( GROUP_CONCAT( DISTINCT STR( ?_subject_owner_single ) ; SEPARATOR = '|' ) AS ?_subject_owner )
        WHERE { ?_subject_ ?anyPredicate ?anyObject .
        ?_subject_ foaf:title ?name .
        OPTIONAL { ?_subject_ ^dcterms:relation ?_subject_owner_single . } }
        GROUP BY ?_subject_ ?name
        """,
        Map.of(
            SparqlVariableUtil.SUBJECT_NAME,
            PET_IRI,
            "name",
            "Pip",
            SparqlVariableUtil.SUBJECT_NAME + "owner",
            PERSON_IRI + "|" + PERSON_IRI + 2));
  }

  private SailRepository defaultRepository() {
    return repository(
        statement(PERSON_IRI, FOAF.FIRST_NAME, "Bau"),
        statement(PERSON_IRI, DCTERMS.RELATION, Values.iri(PET_IRI)),
        statement(PET_IRI, FOAF.TITLE, "Pip"));
  }

  private TableMetadata personTable(String refBackColumnName, String... refbackSemantics) {
    return TableMetadata.table(
        "Person",
        Column.column("name", ColumnType.STRING).setSemantics("foaf:firstName").setPkey(),
        Column.column("pets", ColumnType.REFBACK)
            .setRefTable("Pet")
            .setRefBack(refBackColumnName)
            .setSemantics(refbackSemantics));
  }

  private TableMetadata petTable(String ownerColumnName, ColumnType ownerType, boolean required) {
    return TableMetadata.table(
        "Pet",
        Column.column("name", ColumnType.STRING).setSemantics("foaf:title").setPkey(),
        Column.column(ownerColumnName, ownerType).setRefTable("Person").setRequired(required));
  }
}
