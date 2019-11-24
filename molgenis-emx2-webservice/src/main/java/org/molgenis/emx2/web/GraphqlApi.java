package org.molgenis.emx2.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.Scalars;
import graphql.schema.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.SqlJsonQuery;
import org.molgenis.emx2.sql.SqlTypeUtils;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectField.newInputObjectField;
import static graphql.schema.GraphQLInputObjectType.newInputObject;
import static graphql.schema.GraphQLObjectType.newObject;
import static org.molgenis.emx2.ColumnType.REF;
import static spark.Spark.*;

public class GraphqlApi {

  private GraphqlApi() {
    // hide constructor
  }

  public static void createGraphQLSchema() {

    // schema level operations
    final String schemaPath = "/api/graphql/:schema"; // NOSONAR
    get(schemaPath, GraphqlApi::getQuery);
    post(schemaPath, GraphqlApi::getQuery);
  }

  private static String getQuery(Request request, Response response) throws IOException {

    // very expensive for now, somehow need to cache this I suppose
    Schema schema =
        MolgenisWebservice.getAuthenticatedDatabase(request)
            .getSchema(request.params(MolgenisWebservice.SCHEMA));
    GraphQLSchema gl = createGraphQLSchema(schema);

    String query = null;
    if ("POST".equals(request.requestMethod())) {
      ObjectNode node = new ObjectMapper().readValue(request.body(), ObjectNode.class);
      query = node.get("query").asText();
    } else {
      query =
          request.queryParamOrDefault(
              "query",
              "{\n"
                  + "  __schema {\n"
                  + "    types {\n"
                  + "      name\n"
                  + "    }\n"
                  + "  }\n"
                  + "}");
    }
    System.out.println("query\n" + query);
    GraphQL g = GraphQL.newGraphQL(gl).build();

    ExecutionResult executionResult = g.execute(query);
    for (GraphQLError err : executionResult.getErrors()) {
      System.err.println(err);
    }
    Map<String, Object> toSpecificationResult = executionResult.toSpecification();
    String result = JsonApi.getWriter().writeValueAsString(toSpecificationResult);
    System.out.println("result:\n" + result);
    return result;
  }

  public static final String MUTATION_RESULT = "MutationResult";

  public static GraphQLSchema createGraphQLSchema(Schema model) {

    GraphQLObjectType.Builder queryBuilder = newObject().name("Query");
    GraphQLObjectType.Builder mutationBuilder = newObject().name("Save");

    for (String tableName : model.getTableNames()) {
      Table table = model.getTable(tableName);
      TableMetadata tableMetadata = table.getMetadata();

      // add each table as a table type
      GraphQLObjectType.Builder typeBuilder = newObject().name(tableName);
      for (Column col : tableMetadata.getColumns()) {
        typeBuilder.field(newFieldDefinition().name(col.getColumnName()).type(getType(col)));
      }
      GraphQLObjectType type = typeBuilder.build();

      // add each table as input type
      GraphQLInputObjectType.Builder inputBuilder = newInputObject().name(tableName + "Input");
      for (Column col : tableMetadata.getColumns()) {
        inputBuilder.field(newInputObjectField().name(col.getColumnName()).type(getInputType(col)));
      }
      GraphQLInputObjectType inputType = inputBuilder.build();

      // add as field to query
      queryBuilder
          .field(
              newFieldDefinition()
                  .name(tableMetadata.getTableName())
                  .type(GraphQLList.list(type))
                  .dataFetcher(queryFetcher(table))
                  .argument(
                      GraphQLArgument.newArgument()
                          .name("filter")
                          .type(createFilterType(tableMetadata))
                          .build()))
          .build();

      // add 'save' and 'delecte' fields to mutation
      mutationBuilder.field(
          newFieldDefinition()
              .name("save" + tableMetadata.getTableName())
              .type(GraphQLTypeReference.typeRef(MUTATION_RESULT))
              .dataFetcher(saveFetcher(table))
              .argument(
                  GraphQLArgument.newArgument().name("input").type(GraphQLList.list(inputType))));

      newFieldDefinition()
          .name("delete" + tableMetadata.getTableName())
          .type(GraphQLTypeReference.typeRef(MUTATION_RESULT))
          .dataFetcher(deleteFetcher(table))
          .argument(GraphQLArgument.newArgument().name("input").type(GraphQLList.list(inputType)));
    }

    // assemble and return
    return graphql.schema.GraphQLSchema.newSchema()
        .query(queryBuilder)
        .mutation(mutationBuilder)
        .additionalType(getMutationResultType())
        .additionalType(getMolgenisStringFilter())
        .build();
  }

  private static GraphQLInputType getInputType(Column col) {
    ColumnType type = col.getColumnType();
    if (REF.equals(type)) type = SqlTypeUtils.getRefColumnType(col);
    switch (type) {
      case DECIMAL:
        return Scalars.GraphQLBigDecimal;
      case INT:
        return Scalars.GraphQLInt;
      default:
        return Scalars.GraphQLString;
    }
  }

  private static GraphQLOutputType getType(Column col) {
    switch (col.getColumnType()) {
      case DECIMAL:
        return Scalars.GraphQLBigDecimal;
      case INT:
        return Scalars.GraphQLInt;
      case REF:
        return GraphQLTypeReference.typeRef(col.getRefTableName());
      case REF_ARRAY:
        return GraphQLList.list(GraphQLTypeReference.typeRef(col.getRefTableName()));
      default:
        return Scalars.GraphQLString;
    }
  }

  private static GraphQLObjectType getMutationResultType() {
    return newObject()
        .name(MUTATION_RESULT)
        .field(newFieldDefinition().name("message").type(Scalars.GraphQLString).build())
        .build();
  }

  private static GraphQLInputObjectType createFilterType(TableMetadata table) {
    GraphQLInputObjectType.Builder filterBuilder =
        newInputObject().name(table.getTableName() + "Filter");
    for (Column col : table.getColumns()) {
      filterBuilder.field(createFilterType(col));
    }
    return filterBuilder.build();
  }

  private static GraphQLInputObjectField createFilterType(Column col) {
    // colname : { "eq":[string], "range": [num,num,...,...], "contains":[string]}
    return newInputObjectField()
        .name(col.getColumnName())
        .type(GraphQLTypeReference.typeRef("MolgenisStringFilter"))
        .build();
  }

  private static GraphQLInputObjectType getMolgenisStringFilter() {
    return newInputObject()
        .name("MolgenisStringFilter")
        .field(newInputObjectField().name("eq").type(GraphQLList.list(Scalars.GraphQLString)))
        .build();
  }

  private static DataFetcher saveFetcher(Table aTable) {
    return dataFetchingEnvironment -> {
      Table table = aTable;
      List<Map<String, Object>> map = dataFetchingEnvironment.getArgument("input");
      return table.update(convertToRows(map));
    };
  }

  private static Iterable<Row> convertToRows(List<Map<String, Object>> map) {
    List<Row> rows = new ArrayList<>();
    for (Map<String, Object> row : map) {
      rows.add(new Row(row));
    }
    return rows;
  }

  private static DataFetcher<?> deleteFetcher(Table table) {
    return dataFetchingEnvironment -> {
      throw new UnsupportedOperationException();
    };
  }

  public static DataFetcher queryFetcher(Table aTable) {
    return dataFetchingEnvironment -> {
      Table table = aTable;
      Object o = dataFetchingEnvironment.getArgument("filter");
      // Query q = createSelect(aTable.query(), "", dataFetchingEnvironment.getSelectionSet());
      SqlJsonQuery q = new SqlJsonQuery(table);
      q.select(createSelect(dataFetchingEnvironment.getSelectionSet()));
      return transform(q.retrieve());
    };
  }

  /** bit unfortunate that we have to convert from json to map and back */
  private static Object transform(String json) throws IOException {
    return new ObjectMapper().readValue(json, List.class);
  }

  /**
   * creates a list like List.of(field1,field2, path1, List.of(pathsubfield1), ...)
   *
   * @param selection
   * @return
   */
  private static List createSelect(DataFetchingFieldSelectionSet selection) {
    ArrayList result = new ArrayList();
    for (SelectedField sf : selection.getFields()) {
      result.add(sf.getName());
      if (sf.getSelectionSet().getFields().size() > 0) {
        result.add(createSelect(sf.getSelectionSet()));
      }
    }
    return result;
  }

  private static Query createSelect(
      Query query, String prefix, DataFetchingFieldSelectionSet selectionSet) {
    for (SelectedField s : selectionSet.getFields()) {
      query.select(prefix + s.getName());
      createSelect(query, prefix + s.getName() + "/", s.getSelectionSet());
    }
    return query;
  }

  private static List<Map<String, Object>> transform(List<Row> rows) {
    List<Map<String, Object>> list = new ArrayList<>();
    for (Row r : rows) {
      Map m = r.getValueMap();
      list.add(r.getValueMap());
    }
    return list;
  }
}
