package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.Constants.ACCEPT_JSON;
import static org.molgenis.emx2.web.Constants.CONTENT_TYPE;
import static org.molgenis.emx2.web.MolgenisWebservice.*;
import static spark.Spark.get;
import static spark.Spark.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.graphql.GraphqlApiFactory;
import org.molgenis.emx2.graphql.GraphqlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

/**
 * Benchmarks show the api part adds about 10-30ms overhead on top of the underlying database call
 */
public class GraphqlApi {
  public static final String QUERY = "query";
  public static final String VARIABLES = "variables";
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
    final String schemaPath = "/:schema/graphql"; // NOSONAR
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

    // apps and api is not a schema but a resource
    if ("apps".equals(schemaName) || "api".equals(schemaName)) {
      return handleDatabaseRequests(request, response);
    }

    // todo, really check permissions
    if (getSchema(request) == null) {
      throw new GraphqlException(
          "Schema '" + schemaName + "' unknown. Might you need to sign in or ask permission?");
    }
    GraphQL graphqlForSchema = session.getGraphqlForSchema(schemaName);
    response.header(CONTENT_TYPE, ACCEPT_JSON);
    return executeQuery(graphqlForSchema, request);
  }

  private static String executeQuery(GraphQL g, Request request) throws IOException {
    String query = getQueryFromRequest(request);
    Map<String, Object> variables = getVariablesFromRequest(request);

    long start = System.currentTimeMillis();

    // we don't log password calls
    if (logger.isInfoEnabled()) {
      if (query.contains("password")) {
        logger.info("query: obfuscated because contains parameter with name 'password'");
      } else {
        logger.info("query: {}", query.replaceAll("[\n|\r|\t]", "").replaceAll(" +", " "));
      }
    }

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
      throw new MolgenisException(executionResult.getErrors().get(0).getMessage());
    }

    if (logger.isInfoEnabled())
      logger.info("graphql request completed in {}ms", +(System.currentTimeMillis() - start));

    return result;
  }

  private static String getQueryFromRequest(Request request) throws IOException {
    String query = null;
    if ("POST".equals(request.requestMethod())) {
      if (request.headers("Content-Type").startsWith("multipart/form-data")) {
        File tempFile = File.createTempFile(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT, ".tmp");
        tempFile.deleteOnExit();
        request.attribute(
            "org.eclipse.jetty.multipartConfig",
            new MultipartConfigElement(tempFile.getAbsolutePath()));
        query = request.queryParams(QUERY);
      } else {
        ObjectNode node = new ObjectMapper().readValue(request.body(), ObjectNode.class);
        query = node.get(QUERY).asText();
      }
    } else {
      query =
          request.queryParamOrDefault(
              QUERY,
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

  private static Map<String, Object> getVariablesFromRequest(Request request) {
    if ("POST".equals(request.requestMethod())) {
      try {
        if (request.headers("Content-Type").startsWith("multipart/form-data")) {
          Map<String, Object> variables =
              new ObjectMapper().readValue(request.queryParams(VARIABLES), Map.class);
          // now replace each part id with the part
          putPartsIntoMap(
              variables,
              request.raw().getParts().stream()
                  .filter(p -> !p.getName().equals(VARIABLES) && !p.getName().equals(QUERY))
                  .collect(Collectors.toList()));
          //
          return variables;
        } else {
          Map<String, Object> node = new ObjectMapper().readValue(request.body(), Map.class);
          return (Map<String, Object>) node.get(VARIABLES);
        }
      } catch (Exception e) {
        throw new MolgenisException(
            "Parsing of graphql variables failed. Should be an object with each graphql variable a key. "
                + e.getMessage(),
            e);
      }
    }
    return null;
  }

  private static void putPartsIntoMap(Map<String, Object> variables, Collection<Part> parts) {
    // check the part links
    for (Map.Entry<String, Object> entry : variables.entrySet()) {
      if (entry.getValue() instanceof String) {
        for (Part part : parts) {
          if (part.getName().equals(entry.getValue())) {
            entry.setValue(part);
          }
        }
      } else if (entry.getValue() instanceof Map) {
        putPartsIntoMap((java.util.Map<String, Object>) entry.getValue(), parts);
      } else if (entry.getValue() instanceof List) {
        for (Object element : (List) entry.getValue()) {
          if (element instanceof Map) {
            putPartsIntoMap((java.util.Map<String, Object>) element, parts);
          }
        }
      }
    }
  }
}
