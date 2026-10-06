package org.molgenis.emx2.graphql;

import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphqlErrorBuilder;
import graphql.execution.AsyncSerialExecutionStrategy;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
import graphql.execution.MergedField;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.MutationType;
import org.molgenis.emx2.Schema;

class GraphqlTransactionalMutationStrategy extends AsyncSerialExecutionStrategy {

  private static final Set<String> DATA_MUTATIONS =
      Set.of(
          MutationType.INSERT.toString(),
          MutationType.SAVE.toString(),
          MutationType.DELETE.toString(),
          MutationType.UPDATE.toString());

  private final Schema schema;

  GraphqlTransactionalMutationStrategy(Schema schema) {
    super(new GraphqlCustomExceptionHandler());
    this.schema = schema;
  }

  @Override
  public CompletableFuture<ExecutionResult> execute(
      ExecutionContext context, ExecutionStrategyParameters parameters) {
    if (!onlyDataMutations(parameters)) {
      return super.execute(context, parameters);
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
        result.set(
            ExecutionResultImpl.newExecutionResult()
                .addError(GraphqlErrorBuilder.newError().message(e.toString()).build())
                .build());
      }
    }
    return CompletableFuture.completedFuture(result.get());
  }

  private static boolean onlyDataMutations(ExecutionStrategyParameters parameters) {
    return parameters.getFields().getSubFieldsList().stream()
        .map(MergedField::getName)
        .allMatch(DATA_MUTATIONS::contains);
  }
}
