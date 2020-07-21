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
import static org.molgenis.emx2.graphql.GraphqlManifestField.queryVersionField;

public class GraphqlApiFactory {
  private static Logger logger = LoggerFactory.getLogger(GraphqlApiFactory.class);

  private GraphqlApiFactory() {
    // hide constructor
  }

  public static GraphQL createGraphqlForDatabase(Database database) {
    GraphQLObjectType.Builder queryBuilder = GraphQLObjectType.newObject().name("Query");
    GraphQLObjectType.Builder mutationBuilder = GraphQLObjectType.newObject().name("Save");

    // add login
    queryBuilder.field(queryVersionField());
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
    queryBuilder.field(queryVersionField());
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
      Row r = new Row();
      for (Map.Entry<String, Object> entry : row.entrySet()) {
        // map
        if (entry.getValue() instanceof Map) {
          convertMapToRow(r, entry);
        }
        // list
        else if (entry.getValue() instanceof List) {
          List list = (List) entry.getValue();
          // list of maps becomes map of lists
          if (list.size() > 0 && list.get(0) instanceof Map) {
            // map of lists
            Map<String, List> values = new LinkedHashMap<>();
            for (Map<String, Object> value : (List<Map<String, Object>>) list) {
              for (Map.Entry<String, Object> subValue : value.entrySet()) {
                String key =
                    value.size() == 1 ? entry.getKey() : entry.getKey() + "-" + subValue.getKey();
                if (values.get(key) == null) {
                  values.put(key, new ArrayList<>());
                }
                if (subValue.getValue() instanceof List) {
                  values.get(key).addAll((List) subValue.getValue());
                } else {
                  values.get(key).add(subValue.getValue());
                }
              }
            }
            for (Map.Entry<String, List> value : values.entrySet()) {
              r.set(value.getKey(), value.getValue());
            }
          }
          // otherwise simply add the list to the key
          else {
            r.set(entry.getKey(), list);
          }
          // primitive value
        } else {
          r.set(entry.getKey(), entry.getValue());
        }
      }
      rows.add(r);
    }
    return rows;
  }

  private static void convertMapToRow(Row r, Map.Entry<String, Object> entry) {
    for (Map.Entry<String, Object> ref : ((Map<String, Object>) entry.getValue()).entrySet()) {
      // only if multiple keys, we will use subkey names
      String key =
          ((Map<String, Object>) entry.getValue()).size() == 1
              ? entry.getKey()
              : entry.getKey() + "-" + ref.getKey();
      r.set(key, ref.getValue());
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
    if (json != null) {
      return new ObjectMapper().readValue(json, Map.class);
    } else {
      return new LinkedHashMap<>();
    }
  }
}
