package org.molgenis.emx2.web;

import graphql.GraphQL;

import org.joda.time.DateTime;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.molgenis.emx2.web.graphql.GraphqlApi.createGraphqlForDatabase;
import static org.molgenis.emx2.web.graphql.GraphqlApi.createGraphqlForSchema;

public class MolgenisSession {
  private static final Logger logger = LoggerFactory.getLogger(MolgenisSession.class);

  private String sessionUser;
  private Database database;
  private GraphQL graphqlForDatabase;
  private DateTime createTime;
  private Map<String, GraphQL> graphqlPerSchema = new LinkedHashMap<>();

  public MolgenisSession(Database database) {
    this.database = database;
    this.sessionUser = database.getActiveUser();
    this.createTime = DateTime.now();
  }

  // todo remove this method, molgenis session shouldn't know this
  public GraphQL getGraphqlForDatabase() {
    if (graphqlForDatabase == null) {
      graphqlForDatabase = createGraphqlForDatabase(database);
      logger.info("created graphql for user {}", getSessionUser());
    }
    return graphqlForDatabase;
  }

  public GraphQL getGraphqlForSchema(String schemaName) {
    if (graphqlPerSchema.get(schemaName) == null) {
      Schema schema = database.getSchema(schemaName);
      if (schema == null)
        throw new MolgenisException(
            "Schema not found",
            "Schema with name '" + schemaName + "' does not exist or permission denied");
      graphqlPerSchema.put(schemaName, createGraphqlForSchema(schema));
      logger.info("created graphql {} for user {}", schemaName, getSessionUser());
    }
    return graphqlPerSchema.get(schemaName);
  }

  public Database getDatabase() {
    return database;
  }

  public String getSessionUser() {
    return sessionUser;
  }

  public DateTime getCreateTime() {
    return createTime;
  }

  public void clearSchemaCache(String schemaName) {
    this.graphqlPerSchema.remove(schemaName);
    logger.info("cleared schema cache {} for user {}", schemaName, getSessionUser());
  }

  public void clearCache() {
    this.graphqlPerSchema.clear();
    logger.info("cleared schema for user {}", getSessionUser());
  }
}
