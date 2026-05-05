package org.molgenis.emx2.rdf.generators.query.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.eclipse.rdf4j.sparqlbuilder.core.QueryElement;

public class MapperAssertions {

  public static void assertHasSelectors(ColumnMapper mapper, String... expectedSelectors) {
    assertEquals(
        List.of(expectedSelectors),
        mapper.getSelectors().stream().map(QueryElement::getQueryString).toList());
  }

  public static void assertHasPatterns(ColumnMapper mapper, String... expectedPatterns) {
    assertEquals(
        List.of(expectedPatterns),
        mapper.getPattern().stream().map(QueryElement::getQueryString).toList());
  }

  public static void assertHasGroupBy(ColumnMapper mapper, String... expectedGroupBy) {
    assertEquals(
        List.of(expectedGroupBy),
        mapper.getGroupBy().stream().map(QueryElement::getQueryString).toList());
  }
}
