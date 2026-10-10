package org.molgenis.emx2.graphql;

import static org.molgenis.emx2.graphql.GraphqlConstants.DELETE;
import static org.molgenis.emx2.graphql.GraphqlConstants.INSERT;
import static org.molgenis.emx2.graphql.GraphqlConstants.SAVE;
import static org.molgenis.emx2.graphql.GraphqlConstants.UPDATE;

import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphqlErrorBuilder;
import graphql.execution.AsyncSerialExecutionStrategy;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
import graphql.execution.MergedField;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;

class GraphqlTransactionalMutationStrategy extends AsyncSerialExecutionStrategy {

  private static final Set<String> DATA_MUTATIONS = Set.of(INSERT, SAVE, UPDATE, DELETE);
  private static final String INTROSPECTION_PREFIX = "__";

  private final Schema schema;

  GraphqlTransactionalMutationStrategy(Schema schema) {
    super(new GraphqlCustomExceptionHandler());
    this.schema = schema;
  }

  @Override
  public CompletableFuture<ExecutionResult> execute(
      ExecutionContext context, ExecutionStrategyParameters parameters) {
    List<String> fieldNames = mutationFieldNames(parameters);
    if (fieldNames.stream().noneMatch(DATA_MUTATIONS::contains)) {
      return super.execute(context, parameters);
    }
    List<String> otherMutations =
        fieldNames.stream().filter(name -> !DATA_MUTATIONS.contains(name)).toList();
    if (!otherMutations.isEmpty()) {
      return CompletableFuture.completedFuture(
          errorResult(
              "Data mutations (insert, save, update, delete) cannot be combined with other mutations: "
                  + String.join(", ", otherMutations)));
    }
    AtomicReference<ExecutionResult> result = new AtomicReference<>();
    try {
      schema
          .getDatabase()
          .tx(
              db -> {
                context.getGraphQLContext().put(Schema.class, db.getSchema(schema.getName()));
                result.set(super.execute(context, parameters).join());
                if (!result.get().getErrors().isEmpty()) {
                  throw new MolgenisException("Mutation rolled back because of errors");
                }
              });
    } catch (MolgenisException e) {
      if (result.get() == null || result.get().getErrors().isEmpty()) {
        result.set(errorResult(e.toString()));
      }
    }
    return CompletableFuture.completedFuture(result.get());
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
