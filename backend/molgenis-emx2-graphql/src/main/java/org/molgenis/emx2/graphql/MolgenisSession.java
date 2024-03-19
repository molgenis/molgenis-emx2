package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.Constants.ANONYMOUS;

import graphql.GraphQL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.tasks.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MolgenisSession {
  private static final Logger logger = LoggerFactory.getLogger(MolgenisSession.class);
  private Database database;
  private GraphQL graphqlForDatabase;
  private Map<String, GraphQL> graphqlPerSchema = new ConcurrentHashMap<>();
  private TaskService taskService;

  public MolgenisSession(TaskService taskService) {
    this.database = new SqlDatabase(SqlDatabase.ANONYMOUS);
    this.taskService = taskService;
  }

  public GraphQL getGraphqlForDatabase() {
    if (graphqlForDatabase == null) {
      graphqlForDatabase = new GraphqlApiFactory().createGraphqlForDatabase(this);
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
      graphqlPerSchema.put(
          schemaName, new GraphqlApiFactory().createGraphqlForSchema(this, schemaName));
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

  public MolgenisSession setSessionUser(String user) {
    if (!user.equals(getSessionUser())) {
      this.database = new SqlDatabase(user);
      this.clearCache();
    }
    return this;
  }

  public void clearSessionUser() {
    this.setSessionUser(ANONYMOUS);
  }

  public void clearCache() {
    this.graphqlPerSchema.clear();
    this.graphqlForDatabase = null;
    logger.info("cleared database and caches for user {}", getSessionUser());
  }

  public TaskService getTaskService() {
    return taskService;
  }
}
