package org.molgenis.emx2.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.Scalars;
import graphql.schema.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.Filter;
import org.molgenis.emx2.sql.SqlGraphJsonQuery;
import org.molgenis.emx2.sql.SqlTypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import static org.molgenis.emx2.sql.Filter.f;
import static spark.Spark.*;

/**
 * Benchmarks show the api part adds about 10-30ms overhead on top of the underlying database call
 */
public class GraphqlApi {
  private static Logger logger = LoggerFactory.getLogger(GraphqlApi.class);

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
    long start = System.currentTimeMillis();

    // todo, invalidate on data changes
    // Schema

    Schema schema =
        MolgenisWebservice.getAuthenticatedDatabase(request)
            .getSchema(request.params(MolgenisWebservice.SCHEMA));

    GraphQLSchema gl = createGraphQLSchema(schema);
    GraphQL g = GraphQL.newGraphQL(gl).build();
    logger.info(
        "todo: create cache schema loading, it takes "
            + (System.currentTimeMillis() - start)
            + "ms");

    // tests show overhead of this step is about 1ms (jooq takes the rest)
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
    logger.info("query\n" + query);

    // tests show overhead of this step is about 20ms (jooq takes the rest)
    ExecutionResult executionResult = g.execute(query);
    for (GraphQLError err : executionResult.getErrors()) {
      System.err.println(err);
    }

    // tests show conversions below is under 3ms
    Map<String, Object> toSpecificationResult = executionResult.toSpecification();
    String result = JsonApi.getWriter().writeValueAsString(toSpecificationResult);

    logger.info("graphql request completed in " + (System.currentTimeMillis() - start) + "ms");

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
                          .build())
                  .argument(
                      GraphQLArgument.newArgument()
                          .name("search")
                          .type(Scalars.GraphQLString)
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
    switch (col.getColumnType()) {
      case REF:
        return newInputObjectField()
            .name(col.getColumnName())
            .type(GraphQLTypeReference.typeRef(col.getRefTableName() + "Filter"))
            .build();
      case REF_ARRAY:
        return newInputObjectField()
            .name(col.getColumnName())
            .type(GraphQLList.list(GraphQLTypeReference.typeRef(col.getRefTableName() + "Filter")))
            .build();
      default:
        return newInputObjectField()
            .name(col.getColumnName())
            .type(GraphQLTypeReference.typeRef("MolgenisStringFilter"))
            .build();
    }
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
      SqlGraphJsonQuery q = new SqlGraphJsonQuery(table);
      q.select(createSelect(dataFetchingEnvironment.getSelectionSet()));
      if (dataFetchingEnvironment.getArgument("filter") != null) {
        q.filter(createFilters(table, dataFetchingEnvironment.getArgument("filter")));
      }
      String search = dataFetchingEnvironment.getArgument("search");
      if (search != null) {
        // todo proper tokenizer
        q.search(search.split(" "));
      }
      return transform(q.retrieve());
    };
  }

  private static Filter[] createFilters(Table table, Map<String, Object> filter) {
    List<Filter> subFilters = new ArrayList<>();
    for (Map.Entry<String, Object> entry : filter.entrySet()) {
      Column c = table.getMetadata().getColumn(entry.getKey());
      if (c == null)
        throw new RuntimeException(
            "Column " + entry.getKey() + " unknown in table " + table.getName());
      switch (c.getColumnType()) {
        case REF:
        case REF_ARRAY:
          subFilters.add(
              f(
                  c.getColumnName(),
                  createFilters(
                      table.getSchema().getTable(c.getRefTableName()), (Map) entry.getValue())));
          break;
        default:
          // expect scalar comparison, todo for all types
          if (entry.getValue() instanceof Map && ((Map) entry.getValue()).containsKey("eq")) {
            ArrayList values = (ArrayList) ((Map) entry.getValue()).get("eq");
            subFilters.add(f(entry.getKey()).eq(values.toArray()));
          } else {
            throw new RuntimeException(
                "unknown filter expression " + entry.getValue() + " for column " + entry.getKey());
          }
      }
    }
    return subFilters.toArray(new Filter[subFilters.size()]);
  }

  /** bit unfortunate that we have to convert from json to map and back */
  private static Object transform(String json) throws IOException {
    // benchmark shows this only takes a few ms so not a performance issue
    if (json != null) {
      return new ObjectMapper().readValue(json, List.class);
    } else {
      return new ArrayList<>();
    }
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
}
