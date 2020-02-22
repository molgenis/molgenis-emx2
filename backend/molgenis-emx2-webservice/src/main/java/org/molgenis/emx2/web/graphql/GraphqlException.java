package org.molgenis.emx2.web.graphql;

import org.molgenis.emx2.MolgenisException;

import java.io.IOException;

public class GraphqlException extends MolgenisException {
  public GraphqlException(String title, String message) {
    super(title, message);
  }

  public GraphqlException(String title, Exception e) {
    super(title, e);
  }
}
