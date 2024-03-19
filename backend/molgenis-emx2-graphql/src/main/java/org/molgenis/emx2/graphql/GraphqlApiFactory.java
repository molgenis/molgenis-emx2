package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.Privileges.VIEWER;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.AsyncExecutionStrategy;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import java.io.IOException;
import java.util.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphqlApiFactory {
  private static Logger logger = LoggerFactory.getLogger(GraphqlApiFactory.class);

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

  public GraphQL createGraphqlForDatabase(MolgenisSession session) {

    GraphQLObjectType.Builder queryBuilder = GraphQLObjectType.newObject().name("Query");
    GraphQLObjectType.Builder mutationBuilder = GraphQLObjectType.newObject().name("Save");

    // add login
    // all the same between schemas
    queryBuilder.field(new GraphqlManifesFieldFactory().queryVersionField(session.getDatabase()));

    // admin operations
    if (session.getDatabase().isAdmin()) {
      queryBuilder.field(GraphlAdminFieldFactory.queryAdminField(session.getDatabase()));
    }

    // database operations
    GraphqlDatabaseFieldFactory db = new GraphqlDatabaseFieldFactory();
    queryBuilder.field(db.schemasQuery(session.getDatabase()));
    queryBuilder.field(db.settingsQueryField(session.getDatabase()));
    queryBuilder.field(db.tasksQueryField(session.getTaskService()));

    mutationBuilder.field(db.createMutation(session.getDatabase()));
    mutationBuilder.field(db.deleteMutation(session.getDatabase()));
    mutationBuilder.field(db.updateMutation(session.getDatabase()));
    mutationBuilder.field(db.dropMutation(session.getDatabase()));
    mutationBuilder.field(db.changeMutation(session.getDatabase()));

    // account operations
    GraphqlSessionFieldFactory sessionFieldFactory = new GraphqlSessionFieldFactory();
    queryBuilder.field(sessionFieldFactory.sessionQueryField(session.getDatabase(), null));
    mutationBuilder.field(sessionFieldFactory.signinField(session));
    mutationBuilder.field(sessionFieldFactory.signupField(session.getDatabase()));
    if (!session.getDatabase().isAnonymous()) {
      mutationBuilder.field(sessionFieldFactory.signoutField(session));
      mutationBuilder.field(sessionFieldFactory.changePasswordField(session.getDatabase()));
      mutationBuilder.field(sessionFieldFactory.createTokenField(session.getDatabase()));
    }

    // notice we here add custom exception handler for mutations
    return GraphQL.newGraphQL(
            GraphQLSchema.newSchema().query(queryBuilder).mutation(mutationBuilder).build())
        .mutationExecutionStrategy(new AsyncExecutionStrategy(new GraphqlCustomExceptionHandler()))
        .build();
  }

  public GraphQL createGraphqlForSchema(MolgenisSession session, String schemaName) {
    long start = System.currentTimeMillis();
    logger.info("creating graphql for schema: {}", schemaName);

    GraphQLObjectType.Builder queryBuilder = GraphQLObjectType.newObject().name("Query");
    GraphQLObjectType.Builder mutationBuilder = GraphQLObjectType.newObject().name("Save");

    // _manifest query
    queryBuilder.field(new GraphqlManifesFieldFactory().queryVersionField(session.getDatabase()));

    // _schema query
    Schema schema = session.getDatabase().getSchema(schemaName);
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
    mutationBuilder.field(sessionFieldFactory.signupField(schema.getDatabase()));

    // authenticated user operations
    if (!schema.getDatabase().isAnonymous()) {
      mutationBuilder.field(sessionFieldFactory.signoutField(session));
      mutationBuilder.field(sessionFieldFactory.changePasswordField(schema.getDatabase()));
      mutationBuilder.field(sessionFieldFactory.createTokenField(schema.getDatabase()));
    }

    mutationBuilder.field(schemaFields.changeMutation(schema));
    mutationBuilder.field(schemaFields.dropMutation(schema));
    mutationBuilder.field(schemaFields.truncateMutation(schema));
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
                GraphQLSchema.newSchema()
                    .query(queryBuilder.build())
                    .mutation(mutationBuilder.build())
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

    return result;
  }
}
