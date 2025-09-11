package org.molgenis.emx2.graphql;

import graphql.GraphQL;
import org.molgenis.emx2.Database;

public class EMX2GraphQL {
  private Database database;
  private GraphQL graphQL;

  public EMX2GraphQL(Database database, GraphQL graphQL) {
    this.database = database;
    this.graphQL = graphQL;
  }

  public Database getDatabase() {
    return database;
  }

  public GraphQL getGraphQL() {
    return graphQL;
  }
}
