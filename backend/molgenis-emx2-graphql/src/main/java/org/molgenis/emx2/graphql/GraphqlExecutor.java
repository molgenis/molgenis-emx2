package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.Privileges.VIEWER;
import static org.molgenis.emx2.jsonld.JsonLdSchemaGenerator.generateJsonLdSchema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.execution.AsyncExecutionStrategy;
import graphql.parser.ParserOptions;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.molgenis.emx2.*;
import org.molgenis.emx2.json.JsonUtil;
import org.molgenis.emx2.tasks.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphqlExecutor {
  private static Logger logger = LoggerFactory.getLogger(GraphqlExecutor.class);
  private final GraphQL graphql;
  private Schema schema;
  private Database database;
  private Map<String, String> graphqlQueryFragments = new LinkedHashMap<>();

  private GraphqlExecutor(GraphQL graphql) {
    this.graphql = graphql;
    if (ParserOptions.getDefaultParserOptions().getMaxTokens() < 1000000) {
      ParserOptions.setDefaultParserOptions(
          ParserOptions.newParserOptions().maxTokens(1000000).build());
      ParserOptions.setDefaultOperationParserOptions(
          ParserOptions.newParserOptions().maxTokens(1000000).build());
    }
  }

  private GraphqlExecutor(GraphQL graphql, Map<String, String> fragments) {
    this.graphql = graphql;
    this.graphqlQueryFragments = fragments;
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
    this(createGraphqlForDatabase(database, taskService));
    this.database = database;
  }

  public GraphqlExecutor(Database database) {
    this(database, null);
  }

  public GraphqlExecutor(Schema schema, TaskService taskService) {
    this(createGraphqlForSchemaWithFragments(schema, taskService));
    this.schema = schema;
  }

  public GraphqlExecutor(Schema schema) {
    this(schema, null);
  }

  private static GraphqlResult createGraphqlForSchemaWithFragments(
      Schema schema, TaskService taskService) {
    Map<String, String> fragments = new LinkedHashMap<>();
    GraphQL graphql = createGraphqlForSchema(schema, taskService, fragments);
    return new GraphqlResult(graphql, fragments);
  }

  private GraphqlExecutor(GraphqlResult result) {
    this(result.graphql, result.fragments);
  }

  private static class GraphqlResult {
    final GraphQL graphql;
    final Map<String, String> fragments;

    GraphqlResult(GraphQL graphql, Map<String, String> fragments) {
      this.graphql = graphql;
      this.fragments = fragments;
    }
  }

  private static GraphQL createGraphqlForDatabase(Database database, TaskService taskService) {

    GraphQLObjectType.Builder queryBuilder = GraphQLObjectType.newObject().name("Query");
    GraphQLObjectType.Builder mutationBuilder = GraphQLObjectType.newObject().name("Save");

    // add login
    // all the same between schemas
    queryBuilder.field(new GraphqlManifesFieldFactory().queryVersionField(database));

    // admin operations
    if (database.isAdmin()) {
      queryBuilder.field(GraphqlAdminFieldFactory.queryAdminField(database));
      mutationBuilder.field(GraphqlAdminFieldFactory.removeUser(database));
      mutationBuilder.field(GraphqlAdminFieldFactory.setEnabledUser(database));
      mutationBuilder.field(GraphqlAdminFieldFactory.updateUser(database));
    }

    // database operations
    GraphqlDatabaseFieldFactory db = new GraphqlDatabaseFieldFactory();
    queryBuilder.field(db.schemasQuery(database));
    queryBuilder.field(db.settingsQueryField(database));
    queryBuilder.field(db.tasksQueryField(taskService));
    // todo need to allow for owner ? ( need to filter the query to include only owned schema's)
    if (database.isAdmin()) {
      queryBuilder.field(db.lastUpdateQuery(database));
    }

    mutationBuilder.field(db.createMutation(database, taskService));
    mutationBuilder.field(db.deleteMutation(database));
    mutationBuilder.field(db.updateMutation(database));
    mutationBuilder.field(db.dropMutation(database));
    mutationBuilder.field(db.changeMutation(database));

    // account operations
    GraphqlSessionFieldFactory session = new GraphqlSessionFieldFactory();
    queryBuilder.field(session.sessionQueryField(database, null));
    mutationBuilder.field(session.signinField(database));
    mutationBuilder.field(session.signupField(database));
    if (!database.isAnonymous()) {
      mutationBuilder.field(session.signoutField(database));
      mutationBuilder.field(session.changePasswordField(database));
      mutationBuilder.field(session.createTokenField(database));
    }

    // notice we here add custom exception handler for mutations
    return GraphQL.newGraphQL(
            GraphQLSchema.newSchema().query(queryBuilder).mutation(mutationBuilder).build())
        .mutationExecutionStrategy(new AsyncExecutionStrategy(new GraphqlCustomExceptionHandler()))
        .build();
  }

  private static GraphQL createGraphqlForSchema(
      Schema schema, TaskService taskService, Map<String, String> graphqlQueryFragments) {
    long start = System.currentTimeMillis();
    logger.info("creating graphql for schema: {}", schema.getMetadata().getName());

    GraphQLObjectType.Builder queryBuilder = GraphQLObjectType.newObject().name("Query");
    GraphQLObjectType.Builder mutationBuilder = GraphQLObjectType.newObject().name("Save");

    // _manifest query
    queryBuilder.field(new GraphqlManifesFieldFactory().queryVersionField(schema.getDatabase()));

    // _schema query
    GraphqlSchemaFieldFactory schemaFields = new GraphqlSchemaFieldFactory();
    queryBuilder.field(schemaFields.schemaQuery(schema));
    queryBuilder.field(schemaFields.settingsQuery(schema));
    queryBuilder.field(schemaFields.schemaReportsField(schema));

    // _tasks query
    GraphqlDatabaseFieldFactory db = new GraphqlDatabaseFieldFactory();
    queryBuilder.field(db.tasksQueryField(taskService));

    // _session query
    GraphqlSessionFieldFactory sessionFieldFactory = new GraphqlSessionFieldFactory();
    queryBuilder.field(sessionFieldFactory.sessionQueryField(schema.getDatabase(), schema));
    mutationBuilder.field(sessionFieldFactory.signinField(schema.getDatabase()));
    mutationBuilder.field(sessionFieldFactory.signupField(schema.getDatabase()));

    // authenticated user operations
    if (!schema.getDatabase().isAnonymous()) {
      mutationBuilder.field(sessionFieldFactory.signoutField(schema.getDatabase()));
      mutationBuilder.field(sessionFieldFactory.changePasswordField(schema.getDatabase()));
      mutationBuilder.field(sessionFieldFactory.createTokenField(schema.getDatabase()));
    }

    mutationBuilder.field(schemaFields.changeMutation(schema));
    mutationBuilder.field(schemaFields.dropMutation(schema));
    mutationBuilder.field(schemaFields.truncateMutation(schema, taskService));

    if ((schema.getRoleForActiveUser() != null
            && schema.getRoleForActiveUser().equals(Privileges.MANAGER.toString()))
        || schema.getDatabase().isAdmin()) {
      queryBuilder.field(schemaFields.changeLogQuery(schema));
      queryBuilder.field(schemaFields.changeLogCountQuery(schema));
    }

    // table
    GraphqlTableFieldFactory tableField = new GraphqlTableFieldFactory(schema);
    for (TableMetadata table : schema.getMetadata().getTables()) {
      if (table.getColumns().size() > 0) {
        if (table.getTableType().equals(TableType.ONTOLOGIES)
            || schema.getInheritedRolesForActiveUser().contains(VIEWER.toString())) {
          queryBuilder.field(tableField.tableQueryField(table));
        }
        queryBuilder.field(tableField.tableAggField(table));
        queryBuilder.field(tableField.tableGroupByField(table));
      }
      graphqlQueryFragments.put(
          "All" + table.getIdentifier() + "Fields", tableField.getGraphqlFragments(table));
    }
    mutationBuilder.field(tableField.insertMutation(schema));
    mutationBuilder.field(tableField.updateMutation(schema));
    mutationBuilder.field(tableField.upsertMutation(schema));
    mutationBuilder.field(tableField.deleteMutation(schema));

    // assemble and return
    GraphQL result =
        GraphQL.newGraphQL(
                GraphQLSchema.newSchema().query(queryBuilder).mutation(mutationBuilder).build())
            .mutationExecutionStrategy(
                new AsyncExecutionStrategy(new GraphqlCustomExceptionHandler()))
            .build();

    if (logger.isInfoEnabled()) {
      logger.info(
          "creation graphql for schema: {} completed in {}ms",
          schema.getMetadata().getName(),
          (System.currentTimeMillis() - start));
    }

    return result;
  }

  @NotNull
  public ExecutionResult execute(String query) {
    return execute(query, Map.of(), new DummySessionHandler());
  }

  public @NotNull ExecutionResult execute(String query, Map<String, Object> variables) {
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

    // we add fragments if containst "..."
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

  public Schema getSchema() {
    return this.schema;
  }

  public String queryAsString(String query, Map<String, Object> variables) {
    try {
      ExecutionResult result = execute(query, variables);
      return convertExecutionResultToJson(result);
    } catch (Exception e) {
      throw new MolgenisException(e.getMessage(), e);
    }
  }

  public Map queryAsMap(String query, Map<String, Object> variables) {
    try {
      ExecutionResult result = execute(query, variables);
      return result.getData();
    } catch (Exception e) {
      throw new MolgenisException(e.getMessage(), e);
    }
  }

  public String getSelectAllQuery() {
    String query =
        this.getSchema().getMetadata().getTables().stream()
            .map(
                table ->
                    String.format(
                        "%s{...All%sFields}", table.getIdentifier(), table.getIdentifier()))
            .collect(Collectors.joining("\n"));
    query = "{" + query + "}";
    return query;
  }

  public String getJsonLdSchema(String schemaUrl) {
    return generateJsonLdSchema(schema.getMetadata(), schemaUrl);
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
