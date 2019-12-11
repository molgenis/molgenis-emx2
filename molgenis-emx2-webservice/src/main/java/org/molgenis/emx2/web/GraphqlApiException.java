package org.molgenis.emx2.web;

import org.molgenis.emx2.utils.MolgenisException;

import java.io.IOException;

class GraphqlApiException extends MolgenisException {
  public GraphqlApiException(String message) {
    super(
        GraphqlApi.class.getSimpleName().toUpperCase(), GraphqlApi.class.getSimpleName(), message);
  }

  public GraphqlApiException(IOException e) {
    super(e);
  }
}
