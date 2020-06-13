package org.molgenis.emx2.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import org.molgenis.emx2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.*;
import org.molgenis.emx2.graphql.GraphqlApiFactory;

import static org.molgenis.emx2.web.Constants.*;
import static org.molgenis.emx2.web.MolgenisWebservice.*;
import static spark.Spark.*;

/**
 * Benchmarks show the api part adds about 10-30ms overhead on top of the underlying database call
 */
public class GraphqlApi {
  private static Logger logger = LoggerFactory.getLogger(GraphqlApi.class);
  private static MolgenisSessionManager sessionManager;

  private GraphqlApi() {
    // hide constructor
  }

  public static void createGraphQLservice(MolgenisSessionManager sm) {
    sessionManager = sm;

    // per database graphql
    final String databasePath = "/api/graphql";
    get(databasePath, GraphqlApi::handleDatabaseRequests);
    post(databasePath, GraphqlApi::handleDatabaseRequests);

    // per schema graphql
    final String schemaPath = "/api/graphql/:schema"; // NOSONAR
    get(schemaPath, GraphqlApi::handleSchemaRequests);
    post(schemaPath, GraphqlApi::handleSchemaRequests);
  }

  private static String handleDatabaseRequests(Request request, Response response)
      throws IOException {
    MolgenisSession session = sessionManager.getSession(request);
    response.header(CONTENT_TYPE, ACCEPT_JSON);
    return executeQuery(session.getGraphqlForDatabase(), request);
  }

  public static String handleSchemaRequests(Request request, Response response) throws IOException {
    MolgenisSession session = sessionManager.getSession(request);
    String schemaName = sanitize(request.params(SCHEMA));

    // apps is not a schema but a resource
    if ("apps".equals(schemaName)) {
      return handleDatabaseRequests(request, response);
    }

    // todo, really check permissions
    if (getSchema(request) == null) {
      response.status(403);
      return "{\"errors\":[{\"message\":\"Schema '"
          + schemaName
          + "' not found or permission denied.\"}]}";
    }

    GraphQL graphqlForSchema = session.getGraphqlForSchema(schemaName);
    response.header(CONTENT_TYPE, ACCEPT_JSON);
    return executeQuery(graphqlForSchema, request);
  }

  private static Map<String, Object> getVariablesFromRequest(Request request) {
    if ("POST".equals(request.requestMethod())) {
      try {
        Map<String, Object> node = new ObjectMapper().readValue(request.body(), Map.class);
        return (Map<String, Object>) node.get("variables");
      } catch (Exception e) {
        throw new MolgenisException(
            "Parsing of grahpql variables failed. Should be an object with each graphql variable a key. "
                + e.getMessage(),
            e);
      }
    }
    return null;
  }

  private static String executeQuery(GraphQL g, Request request) throws IOException {

    String query = getQueryFromRequest(request);
    Map<String, Object> variables = getVariablesFromRequest(request);

    long start = System.currentTimeMillis();

    if (logger.isInfoEnabled())
      logger.info("query: {}", query.replaceAll("[\n|\r|\t]", "").replaceAll(" +", " "));

    // tests show overhead of this step is about 20ms (jooq takes the rest)
    ExecutionResult executionResult = null;
    if (variables != null) {
      executionResult = g.execute(ExecutionInput.newExecutionInput(query).variables(variables));
    } else {
      executionResult = g.execute(query);
    }

    String result = GraphqlApiFactory.convertExecutionResultToJson(executionResult);

    for (GraphQLError err : executionResult.getErrors()) {
      if (logger.isErrorEnabled()) {
        logger.error(err.getMessage());
      }
    }
    if (executionResult.getErrors().size() > 0) {
      throw new MolgenisException("Error", executionResult.getErrors().get(0).getMessage());
    }

    if (logger.isInfoEnabled())
      logger.info("graphql request completed in {}ms", +(System.currentTimeMillis() - start));

    return result;
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

  static Map<String, String> resultMessage(String detail) {
    Map<String, String> message = new LinkedHashMap<>();
    message.put(DETAIL, detail);
    return message;
  }
}
