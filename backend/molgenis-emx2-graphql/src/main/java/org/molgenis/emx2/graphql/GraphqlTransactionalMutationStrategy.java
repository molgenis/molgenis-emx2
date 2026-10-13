package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.graphql.GraphqlConstants.CHANGE;
import static org.molgenis.emx2.graphql.GraphqlConstants.DELETE;
import static org.molgenis.emx2.graphql.GraphqlConstants.DROP;
import static org.molgenis.emx2.graphql.GraphqlConstants.INSERT;
import static org.molgenis.emx2.graphql.GraphqlConstants.SAVE;
import static org.molgenis.emx2.graphql.GraphqlConstants.TRUNCATE;
import static org.molgenis.emx2.graphql.GraphqlConstants.UPDATE;
import static org.molgenis.emx2.graphql.GraphqlTransactionalMutationStrategy.Phase.DATA;
import static org.molgenis.emx2.graphql.GraphqlTransactionalMutationStrategy.Phase.SCHEMA;

import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphqlErrorBuilder;
import graphql.execution.AsyncSerialExecutionStrategy;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
import graphql.execution.MergedField;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.SqlSchemaMetadata;

/**
 * Executes all schema (change, drop, truncate) and data (insert, save, update, delete) mutation
 * fields of a request in a single transaction. Fields execute in document order; schema mutations
 * must precede data mutations because DDL and TRUNCATE fail on tables with pending deferred
 * constraint checks.
 */
class GraphqlTransactionalMutationStrategy extends AsyncSerialExecutionStrategy {

  enum Phase {
    SCHEMA,
    DATA
  }

  private static final Map<String, Phase> TRANSACTIONAL_MUTATIONS =
      Map.of(
          CHANGE, SCHEMA,
          DROP, SCHEMA,
          TRUNCATE, SCHEMA,
          INSERT, DATA,
          SAVE, DATA,
          UPDATE, DATA,
          DELETE, DATA);
  private static final String INTROSPECTION_PREFIX = "__";

  private final Schema schema;

  GraphqlTransactionalMutationStrategy(Schema schema) {
    super(new GraphqlCustomExceptionHandler());
    this.schema = schema;
  }

  static Schema currentSchema(DataFetchingEnvironment environment, Schema schema) {
    return environment.getGraphQlContext().getOrDefault(Schema.class, schema);
  }

  static boolean inRequestTransaction(DataFetchingEnvironment environment) {
    return environment.getGraphQlContext().hasKey(Schema.class);
  }

  static boolean isCombinedWithOtherMutations(DataFetchingEnvironment environment) {
    return environment
            .getOperationDefinition()
            .getSelectionSet()
            .getSelectionsOfType(Field.class)
            .stream()
            .filter(field -> !field.getName().startsWith(INTROSPECTION_PREFIX))
            .count()
        > 1;
  }

  @Override
  public CompletableFuture<ExecutionResult> execute(
      ExecutionContext context, ExecutionStrategyParameters parameters) {
    List<String> fieldNames = mutationFieldNames(parameters);
    if (fieldNames.stream().noneMatch(TRANSACTIONAL_MUTATIONS::containsKey)) {
      return super.execute(context, parameters);
    }
    String validationError = validate(fieldNames);
    if (validationError != null) {
      return CompletableFuture.completedFuture(errorResult(validationError));
    }
    AtomicReference<ExecutionResult> result = new AtomicReference<>();
    AtomicReference<Schema> transactionSchema = new AtomicReference<>();
    try {
      schema
          .getDatabase()
          .tx(
              db -> {
                transactionSchema.set(db.getSchema(schema.getName()));
                context.getGraphQLContext().put(Schema.class, transactionSchema.get());
                result.set(super.execute(context, parameters).join());
                if (!result.get().getErrors().isEmpty()) {
                  throw new MolgenisException("Mutation rolled back because of errors");
                }
              });
    } catch (MolgenisException e) {
      if (result.get() == null || result.get().getErrors().isEmpty()) {
        result.set(errorResult(e.toString()));
      }
      return CompletableFuture.completedFuture(result.get());
    }
    if (fieldNames.stream().anyMatch(name -> TRANSACTIONAL_MUTATIONS.get(name) == SCHEMA)) {
      ((SqlSchemaMetadata) schema.getMetadata())
          .sync((SqlSchemaMetadata) transactionSchema.get().getMetadata());
      schema.getDatabase().getListener().onSchemaChange();
    }
    return CompletableFuture.completedFuture(result.get());
  }

  @Override
  protected CompletableFuture<ExecutionResult> resolveField(
      ExecutionContext context, ExecutionStrategyParameters parameters) {
    if (context.getGraphQLContext().hasKey(Schema.class) && !context.getErrors().isEmpty()) {
      return CompletableFuture.completedFuture(new ExecutionResultImpl(null, List.of()));
    }
    return super.resolveField(context, parameters);
  }

  private static String validate(List<String> fieldNames) {
    List<String> otherMutations =
        fieldNames.stream().filter(name -> !TRANSACTIONAL_MUTATIONS.containsKey(name)).toList();
    if (!otherMutations.isEmpty()) {
      return "Mutations (change, drop, truncate, insert, save, update, delete) cannot be combined with other mutations: "
          + String.join(", ", otherMutations);
    }
    Phase previous = SCHEMA;
    for (String name : fieldNames) {
      Phase phase = TRANSACTIONAL_MUTATIONS.get(name);
      if (phase.compareTo(previous) < 0) {
        return "Schema mutations (change, drop, truncate) must precede data mutations (insert, save, update, delete)";
      }
      previous = phase;
    }
    return null;
  }

  private static List<String> mutationFieldNames(ExecutionStrategyParameters parameters) {
    return parameters.getFields().getSubFieldsList().stream()
        .map(MergedField::getName)
        .filter(name -> !name.startsWith(INTROSPECTION_PREFIX))
        .toList();
  }

  private static ExecutionResult errorResult(String message) {
    return ExecutionResultImpl.newExecutionResult()
        .addError(GraphqlErrorBuilder.newError().message(message).build())
        .build();
  }
}
