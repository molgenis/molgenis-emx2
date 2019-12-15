package org.molgenis.emx2.web;

import org.molgenis.emx2.utils.MolgenisException;

import java.io.IOException;

class GraphqlException extends MolgenisException {
  public GraphqlException(String message) {
    super(message);
  }

  public GraphqlException(IOException e) {
    super(e);
  }
}
