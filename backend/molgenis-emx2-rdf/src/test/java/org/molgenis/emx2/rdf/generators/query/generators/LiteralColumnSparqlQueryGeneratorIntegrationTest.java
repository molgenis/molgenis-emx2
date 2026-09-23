package org.molgenis.emx2.rdf.generators.query.generators;

import static org.molgenis.emx2.rdf.generators.MapperAssertions.*;
import static org.molgenis.emx2.rdf.generators.query.generators.SparqlQueryTestUtils.*;

import java.util.Map;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.Semantic;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.rdf.generators.query.SparqlVariableUtil;
import org.molgenis.emx2.rdf.generators.query.TableQueryGenerator;

class LiteralColumnSparqlQueryGeneratorIntegrationTest {

  private static final String IRI = "https://example.com/person";
  public static final TableQueryGenerator GENERATOR = new TableQueryGenerator();

  @Test
  void shouldMapColumn() {
    SailRepository repository =
        repository(
            statement("https://example.com/person", FOAF.FIRST_NAME, "Bau"),
            statement("https://example.com/person", FOAF.LAST_NAME, "Terham"));

    TableMetadata table =
        new SchemaMetadata()
            .create(
                TableMetadata.table(
                    "Person", Column.column("name").setSemantics("foaf:firstName")));

    try (SailRepositoryConnection connection = repository.getConnection()) {
      TupleQueryResult bindingSets = executeQuery(connection, GENERATOR.generate(table));
      assertHasResults(bindingSets, Map.of(SparqlVariableUtil.SUBJECT_NAME, IRI, "name", "Bau"));
    }
  }

  @Test
  void shouldMapOptional() {
    String iri1 = IRI + 1;
    String iri2 = IRI + 2;

    SailRepository repository =
        repository(
            statement(iri1, FOAF.FIRST_NAME, "Bau"),
            statement(iri1, FOAF.LAST_NAME, "Terham"),
            statement(iri2, FOAF.FIRST_NAME, "Lewis"));

    TableMetadata table =
        new SchemaMetadata()
            .create(
                TableMetadata.table(
                    "Person",
                    Column.column("lastName").setSemantics("foaf:lastName").setRequired(false)));

    try (SailRepositoryConnection connection = repository.getConnection()) {
      TupleQueryResult bindingSets = executeQuery(connection, GENERATOR.generate(table));
      assertHasResults(
          bindingSets,
          Map.of(SparqlVariableUtil.SUBJECT_NAME, iri2),
          Map.of(SparqlVariableUtil.SUBJECT_NAME, iri1, "lastName", "Terham"));
    }
  }

  @Test
  void shouldHandleNoSemanticsForColumn() {
    String iri1 = IRI + 1;
    SailRepository repository = repository(statement(iri1, FOAF.FIRST_NAME, "Bau"));

    TableMetadata table =
        new SchemaMetadata().create(TableMetadata.table("Person", Column.column("name")));

    try (SailRepositoryConnection connection = repository.getConnection()) {
      TupleQueryResult bindingSets = executeQuery(connection, GENERATOR.generate(table));
      assertHasResults(bindingSets, Map.of(SparqlVariableUtil.SUBJECT_NAME, iri1));
    }
  }

  @Test
  void shouldHandleNullSemanticsForColumn() {
    String iri1 = IRI + 1;
    SailRepository repository = repository(statement(iri1, FOAF.FIRST_NAME, "Bau"));

    TableMetadata table =
        new SchemaMetadata()
            .create(
                TableMetadata.table(
                    "Person", Column.column("name").setSemantics((Semantic[]) null)));

    try (SailRepositoryConnection connection = repository.getConnection()) {
      TupleQueryResult bindingSets = executeQuery(connection, GENERATOR.generate(table));
      assertHasResults(bindingSets, Map.of(SparqlVariableUtil.SUBJECT_NAME, iri1));
    }
  }

  @Test
  void shouldMapMultipleSemantic() {
    String iri1 = IRI + 1;
    String iri2 = IRI + 2;
    String iri3 = IRI + 3;
    String iri4 = IRI + 4;

    SailRepository repository =
        repository(
            statement(iri1, FOAF.FIRST_NAME, "Lewis"),
            statement(iri2, FOAF.GIVEN_NAME, "Robin"),
            statement(iri3, FOAF.FIRST_NAME, "Demetrius"),
            statement(iri3, FOAF.GIVEN_NAME, "Also Demetrius"),
            statement(iri4, FOAF.LAST_NAME, "Terham"));

    TableMetadata table =
        new SchemaMetadata()
            .create(
                TableMetadata.table(
                    "Person",
                    Column.column("name").setSemantics("foaf:firstName", "foaf:givenName")));

    try (SailRepositoryConnection connection = repository.getConnection()) {
      TupleQueryResult bindingSets = executeQuery(connection, GENERATOR.generate(table));
      assertHasResults(
          bindingSets,
          Map.of(SparqlVariableUtil.SUBJECT_NAME, iri2, "name", "Robin"),
          Map.of(SparqlVariableUtil.SUBJECT_NAME, iri3, "name", "Demetrius"),
          Map.of(SparqlVariableUtil.SUBJECT_NAME, iri4),
          Map.of(SparqlVariableUtil.SUBJECT_NAME, iri1, "name", "Lewis"));
    }
  }

  @Test
  void shouldMapMultipleSemanticWithFilter() {
    String iri1 = IRI + 1;
    String iri2 = IRI + 2;
    String iri3 = IRI + 3;
    String iri4 = IRI + 4;

    SailRepository repository =
        repository(
            statement(iri1, FOAF.FIRST_NAME, "Lewis"),
            statement(iri2, FOAF.GIVEN_NAME, "Robin"),
            statement(iri3, FOAF.FIRST_NAME, "Demetrius"),
            statement(iri3, FOAF.GIVEN_NAME, "Also Demetrius"),
            statement(iri4, FOAF.LAST_NAME, "Terham"));

    TableMetadata table =
        new SchemaMetadata()
            .create(
                TableMetadata.table(
                    "Person",
                    Column.column("name")
                        .setSemantics("foaf:firstName", "foaf:givenName")
                        .setRequired(true)));

    try (SailRepositoryConnection connection = repository.getConnection()) {
      TupleQueryResult bindingSets = executeQuery(connection, GENERATOR.generate(table));
      assertHasResults(
          bindingSets,
          Map.of(SparqlVariableUtil.SUBJECT_NAME, iri2, "name", "Robin"),
          Map.of(SparqlVariableUtil.SUBJECT_NAME, iri3, "name", "Demetrius"),
          Map.of(SparqlVariableUtil.SUBJECT_NAME, iri1, "name", "Lewis"));
    }
  }

  @Test
  void shouldNormalizeColumnName() {
    SailRepository repository =
        repository(statement("https://example.com/person", FOAF.FIRST_NAME, "Bau"));

    TableMetadata table =
        new SchemaMetadata()
            .create(
                TableMetadata.table(
                    "Person", Column.column("foo bar").setSemantics("foaf:firstName")));

    try (SailRepositoryConnection connection = repository.getConnection()) {
      TupleQueryResult bindingSets = executeQuery(connection, GENERATOR.generate(table));
      assertHasResults(
          bindingSets, Map.of(SparqlVariableUtil.SUBJECT_NAME, IRI, "foo___bar", "Bau"));
    }
  }

  @Test
  void givenColumn_whenSemanticIsIRI_thenSurroundWithPointBrackets() {
    SailRepository repository =
        repository(
            statement(
                "https://example.com/person", Values.iri("https://example.org/ns#test"), "Bau"));

    TableMetadata table =
        new SchemaMetadata()
            .create(
                TableMetadata.table(
                    "Person",
                    Column.column("foo bar").setSemantics("https://example.org/ns#test")));

    try (SailRepositoryConnection connection = repository.getConnection()) {
      TupleQueryResult bindingSets = executeQuery(connection, GENERATOR.generate(table));
      assertHasResults(
          bindingSets, Map.of(SparqlVariableUtil.SUBJECT_NAME, IRI, "foo___bar", "Bau"));
    }
  }

  @Test
  void givenColumnWithMultipleSemantics_whenSemanticIsIRI_thenSurroundWithPointyBrackets() {
    String iri1 = IRI + 1;
    String iri2 = IRI + 2;

    SailRepository repository =
        repository(
            statement(iri1, FOAF.FIRST_NAME, "Lewis"),
            statement(iri2, Values.iri("https://example.org/ns#test"), "Robin"));

    TableMetadata table =
        new SchemaMetadata()
            .create(
                TableMetadata.table(
                    "Person",
                    Column.column("name")
                        .setSemantics("foaf:firstName", "https://example.org/ns#test")));

    try (SailRepositoryConnection connection = repository.getConnection()) {
      TupleQueryResult bindingSets = executeQuery(connection, GENERATOR.generate(table));
      assertHasResults(
          bindingSets,
          Map.of(SparqlVariableUtil.SUBJECT_NAME, iri1, "name", "Lewis"),
          Map.of(SparqlVariableUtil.SUBJECT_NAME, iri2, "name", "Robin"));
    }
  }
}
