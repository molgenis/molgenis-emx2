package org.molgenis.emx2.graphql;

import graphql.ErrorClassification;
import graphql.ExceptionWhileDataFetching;
import graphql.GraphQLError;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ResultPath;
import graphql.language.SourceLocation;
import java.util.List;
import org.molgenis.emx2.MolgenisException;

public class GraphqlCustomExceptionHandler implements DataFetcherExceptionHandler {

  @Override
  public DataFetcherExceptionHandlerResult onException(
      DataFetcherExceptionHandlerParameters handlerParameters) {

    final Throwable exception = handlerParameters.getException();
    final SourceLocation sourceLocation = handlerParameters.getSourceLocation();
    final ResultPath path = handlerParameters.getPath();
    GraphQLError error = new ExceptionWhileDataFetching(path, exception, sourceLocation);
    final ErrorClassification errorType = error.getErrorType();

    if (exception instanceof MolgenisException) {
      error =
          new GraphQLError() {

            @Override
            public String getMessage() {
              return exception.toString();
            }

            @Override
            public List<SourceLocation> getLocations() {
              return List.of(sourceLocation);
            }

            @Override
            public ErrorClassification getErrorType() {
              return errorType;
            }
          };
    }

    return DataFetcherExceptionHandlerResult.newResult().error(error).build();
  }
}
