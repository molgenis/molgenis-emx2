package org.molgenis.emx2.graphql;

import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.language.SourceLocation;
import java.util.List;

class MolgenisGraphqlError implements GraphQLError {

  private final Throwable exception;
  private final SourceLocation sourceLocation;
  private final transient ErrorClassification errorType;

  MolgenisGraphqlError(
      Throwable exception, SourceLocation sourceLocation, ErrorClassification errorType) {
    this.exception = exception;
    this.sourceLocation = sourceLocation;
    this.errorType = errorType;
  }

  Throwable getException() {
    return exception;
  }

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
}
