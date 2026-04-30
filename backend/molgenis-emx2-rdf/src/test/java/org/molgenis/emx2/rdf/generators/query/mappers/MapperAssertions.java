package org.molgenis.emx2.rdf.generators.query.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.eclipse.rdf4j.sparqlbuilder.core.QueryElement;

public class MapperAssertions {

  public static void assertHasSelectors(ColumnMapper mapper, String... expectedSelectors) {
    assertEquals(
        mapper.getSelectors().stream().map(QueryElement::getQueryString).toList(),
        List.of(expectedSelectors));
  }

  public static void assertPatternsMatch(ColumnMapper mapper, String... expectedPatterns) {
    assertEquals(
        mapper.getPattern().stream().map(QueryElement::getQueryString).toList(),
        List.of(expectedPatterns));
  }
}
