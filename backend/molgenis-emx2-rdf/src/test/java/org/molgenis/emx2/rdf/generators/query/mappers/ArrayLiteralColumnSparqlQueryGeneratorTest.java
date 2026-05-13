package org.molgenis.emx2.rdf.generators.query.mappers;

import static org.eclipse.rdf4j.model.util.Statements.statement;
import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.molgenis.emx2.rdf.generators.query.mappers.MapperAssertions.*;

import java.util.List;
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
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.rdf.DefaultNamespace;

class ArrayLiteralColumnSparqlQueryGeneratorTest {

  private static final Variable START = SparqlBuilder.var("start");

  @Test
  void shouldConcatSelectors() {
    Column column = Column.column("foo").setRequired(true).setSemantics("foaf:test");
    SparqlQueryGenerator mapper = new ArrayColumnSparqlQueryGenerator(START, column);
    assertHasPatterns(mapper, "?start foaf:test ?foo_single .");
    assertHasSelectors(
        mapper, "( GROUP_CONCAT( DISTINCT STR( ?foo_single ) ; SEPARATOR = ',' ) AS ?foo )");
    assertHasGroupBy(mapper);
  }

  @Test
  void givenMultipleSemantics_thenConcatBindValue() {
    Column column =
        Column.column("foo")
            .setRequired(true)
            .setSemantics("foaf:test", "foaf:alternative", "foaf:also_alternative");
    SparqlQueryGenerator mapper = new ArrayColumnSparqlQueryGenerator(START, column);

    assertHasPatterns(
        mapper,
        """
        OPTIONAL { OPTIONAL { ?start foaf:test ?foo_single0 . }
        OPTIONAL { ?start foaf:alternative ?foo_single1 . }
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
            Column.column("foo").setRequired(true).setSemantics("foaf:foo"),
            Column.column("bar").setRequired(true).setSemantics("foaf:bar"));

    SelectQuery query = Queries.SELECT().prefix(DefaultNamespace.FOAF.getNamespace());
    for (Column column : columns) {
      ArrayColumnSparqlQueryGenerator collectionColumnMapper = new ArrayColumnSparqlQueryGenerator(START, column);
      collectionColumnMapper.getSelectors().forEach(query::select);
      collectionColumnMapper.getGroupBy().forEach(query::groupBy);
      collectionColumnMapper.getPatterns().forEach(query::where);
    }

    SailRepository repository = new SailRepository(new MemoryStore());
    try (SailRepositoryConnection connection = repository.getConnection()) {
      addTripletToRepository(connection, "foo", "foo1");
      addTripletToRepository(connection, "foo", "foo2");
      addTripletToRepository(connection, "bar", "bar1");
      addTripletToRepository(connection, "bar", "bar2");
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
      SailRepositoryConnection connection, String predicate, String object) {
    connection.add(
        statement(
            iri(DefaultNamespace.FOAF.resolve("bob")),
            iri(DefaultNamespace.FOAF.resolve(predicate)),
            literal(object),
            null));
  }
}
