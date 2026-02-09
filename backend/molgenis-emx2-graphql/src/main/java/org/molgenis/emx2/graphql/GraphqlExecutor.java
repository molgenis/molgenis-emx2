package org.molgenis.emx2.graphql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.parser.ParserOptions;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.molgenis.emx2.*;
import org.molgenis.emx2.json.JsonUtil;
import org.molgenis.emx2.tasks.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphqlExecutor {
  private static Logger logger = LoggerFactory.getLogger(GraphqlExecutor.class);
  private GraphQL graphql;
  private Map<String, String> graphqlQueryFragments = new LinkedHashMap<>();

  private GraphqlExecutor() {
    if (ParserOptions.getDefaultParserOptions().getMaxTokens() < 1000000) {
      ParserOptions.setDefaultParserOptions(
          ParserOptions.newParserOptions().maxTokens(1000000).build());
      ParserOptions.setDefaultOperationParserOptions(
          ParserOptions.newParserOptions().maxTokens(1000000).build());
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
    // alternatively, we should change the SQL to result escaped results but that is a nightmare to
    // build
    if (json != null) {
      return new ObjectMapper().readValue(json, Map.class);
    } else {
      return null;
    }
  }

  public GraphqlExecutor(Database database, TaskService taskService) {
    this();
    this.graphql = GraphqlFactory.createGraphqlForDatabase(database, taskService);
  }

  public GraphqlExecutor(Database database) {
    this(database, null);
  }

  public GraphqlExecutor(Schema schema, TaskService taskService) {
    this();
    this.graphql = GraphqlFactory.createGraphqlForSchema(schema, taskService);
    this.graphqlQueryFragments = GraphqlTableFragmentGenerator.generate(schema);
  }

  public GraphqlExecutor(Schema schema) {
    this(schema, null);
  }

  public @NotNull ExecutionResult executeWithoutSession(String query) {
    return execute(query, Map.of(), new DummySessionHandler());
  }

  public @NotNull ExecutionResult executeWithoutSession(
      String query, Map<String, Object> variables) {
    return execute(query, variables, new DummySessionHandler());
  }

  public @NotNull ExecutionResult execute(
      String query, Map<String, Object> variables, GraphqlSessionHandlerInterface sessionManager) {
    long start = System.currentTimeMillis();
    Map<?, Object> graphQLContext =
        sessionManager != null
            ? Map.of(GraphqlSessionHandlerInterface.class, sessionManager)
            : Map.of();

    // we don't log password calls
    if (logger.isInfoEnabled()) {
      if (query.contains("password")) {
        logger.info("query: obfuscated because contains parameter with name 'password'");
      } else {
        logger.info("query: {}", query.replaceAll("[\n|\r|\t]", "").replaceAll(" +", " "));
      }
    }

    // we add fragments if contains "..."
    if (query.contains("...")) {
      Pattern fragmentPattern = Pattern.compile("\\.\\.\\.\\s*([A-Za-z_][A-Za-z0-9_]*)");
      Matcher matcher = fragmentPattern.matcher(query);
      while (matcher.find()) {
        String name = matcher.group(1);
        if (graphqlQueryFragments.get(name) == null) {
          throw new MolgenisException("Graphql fragment not found: " + name);
        }
        query = graphqlQueryFragments.get(name) + "\n" + query;
      }
    }

    // tests show overhead of this step is about 20ms (jooq takes the rest)
    ExecutionResult executionResult = null;
    if (variables != null) {
      executionResult =
          graphql.execute(
              ExecutionInput.newExecutionInput(query)
                  .graphQLContext(graphQLContext)
                  .variables(variables)
                  .build());
    } else {
      executionResult =
          graphql.execute(
              ExecutionInput.newExecutionInput(query).graphQLContext(graphQLContext).build());
    }

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

    return executionResult;
  }

  public static class DummySessionHandler implements GraphqlSessionHandlerInterface {
    @Override
    public void createSession(String username) {}

    @Override
    public void destroySession() {}

    @Override
    public String getCurrentUser() {
      return "";
    }
  }
}
