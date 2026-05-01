package org.molgenis.emx2.harvester.mappers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;

class PlainColumnMapperTest {

  private static final Variable START = SparqlBuilder.var("start");

  @Test
  void shouldHandleRequiredColumn() {
    Column column = Column.column("foo").setRequired(true).setSemantics("foaf:test");
    PlainColumnMapper mapper = new PlainColumnMapper(START, column);
    assertPatternsMatch(mapper, "?start foaf:test ?foo .");
  }

  @Test
  void shouldHandleOptionalColumn() {
    Column column = Column.column("foo").setRequired(false).setSemantics("foaf:test");
    PlainColumnMapper mapper = new PlainColumnMapper(START, column);
    assertPatternsMatch(mapper, "OPTIONAL { ?start foaf:test ?foo . }");
  }

  @Test
  void givenColumnWithMultipleSemantics_thenReturnCoalesce() {
    Column column =
        Column.column("foo")
            .setRequired(false)
            .setSemantics("foaf:test", "foaf:alternative", "foaf:also_alternative");
    PlainColumnMapper mapper = new PlainColumnMapper(START, column);
    assertPatternsMatch(
        mapper,
        "OPTIONAL { ?start foaf:test ?foo0 . }",
        "OPTIONAL { ?start foaf:alternative ?foo1 . }",
        "OPTIONAL { ?start foaf:also_alternative ?foo2 . }",
        "BIND( COALESCE( ?foo0, ?foo1, ?foo2 ) AS ?foo )");
  }

  @Test
  void givenColumnWithMultipleSemantics_whenRequired_thenReturnCoalesceWithFilter() {
    Column column =
        Column.column("foo")
            .setRequired(true)
            .setSemantics("foaf:test", "foaf:alternative", "foaf:also_alternative");
    PlainColumnMapper mapper = new PlainColumnMapper(START, column);
    assertPatternsMatch(
        mapper,
        "OPTIONAL { ?start foaf:test ?foo0 . }",
        "OPTIONAL { ?start foaf:alternative ?foo1 . }",
        "OPTIONAL { ?start foaf:also_alternative ?foo2 . }",
        "BIND( COALESCE( ?foo0, ?foo1, ?foo2 ) AS ?foo )",
        "FILTER ( BOUND( ?foo ) )");
  }

  @Test
  void shouldNormalizeColumnName() {
    Column column = Column.column("foo bar").setRequired(true).setSemantics("foaf:test");
    PlainColumnMapper mapper = new PlainColumnMapper(START, column);
    assertPatternsMatch(mapper, "?start foaf:test ?foo_bar .");
  }

  private void assertPatternsMatch(ColumnMapper mapper, String... expectedPatterns) {
    List<GraphPattern> patterns = mapper.getPattern();
    assertEquals(expectedPatterns.length, patterns.size());
    for (int i = 0; i < expectedPatterns.length; i++) {
      assertEquals(expectedPatterns[i], patterns.get(i).getQueryString());
    }
  }
}
