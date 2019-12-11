package org.molgenis.emx2.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.schema.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.MolgenisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.*;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;
import static org.molgenis.emx2.web.GraphqlMetadataApi.*;
import static org.molgenis.emx2.web.GraphqlTableApi.createTableMutationField;
import static org.molgenis.emx2.web.GraphqlTableApi.createTableQueryField;
import static org.molgenis.emx2.web.GraphqlTypes.*;
import static spark.Spark.*;

/**
 * Benchmarks show the api part adds about 10-30ms overhead on top of the underlying database call
 */
class GraphqlApi {
  private static Logger logger = LoggerFactory.getLogger(GraphqlApi.class);

  private GraphqlApi() {
    // hide constructor
  }

  static void createGraphQLservice() {
    // schema level operations
    final String schemaPath = "/api/graphql/:schema"; // NOSONAR

    // per schema grapql
    get(schemaPath, GraphqlApi::getQuery);
    post(schemaPath, GraphqlApi::getQuery);

    // small overall graphql
    get("/api/graphql", GraphqlApi::getBaseQuery);
    post("/api/graphql", GraphqlApi::getBaseQuery);

    // user api
    get("/api/user/graphql", GraphqlApi::getUserQuery);
    post("/api/user/graphql", GraphqlApi::getUserQuery);
  }

  private static Object getUserQuery(Request request, Response response) throws IOException {
    Database database = MolgenisWebservice.getAuthenticatedDatabase(request);
    return convertExecutionResultToJson(
        new GraphqlUserApi(database).execute(getQueryFromRequest(request), request));
  }

  private static String getBaseQuery(Request request, Response response) throws IOException {
    Database database = MolgenisWebservice.getAuthenticatedDatabase(request);
    return convertExecutionResultToJson(
        new GraphqlApiForDatabase(database).execute(getQueryFromRequest(request)));
  }

  // base schema is always the same. Crazy code but seems best way to do this code first

  private static String getQuery(Request request, Response response) throws IOException {

    long start = System.currentTimeMillis();
    Schema schema =
        MolgenisWebservice.getAuthenticatedDatabase(request)
            .getSchema(request.params(MolgenisWebservice.SCHEMA));

    GraphQLSchema gl = createGraphQLSchema(schema);
    GraphQL g = GraphQL.newGraphQL(gl).build();
    if (logger.isInfoEnabled())
      logger.info(
          "todo: create cache schema loading, it takes {}ms", (System.currentTimeMillis() - start));

    start = System.currentTimeMillis();

    // tests show overhead of this step is about 1ms (jooq takes the rest)
    return executeQuery(g, getQueryFromRequest(request));
  }

  private static String executeQuery(GraphQL g, String query) throws JsonProcessingException {
    long start = System.currentTimeMillis();

    if (logger.isInfoEnabled())
      logger.info("query: {}", query.replaceAll("[\n|\r|\t]", "").replaceAll(" +", " "));

    // tests show overhead of this step is about 20ms (jooq takes the rest)
    ExecutionResult executionResult = g.execute(query);
    for (GraphQLError err : executionResult.getErrors()) {
      logger.error(err.toString());
    }
    String result = convertExecutionResultToJson(executionResult);

    if (logger.isInfoEnabled())
      logger.info("graphql request completed in {}ms", +(System.currentTimeMillis() - start));

    return result;
  }

  static String convertExecutionResultToJson(ExecutionResult executionResult)
      throws JsonProcessingException {
    // tests show conversions below is under 3ms
    Map<String, Object> toSpecificationResult = executionResult.toSpecification();
    return JsonApi.getWriter().writeValueAsString(toSpecificationResult);
  }

  private static String getQueryFromRequest(Request request) throws IOException {
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
    return query;
  }

  static GraphQLSchema createGraphQLSchema(Schema model) {
    GraphQLObjectType.Builder queryBuilder = newObject().name("Query");
    GraphQLObjectType.Builder mutationBuilder = newObject().name("Save");
    GraphQLObjectType mutationResultType = GraphqlTypes.typeForMutationResult;

    // add query and mutation for each table
    for (String tableName : model.getTableNames()) {
      Table table = model.getTable(tableName);
      queryBuilder.field(createTableQueryField(table));
      mutationBuilder.field(createTableMutationField(table));
    }

    // add meta query and mutation
    queryBuilder.field(createMetadataQueryField(model));
    mutationBuilder.field(createMetadataMutationField(model));

    // assemble and return
    return graphql.schema.GraphQLSchema.newSchema()
        .query(queryBuilder)
        .mutation(mutationBuilder)
        .build();
  }

  static Object transform(MolgenisException e) {
    Map<String, String> result = new LinkedHashMap<>();
    result.put("title", e.getTitle());
    result.put("type", e.getType());
    result.put(DETAIL, e.getDetail());
    return result;
  }

  static Map<String, String> resultMessage(String detail) {
    Map<String, String> message = new LinkedHashMap<>();
    message.put(DETAIL, detail);
    return message;
  }

  static Iterable<Row> convertToRows(List<Map<String, Object>> map) {
    List<Row> rows = new ArrayList<>();
    for (Map<String, Object> row : map) {
      rows.add(new Row(row));
    }
    return rows;
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
