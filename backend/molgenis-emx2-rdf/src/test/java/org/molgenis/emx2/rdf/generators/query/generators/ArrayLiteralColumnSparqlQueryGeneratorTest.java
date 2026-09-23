package org.molgenis.emx2.rdf.generators.query.generators;

import static org.molgenis.emx2.rdf.generators.query.generators.SparqlQueryTestUtils.*;

import java.util.Map;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.rdf.generators.query.SparqlVariableUtil;
import org.molgenis.emx2.rdf.generators.query.TableQueryGenerator;

class ArrayLiteralColumnSparqlQueryGeneratorTest {

  private static final String IRI = "https://example.com/person";
  public static final TableQueryGenerator GENERATOR = new TableQueryGenerator();

  @Test
  void shouldConcatSelectors() {
    SailRepository repository =
        repository(
            SparqlQueryTestUtils.statement(IRI, FOAF.FIRST_NAME, "Lewis"),
            SparqlQueryTestUtils.statement(IRI, FOAF.FIRST_NAME, "Robin"),
            SparqlQueryTestUtils.statement(IRI, FOAF.FIRST_NAME, "Demetrius"));

    TableMetadata table =
        new SchemaMetadata()
            .create(
                TableMetadata.table(
                    "Person",
                    Column.column("name", ColumnType.STRING_ARRAY).setSemantics("foaf:firstName")));

    try (SailRepositoryConnection connection = repository.getConnection()) {
      String query = GENERATOR.generate(table);
      assertQueryEquals(
          """
          SELECT ?_subject_ ( GROUP_CONCAT( DISTINCT STR( ?name_single ) ; SEPARATOR = '|' ) AS ?name )
          WHERE { ?_subject_ ?anyPredicate ?anyObject .
          OPTIONAL { ?_subject_ foaf:firstName ?name_single . } }
          GROUP BY ?_subject_
          """,
          query);
      TupleQueryResult bindingSets = executeQuery(connection, query);
      assertHasResults(
          bindingSets,
          Map.of(SparqlVariableUtil.SUBJECT_NAME, IRI, "name", "Lewis|Robin|Demetrius"));
    }
  }

  @Test
  void shouldHandleNoSemantics() {
    SailRepository repository =
        repository(
            SparqlQueryTestUtils.statement(IRI, FOAF.FIRST_NAME, "Lewis"),
            SparqlQueryTestUtils.statement(IRI, FOAF.FIRST_NAME, "Robin"),
            SparqlQueryTestUtils.statement(IRI, FOAF.FIRST_NAME, "Demetrius"));

    TableMetadata table =
        new SchemaMetadata()
            .create(TableMetadata.table("Person", Column.column("name", ColumnType.STRING_ARRAY)));

    try (SailRepositoryConnection connection = repository.getConnection()) {
      String query = GENERATOR.generate(table);
      assertQueryEquals(
          """
          SELECT ?_subject_
          WHERE { ?_subject_ ?anyPredicate ?anyObject . }
          GROUP BY ?_subject_
          """,
          query);
      TupleQueryResult bindingSets = executeQuery(connection, query);
      assertHasResults(bindingSets, Map.of(SparqlVariableUtil.SUBJECT_NAME, IRI));
    }
  }

  @Test
  void givenMultipleSemantics_thenConcatBindValue() {
    SailRepository repository =
        repository(
            SparqlQueryTestUtils.statement(IRI, FOAF.FIRST_NAME, "Lewis"),
            SparqlQueryTestUtils.statement(IRI, FOAF.FIRST_NAME, "Robin"),
            SparqlQueryTestUtils.statement(IRI, FOAF.FIRST_NAME, "Demetrius"));

    TableMetadata table =
        new SchemaMetadata()
            .create(
                TableMetadata.table(
                    "Person",
                    Column.column("name", ColumnType.STRING_ARRAY)
                        .setSemantics("foaf:firstName", "foaf:givenName")));

    try (SailRepositoryConnection connection = repository.getConnection()) {
      String query = GENERATOR.generate(table);
      assertQueryEquals(
          """
          SELECT ?_subject_ ( GROUP_CONCAT( DISTINCT STR( ?name_single ) ; SEPARATOR = '|' ) AS ?name )
          WHERE { ?_subject_ ?anyPredicate ?anyObject .
          OPTIONAL { ?_subject_ foaf:firstName ?name_single0 . }
          OPTIONAL { ?_subject_ foaf:givenName ?name_single1 . }
          BIND( COALESCE( ?name_single0, ?name_single1 ) AS ?name_single ) }
          GROUP BY ?_subject_
          """,
          query);
      TupleQueryResult bindingSets = executeQuery(connection, query);
      assertHasResults(
          bindingSets,
          Map.of(SparqlVariableUtil.SUBJECT_NAME, IRI, "name", "Lewis|Robin|Demetrius"));
    }
  }

  @Disabled("This use case is currently a known limitation")
  @Test
  void givenMultipleSemantics_whenDifferentSemanticsUsedForSameCollection_thenCombine() {
    SailRepository repository =
        repository(
            SparqlQueryTestUtils.statement(IRI, FOAF.FIRST_NAME, "Lewis"),
            SparqlQueryTestUtils.statement(IRI, FOAF.GIVEN_NAME, "Robin"),
            SparqlQueryTestUtils.statement(IRI, FOAF.FIRST_NAME, "Demetrius"));

    TableMetadata table =
        new SchemaMetadata()
            .create(
                TableMetadata.table(
                    "Person",
                    Column.column("name", ColumnType.STRING_ARRAY)
                        .setSemantics("foaf:firstName", "foaf:givenName")));

    try (SailRepositoryConnection connection = repository.getConnection()) {
      TupleQueryResult bindingSets = executeQuery(connection, GENERATOR.generate(table));
      assertHasResults(
          bindingSets,
          Map.of(SparqlVariableUtil.SUBJECT_NAME, IRI, "name", "Lewis|Robin|Demetrius"));
    }
  }

  @Test
  void givenCollection_whenValueAppearsMultipleTimes_thenDistinct() {
    SailRepository repository =
        repository(
            SparqlQueryTestUtils.statement(IRI, FOAF.FIRST_NAME, "Lewis"),
            SparqlQueryTestUtils.statement(IRI, FOAF.FIRST_NAME, "Robin"),
            SparqlQueryTestUtils.statement(IRI, FOAF.FIRST_NAME, "Robin"));

    TableMetadata table =
        new SchemaMetadata()
            .create(
                TableMetadata.table(
                    "Person",
                    Column.column("name", ColumnType.STRING_ARRAY).setSemantics("foaf:firstName")));

    try (SailRepositoryConnection connection = repository.getConnection()) {
      String query = GENERATOR.generate(table);
      assertQueryEquals(
          """
          SELECT ?_subject_ ( GROUP_CONCAT( DISTINCT STR( ?name_single ) ; SEPARATOR = '|' ) AS ?name )
          WHERE { ?_subject_ ?anyPredicate ?anyObject .
          OPTIONAL { ?_subject_ foaf:firstName ?name_single . } }
          GROUP BY ?_subject_
          """,
          query);
      TupleQueryResult bindingSets = executeQuery(connection, query);
      assertHasResults(
          bindingSets, Map.of(SparqlVariableUtil.SUBJECT_NAME, IRI, "name", "Lewis|Robin"));
    }
  }
}
