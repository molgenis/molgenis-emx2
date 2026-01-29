package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.Constants.ACCEPT_JSON;
import static org.molgenis.emx2.web.Constants.CONTENT_TYPE;
import static org.molgenis.emx2.web.MolgenisWebservice.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.*;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.graphql.GraphqlApi;
import org.molgenis.emx2.graphql.GraphqlException;
import org.molgenis.emx2.graphql.GraphqlSessionHandlerInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Benchmarks show the api part adds about 10-30ms overhead on top of the underlying database call
 */
public class GraphqlApiService {
  public static final String QUERY = "query";
  public static final String VARIABLES = "variables";
  private static Logger logger = LoggerFactory.getLogger(GraphqlApiService.class);

  private GraphqlApiService() {
    // hide constructor
  }

  public static void create(Javalin app) {

    // per schema graphql calls from app
    final String appSchemaGqlPath = "apps/{app}/{schema}/graphql"; // NOSONAR
    app.get(appSchemaGqlPath, GraphqlApiService::handleSchemaRequests);
    app.post(appSchemaGqlPath, GraphqlApiService::handleSchemaRequests);

    // per schema graphql calls from app
    final String appGqlPath = "apps/{app}/graphql"; // NOSONAR
    app.get(appGqlPath, GraphqlApiService::handleDatabaseRequests);
    app.post(appGqlPath, GraphqlApiService::handleDatabaseRequests);

    // per database graphql
    final String databasePath = "/api/graphql";
    app.get(databasePath, GraphqlApiService::handleDatabaseRequests);
    app.post(databasePath, GraphqlApiService::handleDatabaseRequests);

    // per schema graphql
    final String schemaPath = "/{schema}/graphql"; // NOSONAR
    app.get(schemaPath, GraphqlApiService::handleSchemaRequests);
    app.post(schemaPath, GraphqlApiService::handleSchemaRequests);

    // per schema graphql
    final String schemaAppPath = "/{schema}/{app}/graphql"; // NOSONAR
    app.get(schemaAppPath, GraphqlApiService::handleSchemaRequests);
    app.post(schemaAppPath, GraphqlApiService::handleSchemaRequests);
  }

  private static void handleDatabaseRequests(Context ctx) throws IOException {
    ctx.header(CONTENT_TYPE, ACCEPT_JSON);
    String result = executeQuery(applicationCache.getDatabaseGraphqlForUser(ctx), ctx);
    ctx.json(result);
  }

  public static void handleSchemaRequests(Context ctx) throws IOException {
    String schemaName = sanitize(ctx.pathParam(SCHEMA));

    // apps and api is not a schema but a resource
    if ("apps".equals(schemaName) || "api".equals(schemaName)) {
      handleDatabaseRequests(ctx);
      return;
    }

    // todo, really check permissions
    if (getSchema(ctx) == null) {
      throw new GraphqlException(
          "Schema '" + schemaName + "' unknown. Might you need to sign in or ask permission?");
    }
    GraphqlApi graphqlForSchema = applicationCache.getSchemaGraphqlForUser(schemaName, ctx);
    ctx.header(CONTENT_TYPE, ACCEPT_JSON);
    ctx.json(executeQuery(graphqlForSchema, ctx));
  }

  private static String executeQuery(GraphqlApi graphqlApi, Context ctx) throws IOException {
    String query = getQueryFromRequest(ctx);
    Map<String, Object> variables = getVariablesFromRequest(ctx);
    GraphqlSessionHandlerInterface sessionManager = new MolgenisSessionHandler(ctx.req());

    ExecutionResult executionResult = graphqlApi.execute(query, variables, sessionManager);

    String result = GraphqlApi.convertExecutionResultToJson(executionResult);
    return result;
  }

  private static String getQueryFromRequest(Context ctx) throws IOException {
    String query;
    if (HandlerType.POST.equals(ctx.method())) {
      if (ctx.header("Content-Type").startsWith("multipart/form-data")) {
        File tempFile = File.createTempFile(MolgenisWebservice.TEMPFILES_DELETE_ON_EXIT, ".tmp");
        tempFile.deleteOnExit();
        ctx.attribute(
            "org.eclipse.jetty.multipartConfig",
            new MultipartConfigElement(tempFile.getAbsolutePath()));
        query = ctx.formParam(QUERY);
      } else {
        ObjectNode node = new ObjectMapper().readValue(ctx.body(), ObjectNode.class);
        query = node.get(QUERY).asText();
      }
    } else {
      if (ctx.queryParam(QUERY) != null) {
        query = ctx.queryParam(QUERY);
      } else {
        query =
            "{\n" + "  __schema {\n" + "    types {\n" + "      name\n" + "    }\n" + "  }\n" + "}";
      }
    }
    return query;
  }

  private static Map<String, Object> getVariablesFromRequest(Context ctx) {
    if (HandlerType.POST.equals(ctx.method())) {
      try {
        if (ctx.header("Content-Type").startsWith("multipart/form-data")) {
          String variableString = ctx.formParam(VARIABLES);
          Map<String, Object> variables = new ObjectMapper().readValue(variableString, Map.class);
          // now replace each part id with the part
          putPartsIntoMap(
              variables,
              ctx.req().getParts().stream()
                  .filter(p -> !p.getName().equals(VARIABLES) && !p.getName().equals(QUERY))
                  .collect(Collectors.toList()));
          //
          return variables;
        } else {
          Map<String, Object> node = new ObjectMapper().readValue(ctx.body(), Map.class);
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
