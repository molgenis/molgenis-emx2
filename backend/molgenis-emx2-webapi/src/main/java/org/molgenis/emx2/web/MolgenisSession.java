package org.molgenis.emx2.web;

import graphql.GraphQL;
import java.util.LinkedHashMap;
import java.util.Map;
import org.joda.time.DateTime;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.graphql.GraphqlApiFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MolgenisSession {
  private static final Logger logger = LoggerFactory.getLogger(MolgenisSession.class);

  private String token;
  private Database database;
  private GraphQL graphqlForDatabase;
  private DateTime createTime;
  private Map<String, GraphQL> graphqlPerSchema = new LinkedHashMap<>();

  public MolgenisSession(Database database, String token) {
    this.token = token;
    this.database = database;
    this.createTime = DateTime.now();
  }

  public GraphQL getGraphqlForDatabase() {
    if (graphqlForDatabase == null) {
      graphqlForDatabase = new GraphqlApiFactory().createGraphqlForDatabase(database);
      logger.info("created graphql for user {}", getSessionUser());
    }
    return graphqlForDatabase;
  }

  public GraphQL getGraphqlForSchema(String schemaName) {
    logger.info("getting graphql schema '{}' for user '{}'", schemaName, getSessionUser());
    if (graphqlPerSchema.get(schemaName) == null) {
      Schema schema = database.getSchema(schemaName);
      if (schema == null)
        throw new MolgenisException(
            "Schema not found: Schema with name '"
                + schemaName
                + "' does not exist or permission denied");
      graphqlPerSchema.put(schemaName, new GraphqlApiFactory().createGraphqlForSchema(schema));
      logger.info("created graphql schema '{}' for user '{}'", schemaName, getSessionUser());
    }
    logger.info("return graphql schema '{}' for user '{}'", schemaName, getSessionUser());
    return graphqlPerSchema.get(schemaName);
  }

  public Database getDatabase() {
    return database;
  }

  public String getSessionUser() {
    return database.getActiveUser();
  }

  public DateTime getCreateTime() {
    return createTime;
  }

  public void clearCache() {
    this.graphqlPerSchema.clear();
    this.graphqlForDatabase = null;
    this.database.clearCache();
    logger.info("cleared database and caches for user {}", getSessionUser());
  }

  public void setCreateTime(DateTime newTime) {
    this.createTime = newTime;
  }

  public String getToken() {
    return token;
  }
}
