package org.molgenis.emx2.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.molgenis.emx2.Constants.ANONYMOUS;

import graphql.GraphQL;
import org.junit.jupiter.api.Test;

class MolgenisSessionTest {

  @Test
  void getGraphqlForSchema() {
    MolgenisSession molgenisSession = new MolgenisSession(ANONYMOUS);
    GraphQL graphqlForSchema = molgenisSession.getGraphqlForSchema("testSchema");
    assertNotNull(graphqlForSchema, "GraphQL schema should not be null");

    when(molgenisSession.getSessionUser()).thenReturn("anonymous");
    GraphQL anonymousGraphql = molgenisSession.getGraphqlForSchema("testSchema");
    assertNotNull(anonymousGraphql, "Anonymous GraphQL schema should not be null");

    molgenisSession.clearCache();
    GraphQL anonymousGraphqlAfterClear = molgenisSession.getGraphqlForSchema("testSchema");
    assertNotNull(anonymousGraphqlAfterClear, "Anonymous GraphQL schema should not be null");
    assertEquals(anonymousGraphql, anonymousGraphqlAfterClear);
  }
}
