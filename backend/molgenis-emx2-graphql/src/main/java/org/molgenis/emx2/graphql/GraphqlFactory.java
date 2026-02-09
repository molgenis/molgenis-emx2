package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.Privileges.VIEWER;

import graphql.GraphQL;
import graphql.execution.AsyncExecutionStrategy;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import org.molgenis.emx2.*;
import org.molgenis.emx2.tasks.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphqlFactory {
  private static Logger logger = LoggerFactory.getLogger(GraphqlFactory.class);

  private GraphqlFactory() {}

  public static GraphQL createGraphqlForDatabase(Database database, TaskService taskService) {

    GraphQLObjectType.Builder queryBuilder = GraphQLObjectType.newObject().name("Query");
    GraphQLObjectType.Builder mutationBuilder = GraphQLObjectType.newObject().name("Save");

    queryBuilder.field(new GraphqlManifesFieldFactory().queryVersionField(database));

    if (database.isAdmin()) {
      queryBuilder.field(GraphqlAdminFieldFactory.queryAdminField(database));
      mutationBuilder.field(GraphqlAdminFieldFactory.removeUser(database));
      mutationBuilder.field(GraphqlAdminFieldFactory.setEnabledUser(database));
      mutationBuilder.field(GraphqlAdminFieldFactory.updateUser(database));
    }

    GraphqlDatabaseFieldFactory db = new GraphqlDatabaseFieldFactory();
    queryBuilder.field(db.schemasQuery(database));
    queryBuilder.field(db.settingsQueryField(database));
    queryBuilder.field(db.tasksQueryField(taskService));
    if (database.isAdmin()) {
      queryBuilder.field(db.lastUpdateQuery(database));
    }

    mutationBuilder.field(db.createMutation(database, taskService));
    mutationBuilder.field(db.deleteMutation(database));
    mutationBuilder.field(db.updateMutation(database));
    mutationBuilder.field(db.dropMutation(database));
    mutationBuilder.field(db.changeMutation(database));

    GraphqlSessionFieldFactory session = new GraphqlSessionFieldFactory();
    queryBuilder.field(session.sessionQueryField(database, null));
    mutationBuilder.field(session.signinField(database));
    mutationBuilder.field(session.signupField(database));
    if (!database.isAnonymous()) {
      mutationBuilder.field(session.signoutField(database));
      mutationBuilder.field(session.changePasswordField(database));
      mutationBuilder.field(session.createTokenField(database));
    }

    GraphQL graphql =
        GraphQL.newGraphQL(
                GraphQLSchema.newSchema().query(queryBuilder).mutation(mutationBuilder).build())
            .mutationExecutionStrategy(
                new AsyncExecutionStrategy(new GraphqlCustomExceptionHandler()))
            .build();

    return graphql;
  }

  public static GraphQL createGraphqlForSchema(Schema schema, TaskService taskService) {
    long start = System.currentTimeMillis();
    logger.info("creating graphql for schema: {}", schema.getMetadata().getName());

    GraphQLObjectType.Builder queryBuilder = GraphQLObjectType.newObject().name("Query");
    GraphQLObjectType.Builder mutationBuilder = GraphQLObjectType.newObject().name("Save");

    queryBuilder.field(new GraphqlManifesFieldFactory().queryVersionField(schema.getDatabase()));

    GraphqlSchemaFieldFactory schemaFields = new GraphqlSchemaFieldFactory();
    queryBuilder.field(schemaFields.schemaQuery(schema));
    queryBuilder.field(schemaFields.settingsQuery(schema));
    queryBuilder.field(schemaFields.schemaReportsField(schema));

    GraphqlDatabaseFieldFactory db = new GraphqlDatabaseFieldFactory();
    queryBuilder.field(db.tasksQueryField(taskService));

    GraphqlSessionFieldFactory sessionFieldFactory = new GraphqlSessionFieldFactory();
    queryBuilder.field(sessionFieldFactory.sessionQueryField(schema.getDatabase(), schema));
    mutationBuilder.field(sessionFieldFactory.signinField(schema.getDatabase()));
    mutationBuilder.field(sessionFieldFactory.signupField(schema.getDatabase()));

    if (!schema.getDatabase().isAnonymous()) {
      mutationBuilder.field(sessionFieldFactory.signoutField(schema.getDatabase()));
      mutationBuilder.field(sessionFieldFactory.changePasswordField(schema.getDatabase()));
      mutationBuilder.field(sessionFieldFactory.createTokenField(schema.getDatabase()));
    }

    mutationBuilder.field(schemaFields.changeMutation(schema));
    mutationBuilder.field(schemaFields.dropMutation(schema));
    mutationBuilder.field(schemaFields.truncateMutation(schema, taskService));

    if ((schema.getRoleForActiveUser() != null
            && schema.getRoleForActiveUser().equals(Privileges.MANAGER.toString()))
        || schema.getDatabase().isAdmin()) {
      queryBuilder.field(schemaFields.changeLogQuery(schema));
      queryBuilder.field(schemaFields.changeLogCountQuery(schema));
    }

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

    GraphQL graphql =
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

    return graphql;
  }
}
