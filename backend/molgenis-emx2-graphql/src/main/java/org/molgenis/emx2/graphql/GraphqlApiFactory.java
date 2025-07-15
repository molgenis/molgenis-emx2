package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.Privileges.VIEWER;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.AsyncExecutionStrategy;
import graphql.parser.ParserOptions;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import java.io.IOException;
import java.util.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.json.JsonUtil;
import org.molgenis.emx2.tasks.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// only use in context of GraphqlSession, should not be used directly except for testing
public class GraphqlApiFactory {
  private static Logger logger = LoggerFactory.getLogger(GraphqlApiFactory.class);

  public GraphqlApiFactory() {
    if (ParserOptions.getDefaultParserOptions().getMaxTokens() < 1000000) {
      ParserOptions.setDefaultParserOptions(
          ParserOptions.newParserOptions().maxTokens(1000000).build());
      ParserOptions.setDefaultOperationParserOptions(
          ParserOptions.newParserOptions().maxTokens(1000000).build());
    }
  }

  public static String convertExecutionResultToJson(ExecutionResult executionResult)
      throws JsonProcessingException {
    // tests show conversions below is under 3ms
    Map<String, Object> toSpecificationResult = executionResult.toSpecification();
    return JsonUtil.getWriter().writeValueAsString(toSpecificationResult);
  }

  /** bit unfortunate that we have to convert from json to map and back */
  static Object transform(String json) throws IOException {
    // benchmark shows this only takes a few ms so not a large performance issue
    // alternatively, we should change the SQL to result escaped results but that is a nightmare to
    // build
    if (json != null) {
      return new ObjectMapper().readValue(json, Map.class);
    } else {
      return null;
    }
  }

  GraphQL createGraphqlForDatabase(GraphqlSession session) {

    GraphQLObjectType.Builder queryBuilder = GraphQLObjectType.newObject().name("Query");
    GraphQLObjectType.Builder mutationBuilder = GraphQLObjectType.newObject().name("Save");
    Database database = session.getDatabase();
    TaskService taskService = session.getTaskService();

    // add login
    // all the same between schemas
    queryBuilder.field(new GraphqlManifesFieldFactory().queryVersionField(database));

    // admin operations
    if (database.isAdmin()) {
      queryBuilder.field(GraphqlAdminFieldFactory.queryAdminField(database));
      mutationBuilder.field(GraphqlAdminFieldFactory.removeUser(database));
      mutationBuilder.field(GraphqlAdminFieldFactory.setEnabledUser(database));
      mutationBuilder.field(GraphqlAdminFieldFactory.updateUser(database));
    }

    // database operations
    GraphqlDatabaseFieldFactory db = new GraphqlDatabaseFieldFactory();
    queryBuilder.field(db.schemasQuery(database));
    queryBuilder.field(db.settingsQueryField(database));
    queryBuilder.field(db.tasksQueryField(taskService));
    // todo need to allow for owner ? ( need to filter the query to include only owned schema's)
    if (database.isAdmin()) {
      queryBuilder.field(db.lastUpdateQuery(database));
    }

    mutationBuilder.field(db.createMutation(database, taskService));
    mutationBuilder.field(db.deleteMutation(database));
    mutationBuilder.field(db.updateMutation(database));
    mutationBuilder.field(db.dropMutation(database));
    mutationBuilder.field(db.changeMutation(database));

    // account operations
    GraphqlSessionFieldFactory factory = new GraphqlSessionFieldFactory();
    queryBuilder.field(factory.sessionQueryField(database, null));
    mutationBuilder.field(factory.signinField(session));
    mutationBuilder.field(factory.signupField(session));
    if (!database.isAnonymous()) {
      mutationBuilder.field(factory.signoutField(session));
      mutationBuilder.field(factory.changePasswordField(database));
      mutationBuilder.field(factory.createTokenField(database));
    }

    // notice we here add custom exception handler for mutations
    return GraphQL.newGraphQL(
            GraphQLSchema.newSchema().query(queryBuilder).mutation(mutationBuilder).build())
        .mutationExecutionStrategy(new AsyncExecutionStrategy(new GraphqlCustomExceptionHandler()))
        .build();
  }

  GraphQL createGraphqlForSchema(Schema schema, GraphqlSession session) {
    long start = System.currentTimeMillis();
    logger.info("creating graphql for schema: {}", schema.getMetadata().getName());

    GraphQLObjectType.Builder queryBuilder = GraphQLObjectType.newObject().name("Query");
    GraphQLObjectType.Builder mutationBuilder = GraphQLObjectType.newObject().name("Save");

    // _manifest query
    queryBuilder.field(new GraphqlManifesFieldFactory().queryVersionField(schema.getDatabase()));

    // _schema query
    GraphqlSchemaFieldFactory schemaFields = new GraphqlSchemaFieldFactory();
    queryBuilder.field(schemaFields.schemaQuery(schema));
    queryBuilder.field(schemaFields.settingsQuery(schema));
    queryBuilder.field(schemaFields.schemaReportsField(schema));

    // _tasks query
    GraphqlDatabaseFieldFactory db = new GraphqlDatabaseFieldFactory();
    queryBuilder.field(db.tasksQueryField(session.getTaskService()));

    // _session query
    GraphqlSessionFieldFactory sessionFieldFactory = new GraphqlSessionFieldFactory();
    queryBuilder.field(sessionFieldFactory.sessionQueryField(schema.getDatabase(), schema));
    mutationBuilder.field(sessionFieldFactory.signinField(session));
    mutationBuilder.field(sessionFieldFactory.signupField(session));

    // authenticated user operations
    if (!schema.getDatabase().isAnonymous()) {
      mutationBuilder.field(sessionFieldFactory.signoutField(session));
      mutationBuilder.field(sessionFieldFactory.changePasswordField(schema.getDatabase()));
      mutationBuilder.field(sessionFieldFactory.createTokenField(schema.getDatabase()));
    }

    mutationBuilder.field(schemaFields.changeMutation(schema));
    mutationBuilder.field(schemaFields.dropMutation(schema));
    mutationBuilder.field(schemaFields.truncateMutation(schema, session.getTaskService()));
    if ((schema.getRoleForActiveUser() != null
            && schema.getRoleForActiveUser().equals(Privileges.MANAGER.toString()))
        || schema.getDatabase().isAdmin()) {
      queryBuilder.field(schemaFields.changeLogQuery(schema));
      queryBuilder.field(schemaFields.changeLogCountQuery(schema));
    }

    // table
    GraphqlTableFieldFactory tableField = new GraphqlTableFieldFactory(schema);
    for (TableMetadata table : schema.getMetadata().getTables()) {
      if (table.getColumns().size() > 0) {
        if (table.getTableType().equals(TableType.ONTOLOGIES)
            || schema.getInheritedRolesForActiveUser().contains(VIEWER.toString())) {
          queryBuilder.field(tableField.tableQueryField(table));
        }
        queryBuilder.field(tableField.tableAggField(table));
        queryBuilder.field(tableField.tableGroupByField(table));
      }
    }
    mutationBuilder.field(tableField.insertMutation(schema));
    mutationBuilder.field(tableField.updateMutation(schema));
    mutationBuilder.field(tableField.upsertMutation(schema));
    mutationBuilder.field(tableField.deleteMutation(schema));

    // assemble and return
    GraphQL result =
        GraphQL.newGraphQL(
                GraphQLSchema.newSchema().query(queryBuilder).mutation(mutationBuilder).build())
            .mutationExecutionStrategy(
                new AsyncExecutionStrategy(new GraphqlCustomExceptionHandler()))
            .build();

    if (logger.isInfoEnabled()) {
      logger.info(
          "creation graphql for schema: {} completed in {}ms",
          schema.getMetadata().getName(),
          (System.currentTimeMillis() - start));
    }

    return result;
  }
}
