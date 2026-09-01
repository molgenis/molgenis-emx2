package org.molgenis.emx2.rdf.generators.query.generators;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.rdf.generators.MapperAssertions.*;

import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;

class RefbackColumnSparqlQueryGeneratorTest {

  private static final Variable SUBJECT = SparqlBuilder.var("pet");
  private TableMetadata table;

  @BeforeEach
  void setUp() {
    table = new SchemaMetadata(getClass().getSimpleName()).create(new TableMetadata("Pet"));
  }

  @Test
  void shouldWrapPatternInOptional_whenRefbackColumnNotRequired() {
    Column owningColumn = createColumn(Column.column("pet").setSemantics("foaf:pet"));
    Column refbackColumn = createColumn(Column.column("owner").setRequired(false));

    RefbackColumnSparqlQueryGenerator mapper =
        new RefbackColumnSparqlQueryGenerator(SUBJECT, owningColumn, refbackColumn);

    assertHasPatterns(mapper, "OPTIONAL { ?pet ^foaf:pet ?_subject_owner_single . }");
    assertHasSelectors(
        mapper,
        "( GROUP_CONCAT( DISTINCT STR( ?_subject_owner_single ) ; SEPARATOR = '|' ) AS ?_subject_owner )");
    assertHasGroupBy(mapper);
  }

  @Test
  void shouldNotWrapPatternInOptional_whenRefbackColumnRequired() {
    Column owningColumn = createColumn(Column.column("pet").setSemantics("foaf:pet"));
    Column refbackColumn = createColumn(Column.column("owner").setRequired(true));

    RefbackColumnSparqlQueryGenerator mapper =
        new RefbackColumnSparqlQueryGenerator(SUBJECT, owningColumn, refbackColumn);

    assertHasPatterns(mapper, "?pet ^foaf:pet ?_subject_owner_single .");
    assertHasSelectors(
        mapper,
        "( GROUP_CONCAT( DISTINCT STR( ?_subject_owner_single ) ; SEPARATOR = '|' ) AS ?_subject_owner )");
    assertHasGroupBy(mapper);
  }

  @Test
  void shouldReturnEmptyPatterns_whenOwningColumnHasNoSemantics() {
    Column owningColumn = createColumn(Column.column("pet").setSemantics());
    Column refbackColumn = createColumn(Column.column("owner").setRequired(false));

    RefbackColumnSparqlQueryGenerator mapper =
        new RefbackColumnSparqlQueryGenerator(SUBJECT, owningColumn, refbackColumn);

    assertTrue(mapper.getPatterns().isEmpty());
    assertHasSelectors(
        mapper,
        "( GROUP_CONCAT( DISTINCT STR( ?_subject_owner_single ) ; SEPARATOR = '|' ) AS ?_subject_owner )");
    assertHasGroupBy(mapper);
  }

  @Test
  void shouldNormalizeRefbackColumnName() {
    Column owningColumn = createColumn(Column.column("pet").setSemantics("foaf:pet"));
    Column refbackColumn = createColumn(Column.column("owner extra").setRequired(false));

    RefbackColumnSparqlQueryGenerator mapper =
        new RefbackColumnSparqlQueryGenerator(SUBJECT, owningColumn, refbackColumn);

    assertHasPatterns(mapper, "OPTIONAL { ?pet ^foaf:pet ?_subject_owner___extra_single . }");
    assertHasSelectors(
        mapper,
        "( GROUP_CONCAT( DISTINCT STR( ?_subject_owner___extra_single ) ; SEPARATOR = '|' ) AS ?_subject_owner___extra )");
    assertHasGroupBy(mapper);
  }

  @Test
  void givenOwningColumnWithMultipleSemantics_thenCoalesce() {
    Column owningColumn =
        createColumn(Column.column("pet").setSemantics("foaf:pet", "foaf:pet_alt"));
    Column refbackColumn = createColumn(Column.column("owner").setRequired(false));

    RefbackColumnSparqlQueryGenerator mapper =
        new RefbackColumnSparqlQueryGenerator(SUBJECT, owningColumn, refbackColumn);

    assertHasPatterns(
        mapper,
        """
        OPTIONAL { OPTIONAL { ?pet ^foaf:pet ?_subject_owner_single0 . }
        OPTIONAL { ?pet ^foaf:pet_alt ?_subject_owner_single1 . }
        BIND( COALESCE( ?_subject_owner_single0, ?_subject_owner_single1 ) AS ?_subject_owner_single ) }""");
    assertHasSelectors(
        mapper,
        "( GROUP_CONCAT( DISTINCT STR( ?_subject_owner_single ) ; SEPARATOR = '|' ) AS ?_subject_owner )");
    assertHasGroupBy(mapper);
  }

  private Column createColumn(Column column) {
    return table.add(column).getColumn(column.getName());
  }
}
