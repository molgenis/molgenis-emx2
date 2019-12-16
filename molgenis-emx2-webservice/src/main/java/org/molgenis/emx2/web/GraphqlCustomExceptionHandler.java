package org.molgenis.emx2.web;

import graphql.ErrorClassification;
import graphql.ExceptionWhileDataFetching;
import graphql.GraphQLError;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ExecutionPath;
import graphql.language.SourceLocation;
import org.molgenis.emx2.MolgenisException;

import java.util.List;

public class GraphqlCustomExceptionHandler implements DataFetcherExceptionHandler {

  @Override
  public DataFetcherExceptionHandlerResult onException(
      DataFetcherExceptionHandlerParameters handlerParameters) {

    final Throwable exception = handlerParameters.getException();
    final SourceLocation sourceLocation = handlerParameters.getSourceLocation();
    final ExecutionPath path = handlerParameters.getPath();
    GraphQLError error = new ExceptionWhileDataFetching(path, exception, sourceLocation);
    final ErrorClassification errorType = error.getErrorType();

    if (exception instanceof MolgenisException) {
      error =
          new GraphQLError() {

            @Override
            public String getMessage() {
              return exception.getMessage();
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
