package org.molgenis.emx2.rdf.generators.query.mappers;

import static org.molgenis.emx2.rdf.generators.query.mappers.MapperAssertions.*;

import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;

class CollectionColumnMapperTest {

  private static final Variable START = SparqlBuilder.var("start");

  @Test
  void shouldConcatSelectors() {
    Column column = Column.column("foo").setRequired(true).setSemantics("foaf:test");
    ColumnMapper mapper = new CollectionColumnMapper(START, column);
    assertHasPatterns(mapper, "?start foaf:test ?foo_single .");
    assertHasSelectors(mapper, "( GROUP_CONCAT( STR( ?foo_single ) ; SEPARATOR = ',' ) AS ?foo )");
    assertHasGroupBy(mapper);
  }

  @Test
  void givenMultipleSemantics_thenConcatBindValue() {
    Column column =
        Column.column("foo")
            .setRequired(true)
            .setSemantics("foaf:test", "foaf:alternative", "foaf:also_alternative");
    ColumnMapper mapper = new CollectionColumnMapper(START, column);

    assertHasPatterns(
        mapper,
        """
            OPTIONAL { OPTIONAL { ?start foaf:test ?foo_single0 . }
            OPTIONAL { ?start foaf:alternative ?foo_single1 . }
            OPTIONAL { ?start foaf:also_alternative ?foo_single2 . }
            BIND( COALESCE( ?foo_single0, ?foo_single1, ?foo_single2 ) AS ?foo_single ) }""",
        "FILTER ( BOUND( ?foo_single ) )");
    assertHasSelectors(mapper, "( GROUP_CONCAT( STR( ?foo_single ) ; SEPARATOR = ',' ) AS ?foo )");
    assertHasGroupBy(mapper);
  }
}
