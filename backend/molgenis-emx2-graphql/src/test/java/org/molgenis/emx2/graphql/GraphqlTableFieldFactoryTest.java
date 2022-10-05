package org.molgenis.emx2.graphql;

import static org.junit.Assert.*;

import org.junit.Test;

public class GraphqlTableFieldFactoryTest {

  @Test
  public void unEscape() {
    assertEquals(
        "should replace double underscores with single underscore",
        "field_a",
        GraphqlTableFieldFactory.unEscape("field__a"));
    assertEquals("should pass though null", null, GraphqlTableFieldFactory.unEscape(null));
  }
}
