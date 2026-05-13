package org.molgenis.emx2.rdf.generators.query.mappers;

import static org.molgenis.emx2.rdf.generators.query.mappers.MapperAssertions.*;

import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;

class LiteralColumnSparqlQueryGeneratorTest {

  private static final Variable START = SparqlBuilder.var("start");

  @Test
  void shouldHandleRequiredColumn() {
    Column column = Column.column("foo").setRequired(true).setSemantics("foaf:test");
    LiteralColumnSparqlQueryGenerator mapper = new LiteralColumnSparqlQueryGenerator(START, column);
    assertHasPatterns(mapper, "?start foaf:test ?foo .");
    assertHasSelectors(mapper, "?foo");
    assertHasGroupBy(mapper, "?foo");
  }

  @Test
  void shouldHandleOptionalColumn() {
    Column column = Column.column("foo").setRequired(false).setSemantics("foaf:test");
    LiteralColumnSparqlQueryGenerator mapper = new LiteralColumnSparqlQueryGenerator(START, column);
    assertHasPatterns(mapper, "OPTIONAL { ?start foaf:test ?foo . }");
    assertHasSelectors(mapper, "?foo");
    assertHasGroupBy(mapper, "?foo");
  }

  @Test
  void givenColumnWithMultipleSemantics_thenReturnCoalesce() {
    Column column =
        Column.column("foo")
            .setRequired(false)
            .setSemantics("foaf:test", "foaf:alternative", "foaf:also_alternative");
    LiteralColumnSparqlQueryGenerator mapper = new LiteralColumnSparqlQueryGenerator(START, column);
    assertHasPatterns(
        mapper,
        """
            OPTIONAL { OPTIONAL { ?start foaf:test ?foo0 . }
            OPTIONAL { ?start foaf:alternative ?foo1 . }
            OPTIONAL { ?start foaf:also_alternative ?foo2 . }
            BIND( COALESCE( ?foo0, ?foo1, ?foo2 ) AS ?foo ) }""");
    assertHasSelectors(mapper, "?foo");
    assertHasGroupBy(mapper, "?foo");
  }

  @Test
  void givenColumnWithMultipleSemantics_whenRequired_thenReturnCoalesceWithFilter() {
    Column column =
        Column.column("foo")
            .setRequired(true)
            .setSemantics("foaf:test", "foaf:alternative", "foaf:also_alternative");
    LiteralColumnSparqlQueryGenerator mapper = new LiteralColumnSparqlQueryGenerator(START, column);
    assertHasPatterns(
        mapper,
        """
        OPTIONAL { OPTIONAL { ?start foaf:test ?foo0 . }
        OPTIONAL { ?start foaf:alternative ?foo1 . }
        OPTIONAL { ?start foaf:also_alternative ?foo2 . }
        BIND( COALESCE( ?foo0, ?foo1, ?foo2 ) AS ?foo ) }""",
        "FILTER ( BOUND( ?foo ) )");
    assertHasSelectors(mapper, "?foo");
    assertHasGroupBy(mapper, "?foo");
  }

  @Test
  void shouldNormalizeColumnName() {
    Column column = Column.column("foo bar").setRequired(true).setSemantics("foaf:test");
    LiteralColumnSparqlQueryGenerator mapper = new LiteralColumnSparqlQueryGenerator(START, column);
    assertHasPatterns(mapper, "?start foaf:test ?foo___bar .");
    assertHasSelectors(mapper, "?foo___bar");
    assertHasGroupBy(mapper, "?foo___bar");
  }

  @Test
  void givenColumn_whenSemanticIsIRI_thenSurroundWithPointBrackets() {
    Column column =
        Column.column("foo").setRequired(true).setSemantics("https://example.org/ns#test");
    LiteralColumnSparqlQueryGenerator mapper = new LiteralColumnSparqlQueryGenerator(START, column);
    assertHasPatterns(mapper, "?start <https://example.org/ns#test> ?foo .");
    assertHasSelectors(mapper, "?foo");
    assertHasGroupBy(mapper, "?foo");
  }

  @Test
  void givenColumnWithMultipleSemantics_whenSemanticIsIRI_thenSurroundWithPointyBrackets() {
    Column column =
        Column.column("foo")
            .setRequired(false)
            .setSemantics("foaf:test", "http://example.org/ns#test");
    LiteralColumnSparqlQueryGenerator mapper = new LiteralColumnSparqlQueryGenerator(START, column);
    assertHasPatterns(
        mapper,
        """
            OPTIONAL { OPTIONAL { ?start foaf:test ?foo0 . }
            OPTIONAL { ?start <http://example.org/ns#test> ?foo1 . }
            BIND( COALESCE( ?foo0, ?foo1 ) AS ?foo ) }""");
    assertHasSelectors(mapper, "?foo");
    assertHasGroupBy(mapper, "?foo");
  }
}
