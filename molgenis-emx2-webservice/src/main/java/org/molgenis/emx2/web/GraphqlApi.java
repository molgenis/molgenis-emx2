package org.molgenis.emx2.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.Scalars;
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
import static org.molgenis.emx2.web.Constants.DETAIL;
import static org.molgenis.emx2.web.GraphqlDatabaseFields.*;
import static org.molgenis.emx2.web.GraphqlTableMetadataFields.*;
import static org.molgenis.emx2.web.GraphqlTableFields.tableMutationField;
import static org.molgenis.emx2.web.GraphqlTableFields.tableQueryField;
import static org.molgenis.emx2.web.GraphqlUserFields.*;

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
    get(schemaPath, GraphqlApi::handleSchemaRequests);
    post(schemaPath, GraphqlApi::handleSchemaRequests);

    // small overall graphql
    get("/api/graphql", GraphqlApi::handleDatabaseRequests);
    post("/api/graphql", GraphqlApi::handleDatabaseRequests);
  }

  private static String handleDatabaseRequests(Request request, Response response)
      throws IOException {
    Database database = MolgenisWebservice.getAuthenticatedDatabase(request);
    String result = executeQuery(graphqlForDatabase(database), request);
    handleSessions(request, database);
    return result;
  }

  private static String handleSchemaRequests(Request request, Response response)
      throws IOException {
    Database database = MolgenisWebservice.getAuthenticatedDatabase(request);
    Schema schema = database.getSchema(request.params(MolgenisWebservice.SCHEMA));
    String result = executeQuery(graphqlForSchema(schema), getQueryFromRequest(request));
    handleSessions(request, database);
    return result;
  }

  private static void handleSessions(Request request, Database database) {
    // check if we need to put in session because user logged in
    if (database.getActiveUser() != null && request.session() == null) {
      request.session(true);
      request.session().attribute("database", database);
    }
    // check if we should remove the session because user logged out
    if (database.getActiveUser() == null && request.session() != null) {
      request.session(false);
    }
  }

  static GraphQL graphqlForDatabase(Database database) {
    GraphQLObjectType.Builder queryBuilder = newObject().name("Query");
    GraphQLObjectType.Builder mutationBuilder = newObject().name("Save");

    // add login
    queryBuilder.field(userQueryField(database));
    mutationBuilder.field(loginField(database));
    mutationBuilder.field(logoutField(database));
    mutationBuilder.field(registerField(database));

    queryBuilder.field(querySchemasField(database));
    mutationBuilder.field(createSchemaField(database));
    mutationBuilder.field(deleteSchemaField(database));

    return GraphQL.newGraphQL(
            GraphQLSchema.newSchema().query(queryBuilder).mutation(mutationBuilder).build())
        .build();
  }

  public static GraphQL graphqlForSchema(Schema schema) {
    long start = System.currentTimeMillis();
    GraphQLObjectType.Builder queryBuilder = newObject().name("Query");
    GraphQLObjectType.Builder mutationBuilder = newObject().name("Save");

    // add login
    queryBuilder.field(userQueryField(schema.getDatabase()));
    mutationBuilder.field(loginField(schema.getDatabase()));
    mutationBuilder.field(logoutField(schema.getDatabase()));
    mutationBuilder.field(registerField(schema.getDatabase()));

    // add meta query, if member
    String role = schema.getRoleForUser(schema.getDatabase().getActiveUser());
    boolean isAdmin = "admin".equals(schema.getDatabase().getActiveUser());
    if (role != null || isAdmin) {
      queryBuilder.field(tableMetatadataQueryField(schema));
      // add meta query, if manager
      if ((role != null && role.contains("Manager")) || isAdmin) {
        mutationBuilder.field(tableMetadataMutationField(schema));
      }
    }

    // add query and mutation for each table
    for (String tableName : schema.getTableNames()) {
      Table table = schema.getTable(tableName);
      queryBuilder.field(tableQueryField(table));
      mutationBuilder.field(tableMutationField(table));
    }

    // assemble and return
    GraphQL result =
        GraphQL.newGraphQL(
                GraphQLSchema.newSchema().query(queryBuilder).mutation(mutationBuilder).build())
            .build();

    // log timing so we dont forget to add caching later
    if (logger.isInfoEnabled())
      logger.info(
          "todo: create cache schema loading, it takes {}ms", (System.currentTimeMillis() - start));

    return result;
  }

  private static String executeQuery(GraphQL g, Request request) throws IOException {
    return executeQuery(g, getQueryFromRequest(request));
  }

  private static String executeQuery(GraphQL g, String query) throws IOException {
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

  public static final GraphQLObjectType typeForMutationResult =
      newObject()
          .name("MolgenisMessage")
          .field(newFieldDefinition().name("type").type(Scalars.GraphQLString).build())
          .field(newFieldDefinition().name("title").type(Scalars.GraphQLString).build())
          .field(newFieldDefinition().name(DETAIL).type(Scalars.GraphQLString).build())
          .build();
}
