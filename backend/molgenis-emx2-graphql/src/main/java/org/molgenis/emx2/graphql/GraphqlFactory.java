package org.molgenis.emx2.graphql;

import graphql.GraphQL;
import graphql.execution.AsyncExecutionStrategy;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import org.molgenis.emx2.*;
import org.molgenis.emx2.tasks.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphqlFactory {
  private static Logger logger = LoggerFactory.getLogger(GraphqlFactory.class);

  private GraphqlFactory() {}

  public static GraphQL forDatabase(Database database, TaskService taskService) {

    GraphQLCodeRegistry.Builder codeRegistry = GraphQLCodeRegistry.newCodeRegistry();
    GraphQLObjectType.Builder queryBuilder = GraphQLObjectType.newObject().name("Query");
    GraphQLObjectType.Builder mutationBuilder = GraphQLObjectType.newObject().name("Save");

    queryBuilder.field(new GraphqlManifesFieldFactory().queryVersionField(database, codeRegistry));

    if (database.isAdmin()) {
      queryBuilder.field(GraphqlAdminFieldFactory.queryAdminField(database, codeRegistry));
      mutationBuilder.field(GraphqlAdminFieldFactory.removeUser(database, codeRegistry));
      mutationBuilder.field(GraphqlAdminFieldFactory.setEnabledUser(database, codeRegistry));
      mutationBuilder.field(GraphqlAdminFieldFactory.updateUser(database, codeRegistry));
    }

    GraphqlDatabaseFieldFactory db = new GraphqlDatabaseFieldFactory();
    queryBuilder.field(db.schemasQuery(database, codeRegistry));
    queryBuilder.field(db.settingsQueryField(database, codeRegistry));
    queryBuilder.field(db.tasksQueryField(taskService, database, codeRegistry));
    if (database.isAdmin()) {
      queryBuilder.field(db.lastUpdateQuery(database, codeRegistry));
    }

    mutationBuilder.field(db.createMutation(database, taskService, codeRegistry));
    mutationBuilder.field(db.deleteMutation(database, codeRegistry));
    mutationBuilder.field(db.updateMutation(database, codeRegistry));
    mutationBuilder.field(db.dropMutation(database, codeRegistry));
    mutationBuilder.field(db.changeMutation(database, codeRegistry));

    GraphqlSessionFieldFactory session = new GraphqlSessionFieldFactory();
    queryBuilder.field(session.sessionQueryField(database, null, codeRegistry));
    mutationBuilder.field(session.signinField(database, codeRegistry));
    mutationBuilder.field(session.signupField(database, codeRegistry));
    if (!database.isAnonymous()) {
      mutationBuilder.field(session.signoutField(database, codeRegistry));
      mutationBuilder.field(session.changePasswordField(database, codeRegistry));
      mutationBuilder.field(session.createTokenField(database, codeRegistry));
    }

    GraphQL graphql =
        GraphQL.newGraphQL(
                GraphQLSchema.newSchema()
                    .query(queryBuilder)
                    .mutation(mutationBuilder)
                    .codeRegistry(codeRegistry.build())
                    .build())
            .mutationExecutionStrategy(
                new AsyncExecutionStrategy(new GraphqlCustomExceptionHandler()))
            .build();

    return graphql;
  }

  public static GraphQL forSchema(Schema schema, TaskService taskService) {
    long start = System.currentTimeMillis();
    logger.info("creating graphql for schema: {}", schema.getMetadata().getName());

    GraphQLCodeRegistry.Builder codeRegistry = GraphQLCodeRegistry.newCodeRegistry();
    GraphQLObjectType.Builder queryBuilder = GraphQLObjectType.newObject().name("Query");
    GraphQLObjectType.Builder mutationBuilder = GraphQLObjectType.newObject().name("Save");

    queryBuilder.field(
        new GraphqlManifesFieldFactory().queryVersionField(schema.getDatabase(), codeRegistry));

    GraphqlSchemaFieldFactory schemaFields = new GraphqlSchemaFieldFactory();
    queryBuilder.field(schemaFields.schemaQuery(schema, codeRegistry));
    queryBuilder.field(schemaFields.settingsQuery(schema, codeRegistry));
    queryBuilder.field(schemaFields.schemaReportsField(schema, codeRegistry));

    GraphqlDatabaseFieldFactory db = new GraphqlDatabaseFieldFactory();
    queryBuilder.field(db.tasksQueryField(taskService, schema.getDatabase(), codeRegistry));

    GraphqlSessionFieldFactory sessionFieldFactory = new GraphqlSessionFieldFactory();
    queryBuilder.field(
        sessionFieldFactory.sessionQueryField(schema.getDatabase(), schema, codeRegistry));
    mutationBuilder.field(sessionFieldFactory.signinField(schema.getDatabase(), codeRegistry));
    mutationBuilder.field(sessionFieldFactory.signupField(schema.getDatabase(), codeRegistry));

    if (!schema.getDatabase().isAnonymous()) {
      mutationBuilder.field(sessionFieldFactory.signoutField(schema.getDatabase(), codeRegistry));
      mutationBuilder.field(
          sessionFieldFactory.changePasswordField(schema.getDatabase(), codeRegistry));
      mutationBuilder.field(
          sessionFieldFactory.createTokenField(schema.getDatabase(), codeRegistry));
    }

    mutationBuilder.field(schemaFields.changeMutation(schema, codeRegistry));
    mutationBuilder.field(schemaFields.dropMutation(schema, codeRegistry));
    mutationBuilder.field(schemaFields.truncateMutation(schema, taskService, codeRegistry));

    if (PermissionEvaluator.canManage(schema)) {
      queryBuilder.field(schemaFields.changeLogQuery(schema, codeRegistry));
      queryBuilder.field(schemaFields.changeLogCountQuery(schema, codeRegistry));
    }

    GraphqlTableFieldFactory tableField = new GraphqlTableFieldFactory(schema);
    for (TableMetadata table : schema.getMetadata().getTables()) {
      if (table.getColumns().size() > 0) {
        if (tableField.hasViewPermission(table)) {
          queryBuilder.field(tableField.tableQueryField(table, codeRegistry));
        }
        queryBuilder.field(tableField.tableAggField(table, codeRegistry));
        queryBuilder.field(tableField.tableGroupByField(table, codeRegistry));
      }
    }
    mutationBuilder.field(tableField.insertMutation(schema, codeRegistry));
    mutationBuilder.field(tableField.updateMutation(schema, codeRegistry));
    mutationBuilder.field(tableField.upsertMutation(schema, codeRegistry));
    mutationBuilder.field(tableField.deleteMutation(schema, codeRegistry));

    GraphQL graphql =
        GraphQL.newGraphQL(
                GraphQLSchema.newSchema()
                    .query(queryBuilder)
                    .mutation(mutationBuilder)
                    .codeRegistry(codeRegistry.build())
                    .build())
            .mutationExecutionStrategy(
                new AsyncExecutionStrategy(new GraphqlCustomExceptionHandler()))
            .build();

    if (logger.isInfoEnabled()) {
      logger.info(
          "creation graphql for schema: {} completed in {}ms",
          schema.getMetadata().getName(),
          (System.currentTimeMillis() - start));
    }

    return graphql;
  }
}
