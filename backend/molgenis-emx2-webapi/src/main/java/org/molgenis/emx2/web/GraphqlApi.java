package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.Constants.ACCEPT_JSON;
import static org.molgenis.emx2.web.Constants.CONTENT_TYPE;
import static org.molgenis.emx2.web.MolgenisWebservice.*;
import static org.molgenis.emx2.web.MolgenisWebservice.sessionManager;
import static org.molgenis.emx2.web.util.ContextHelpers.findUsedAuthTokenKey;
import static org.molgenis.emx2.web.util.ContextHelpers.hasAuthTokenSet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.graphql.EMX2GraphQL;
import org.molgenis.emx2.graphql.GraphqlApiFactory;
import org.molgenis.emx2.graphql.GraphqlException;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.tasks.ScriptTableListener;
import org.molgenis.emx2.tasks.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Benchmarks show the api part adds about 10-30ms overhead on top of the underlying database call
 */
public class GraphqlApi {
  public static final String QUERY = "query";
  public static final String VARIABLES = "variables";
  private static final String ANONYMOUS = "anonymous";
  private static Logger logger = LoggerFactory.getLogger(GraphqlApi.class);
  private static MolgenisSessionManager sessionManager;
  private static final TaskService taskService = TaskApi.taskService;

  private GraphqlApi() {
    // hide constructor
  }

  public static void createGraphQLservice(Javalin app, MolgenisSessionManager sm) {
    sessionManager = sm;

    // per schema graphql calls from app
    final String appSchemaGqlPath = "apps/{app}/{schema}/graphql"; // NOSONAR
    app.get(appSchemaGqlPath, GraphqlApi::handleSchemaRequests);
    app.post(appSchemaGqlPath, GraphqlApi::handleSchemaRequests);

    // per schema graphql calls from app
    final String appGqlPath = "apps/{app}/graphql"; // NOSONAR
    app.get(appGqlPath, GraphqlApi::handleDatabaseRequests);
    app.post(appGqlPath, GraphqlApi::handleDatabaseRequests);

    // per database graphql
    final String databasePath = "/api/graphql";
    app.get(databasePath, GraphqlApi::handleDatabaseRequests);
    app.post(databasePath, GraphqlApi::handleDatabaseRequests);

    // per schema graphql
    final String schemaPath = "/{schema}/graphql"; // NOSONAR
    app.get(schemaPath, GraphqlApi::handleSchemaRequests);
    app.post(schemaPath, GraphqlApi::handleSchemaRequests);

    // per schema graphql
    final String schemaAppPath = "/{schema}/{app}/graphql"; // NOSONAR
    app.get(schemaAppPath, GraphqlApi::handleSchemaRequests);
    app.post(schemaAppPath, GraphqlApi::handleSchemaRequests);
  }

  private static void handleDatabaseRequests(Context ctx) throws IOException {
    MolgenisSession session = findSessionForContext(ctx);

    EMX2GraphQL emx2GraphQL;
    if (session == null) {
      emx2GraphQL = new GraphqlApiFactory().createGraphql(ANONYMOUS, taskService);

    } else {
      String sessionUser = session.getSessionUser();
      emx2GraphQL = session.getGraphqlForDatabase(sessionUser, taskService);
    }
    ctx.header(CONTENT_TYPE, ACCEPT_JSON);
    String result = executeQuery(emx2GraphQL, ctx);
    ctx.json(result);
  }

  public static void handleSchemaRequests(Context ctx) throws IOException {
    MolgenisSession session = findSessionForContext(ctx);
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

    EMX2GraphQL graphqlForSchema;
    if (session == null) {
      SqlDatabase database = new SqlDatabase(ANONYMOUS);
      database.addTableListener(new ScriptTableListener(TaskApi.taskSchedulerService));
      Schema schema = database.getSchema(schemaName);
      AnonymousGqlSchemaCache anonymousGqlSchemaCache = AnonymousGqlSchemaCache.getInstance();
      graphqlForSchema = new EMX2GraphQL(database, anonymousGqlSchemaCache.get(schema));
    } else {
      graphqlForSchema = session.getGraphqlForSchema(schemaName);
    }

    ctx.header(CONTENT_TYPE, ACCEPT_JSON);
    ctx.json(executeQuery(graphqlForSchema, ctx));
  }

  private static MolgenisSession findSessionForContext(Context ctx) {
    HttpServletRequest request = ctx.req();

    if (hasAuthTokenSet(request)) {
      // session based on token, not persisted
      String authTokenKey = findUsedAuthTokenKey(request);
      return sessionManager.getNonPersistedSessionBasedOnToken(request, authTokenKey);
    } else if (request.getSession(false) != null) {
      // persistent session based on session id
      return sessionManager.getSession(request);
    } else {
      return null;
    }
  }

  private static String executeQuery(EMX2GraphQL eg, Context ctx) throws IOException {
    String query = getQueryFromRequest(ctx);
    Map<String, Object> variables = getVariablesFromRequest(ctx);

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
    GraphQL g = eg.getGraphQL();
    String userBeforeExecute = eg.getDatabase().getActiveUser();
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
    if (!executionResult.getErrors().isEmpty()) {
      throw new MolgenisException(executionResult.getErrors().getFirst().getMessage());
    }

    String userAfterExecute = eg.getDatabase().getActiveUser();
    if (!userBeforeExecute.equals(userAfterExecute)) {
      if (ctx.req().getSession(false) != null) {
        // do logout
        logger.info("do logout ");
        HttpSession session = ctx.req().getSession();
        sessionManager.removeSession(session.getId());
      } else {
        // create http session
        HttpSession session = ctx.req().getSession();
        // store db, session combination in session manager
        sessionManager.addSession(session, eg.getDatabase());
      }
    }

    if (logger.isInfoEnabled())
      logger.info("graphql request completed in {}ms", +(System.currentTimeMillis() - start));

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
