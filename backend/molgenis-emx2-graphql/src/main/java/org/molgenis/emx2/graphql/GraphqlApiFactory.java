package org.molgenis.emx2.graphql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.AsyncExecutionStrategy;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import org.molgenis.emx2.*;
import org.molgenis.emx2.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.molgenis.emx2.ColumnType.REF;

public class GraphqlApiFactory {
  private static Logger logger = LoggerFactory.getLogger(GraphqlApiFactory.class);

  public GraphqlApiFactory() {}

  public GraphQL createGraphqlForDatabase(Database database) {

    GraphQLObjectType.Builder queryBuilder = GraphQLObjectType.newObject().name("Query");
    GraphQLObjectType.Builder mutationBuilder = GraphQLObjectType.newObject().name("Save");

    // add login
    // all the same between schemas
    queryBuilder.field(new GraphqlManifesFieldFactory().queryVersionField());

    // acount operations
    GraphqlSessionFieldFactory user = new GraphqlSessionFieldFactory();
    queryBuilder.field(user.userQueryField(database, null));
    mutationBuilder.field(user.signinField(database));
    mutationBuilder.field(user.signoutField(database));
    mutationBuilder.field(user.signupField(database));

    // database operations
    GraphqlDatabaseFieldFactory db = new GraphqlDatabaseFieldFactory();
    queryBuilder.field(db.settingsQueryField(database));
    queryBuilder.field(db.schemasQuery(database));

    mutationBuilder.field(db.createMutation(database));
    mutationBuilder.field(db.alterMutation(database));
    mutationBuilder.field(db.deleteMutation(database));

    // notice we here add custom exception handler for mutations
    return GraphQL.newGraphQL(
            GraphQLSchema.newSchema().query(queryBuilder).mutation(mutationBuilder).build())
        .mutationExecutionStrategy(new AsyncExecutionStrategy(new GraphqlCustomExceptionHandler()))
        .build();
  }

  public GraphQL createGraphqlForSchema(Schema schema) {
    long start = System.currentTimeMillis();
    logger.info("creating graphql for schema: " + schema.getMetadata().getName());

    GraphQLObjectType.Builder queryBuilder = GraphQLObjectType.newObject().name("Query");
    GraphQLObjectType.Builder mutationBuilder = GraphQLObjectType.newObject().name("Save");

    // queries
    queryBuilder.field(new GraphqlManifesFieldFactory().queryVersionField());

    // account operations
    GraphqlSessionFieldFactory accountFactory = new GraphqlSessionFieldFactory();
    queryBuilder.field(accountFactory.userQueryField(schema.getDatabase(), schema));
    mutationBuilder.field(accountFactory.signinField(schema.getDatabase()));
    mutationBuilder.field(accountFactory.signoutField(schema.getDatabase()));
    mutationBuilder.field(accountFactory.signupField(schema.getDatabase()));

    // schema
    GraphqlSchemaFieldFactory schemaFields = new GraphqlSchemaFieldFactory();
    queryBuilder.field(schemaFields.schemaQuery(schema));
    queryBuilder.field(schemaFields.settingsQuery(schema));
    mutationBuilder.field(schemaFields.createMutation(schema));
    mutationBuilder.field(schemaFields.alterMutation(schema));
    mutationBuilder.field(schemaFields.dropMutation(schema));

    // table
    GraphqlTableFieldFactory tableField = new GraphqlTableFieldFactory();
    for (String tableName : schema.getTableNames()) {
      Table table = schema.getTable(tableName);
      queryBuilder.field(tableField.tableQueryField(table));
      queryBuilder.field(tableField.tableAggField(table));
    }
    mutationBuilder.field(tableField.insertMutation(schema));
    mutationBuilder.field(tableField.updateMutation(schema));
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

    logger.info(
        "creation graphql for schema: "
            + schema.getMetadata().getName()
            + " completed in "
            + (System.currentTimeMillis() - start)
            + "ms");

    return result;
  }

  static Iterable<Row> convertToRows(TableMetadata metadata, List<Map<String, Object>> map) {
    List<Row> rows = new ArrayList<>();
    for (Map<String, Object> object : map) {
      Row row = new Row();
      for (Column column : metadata.getColumns()) {
        if (object.containsKey(column.getName())) {
          if (column.isReference() && REF.equals(column.getColumnType())) {
            convertRefToRow((Map<String, Object>) object.get(column.getName()), row, column);
          } else if (column.isReference()) {
            // REFBACK, REF_ARRAY
            convertRefArrayToRow(
                (List<Map<String, Object>>) object.get(column.getName()), row, column);
          } else {
            row.set(column.getName(), object.get(column.getName()));
          }
        }
      }
      rows.add(row);
    }
    return rows;
  }

  private static void convertRefArrayToRow(List<Map<String, Object>> list, Row row, Column column) {

    List<Reference> refs = column.getReferences();
    for (Reference ref : refs) {
      if (row.get(ref.getName(), ref.getPrimitiveType()) == null) {
        if (list.size() > 0) {
          row.set(ref.getName(), getRefValueFromList(ref.getPath(), list));
        } else {
          row.set(ref.getName(), new ArrayList<>());
        }
      }
    }
  }

  private static List<Object> getRefValueFromList(
      List<String> path, List<Map<String, Object>> list) {
    List<Object> result = new ArrayList<>();
    for (Map map : list) {
      Object value = getRefValueFromMap(path, map);
      if (value != null) {
        result.add(value);
      }
    }
    return result;
  }

  private static Object getRefValueFromMap(List<String> path, Map<String, Object> map) {
    if (path.size() == 1) {
      return map.get(path.get(0));
    } else {
      // should be > 1 and value should be of type map
      Object value = map.get(path.get(0));
      if (value != null) {
        return getRefValueFromMap(path.subList(1, path.size()), (Map<String, Object>) value);
      }
      return null;
    }
  }

  private static void convertRefToRow(Map<String, Object> map, Row row, Column column) {
    for (Reference ref : column.getReferences()) {
      if (row.get(ref.getName(), ref.getPrimitiveType()) == null) {
        String name = ref.getName();
        if (map == null) {
          row.set(name, null);
        } else {
          row.set(ref.getName(), getRefValueFromMap(ref.getPath(), map));
        }
      }
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
      return null;
    }
  }
}
