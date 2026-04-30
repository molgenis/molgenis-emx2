package org.molgenis.emx2.rdf.generators.query.mappers;

import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MapperAssertions {

  public static void assertHasSelectors(ColumnMapper mapper, String expectedSelectors) {
    assertEquals(
            mapper.getSelectors(), Stream.of(expectedSelectors).map(SparqlBuilder::var).toList());
  }

  public static void assertPatternsMatch(ColumnMapper mapper, String... expectedPatterns) {
    List<GraphPattern> patterns = mapper.getPattern();
    assertEquals(expectedPatterns.length, patterns.size());
    for (int i = 0; i < expectedPatterns.length; i++) {
      assertEquals(expectedPatterns[i], patterns.get(i).getQueryString());
    }
  }
}
