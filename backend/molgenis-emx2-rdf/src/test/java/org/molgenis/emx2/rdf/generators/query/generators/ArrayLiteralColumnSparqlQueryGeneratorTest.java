package org.molgenis.emx2.rdf.generators.query.generators;

import static org.eclipse.rdf4j.model.util.Statements.statement;
import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.SemanticTestUtils.toSemantic;
import static org.molgenis.emx2.rdf.generators.MapperAssertions.*;

import java.util.List;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.DefaultNamespace;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;

class ArrayLiteralColumnSparqlQueryGeneratorTest {

  private static final Variable START = SparqlBuilder.var("start");
  private TableMetadata table;

  @BeforeEach
  void setUp() {
    table =
        new SchemaMetadata(getClass().getSimpleName()).create(new TableMetadata("arrayliterals"));
  }

  @Test
  void shouldConcatSelectors() {
    Column column = createColumn(Column.column("foo").setRequired(true).setSemantics("foaf:test"));
    ColumnSparqlQueryGenerator mapper = new ArrayColumnSparqlQueryGenerator(START, column);
    assertHasPatterns(mapper, "?start foaf:test ?foo_single .");
    assertHasSelectors(
        mapper, "( GROUP_CONCAT( DISTINCT STR( ?foo_single ) ; SEPARATOR = ',' ) AS ?foo )");
    assertHasGroupBy(mapper);
  }

  @Test
  void shouldHandleNoSemantics() {
    Column column = createColumn(Column.column("foo").setRequired(true).setSemantics());
    ColumnSparqlQueryGenerator mapper = new ArrayColumnSparqlQueryGenerator(START, column);
    assertTrue(mapper.getPatterns().isEmpty());
    assertHasSelectors(
        mapper, "( GROUP_CONCAT( DISTINCT STR( ?foo_single ) ; SEPARATOR = ',' ) AS ?foo )");
    assertHasGroupBy(mapper);
  }

  @Test
  void givenMultipleSemantics_thenConcatBindValue() {
    Column column =
        createColumn(
            Column.column("foo")
                .setRequired(true)
                .setSemantics(
                    "foaf:test",
                    "<https://xmlns.com/foaf/0.1/alternative>",
                    "foaf:also_alternative"));
    ColumnSparqlQueryGenerator mapper = new ArrayColumnSparqlQueryGenerator(START, column);

    assertHasPatterns(
        mapper,
        """
        OPTIONAL { OPTIONAL { ?start foaf:test ?foo_single0 . }
        OPTIONAL { ?start <https://xmlns.com/foaf/0.1/alternative> ?foo_single1 . }
        OPTIONAL { ?start foaf:also_alternative ?foo_single2 . }
        BIND( COALESCE( ?foo_single0, ?foo_single1, ?foo_single2 ) AS ?foo_single ) }""",
        "FILTER ( BOUND( ?foo_single ) )");
    assertHasSelectors(
        mapper, "( GROUP_CONCAT( DISTINCT STR( ?foo_single ) ; SEPARATOR = ',' ) AS ?foo )");
    assertHasGroupBy(mapper);
  }

  @Test
  void givenCollection_whenValueAppearsMultipleTimes_thenDistinct() {
    List<Column> columns =
        List.of(
            createColumn(
                Column.column("foo").setRequired(true).setSemantics(toSemantic(FOAF.FIRST_NAME))),
            createColumn(
                Column.column("bar").setRequired(true).setSemantics(toSemantic(FOAF.LAST_NAME))));

    SelectQuery query = Queries.SELECT().prefix(DefaultNamespace.FOAF.getNamespace());
    for (Column column : columns) {
      ArrayColumnSparqlQueryGenerator collectionColumnMapper =
          new ArrayColumnSparqlQueryGenerator(START, column);
      collectionColumnMapper.getSelectors().forEach(query::select);
      collectionColumnMapper.getGroupBy().forEach(query::groupBy);
      collectionColumnMapper.getPatterns().forEach(query::where);
    }

    SailRepository repository = new SailRepository(new MemoryStore());
    try (SailRepositoryConnection connection = repository.getConnection()) {
      addTripletToRepository(connection, FOAF.FIRST_NAME, "foo1");
      addTripletToRepository(connection, FOAF.FIRST_NAME, "foo2");
      addTripletToRepository(connection, FOAF.LAST_NAME, "bar1");
      addTripletToRepository(connection, FOAF.LAST_NAME, "bar2");
      connection.commit();

      try (TupleQueryResult queryResult =
          connection.prepareTupleQuery(QueryLanguage.SPARQL, query.getQueryString()).evaluate()) {
        BindingSet binding = queryResult.next();
        assertEquals(literal("foo1,foo2"), binding.getValue("foo"));
        assertEquals(literal("bar1,bar2"), binding.getValue("bar"));
        assertFalse(queryResult.hasNext());
      }
    }
  }

  private void addTripletToRepository(
      SailRepositoryConnection connection, IRI predicate, String object) {
    connection.add(statement(iri("https://example.com/bob"), predicate, literal(object), null));
  }

  private Column createColumn(Column column) {
    table.add(column);
    return table.getColumn(column.getName());
  }
}
