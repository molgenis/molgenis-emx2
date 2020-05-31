package org.molgenis.emx2.graphql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.AsyncExecutionStrategy;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.molgenis.emx2.graphql.GraphqlAccountFields.*;
import static org.molgenis.emx2.graphql.GraphqlAccountFields.signupField;

public class GraphqlApiFactory {
  private static Logger logger = LoggerFactory.getLogger(GraphqlApiFactory.class);

  private GraphqlApiFactory() {
    // hide constructor
  }

  public static GraphQL createGraphqlForDatabase(Database database) {
    GraphQLObjectType.Builder queryBuilder = GraphQLObjectType.newObject().name("Query");
    GraphQLObjectType.Builder mutationBuilder = GraphQLObjectType.newObject().name("Save");

    // add login
    queryBuilder.field(userQueryField(database));
    mutationBuilder.field(signinField(database));
    mutationBuilder.field(signoutField(database));
    mutationBuilder.field(signupField(database));

    queryBuilder.field(GraphqlDatabaseFields.querySchemasField(database));
    mutationBuilder.field(GraphqlDatabaseFields.createSchemaField(database));
    mutationBuilder.field(GraphqlDatabaseFields.deleteSchemaField(database));

    // notice we here add custom exception handler for mutations
    return GraphQL.newGraphQL(
            GraphQLSchema.newSchema().query(queryBuilder).mutation(mutationBuilder).build())
        .mutationExecutionStrategy(new AsyncExecutionStrategy(new GraphqlCustomExceptionHandler()))
        .build();
  }

  public static GraphQL createGraphqlForSchema(Schema schema) {
    long start = System.currentTimeMillis();

    GraphQLObjectType.Builder queryBuilder = GraphQLObjectType.newObject().name("Query");
    GraphQLObjectType.Builder mutationBuilder = GraphQLObjectType.newObject().name("Save");

    // queries
    queryBuilder.field(GraphqlSchemaFields.schemaQuery(schema));
    queryBuilder.field(userQueryField(schema.getDatabase()));
    for (String tableName : schema.getTableNames()) {
      Table table = schema.getTable(tableName);
      queryBuilder.field(GraphqlTableQueryFields.tableQueryField(table));
    }

    // mutations

    mutationBuilder.field(GraphqlTableMutationFields.insertMutation(schema));
    mutationBuilder.field(GraphqlTableMutationFields.updateMutation(schema));
    mutationBuilder.field(GraphqlTableMutationFields.deleteMutation(schema));
    mutationBuilder.field(GraphqlSchemaFields.createMutation(schema));
    mutationBuilder.field(GraphqlSchemaFields.alterMutation(schema));
    mutationBuilder.field(GraphqlSchemaFields.dropMutation(schema));

    mutationBuilder.field(signinField(schema.getDatabase()));
    mutationBuilder.field(signoutField(schema.getDatabase()));
    mutationBuilder.field(signupField(schema.getDatabase()));

    // assemble and return
    GraphQL result =
        GraphQL.newGraphQL(
                GraphQLSchema.newSchema().query(queryBuilder).mutation(mutationBuilder).build())
            .mutationExecutionStrategy(
                new AsyncExecutionStrategy(new GraphqlCustomExceptionHandler()))
            .build();

    // log timing so we dont forget to add caching later
    if (logger.isInfoEnabled())
      logger.info(
          "todo: create cache schema loading, it takes {}ms", (System.currentTimeMillis() - start));

    return result;
  }

  static Iterable<Row> convertToRows(List<Map<String, Object>> map) {
    List<Row> rows = new ArrayList<>();
    for (Map<String, Object> row : map) {
      rows.add(new Row(row));
    }
    return rows;
  }

  public static String convertExecutionResultToJson(ExecutionResult executionResult)
      throws JsonProcessingException {
    // tests show conversions below is under 3ms
    Map<String, Object> toSpecificationResult = executionResult.toSpecification();
    return JsonUtil.getWriter().writeValueAsString(toSpecificationResult);
  }

  /** bit unfortunate that we have to convert from json to map and back */
  public static Object transform(String json) throws IOException {
    // benchmark shows this only takes a few ms so not a large performance issue
    if (json != null) {
      return new ObjectMapper().readValue(json, Map.class);
    } else {
      return new LinkedHashMap<>();
    }
  }
}
