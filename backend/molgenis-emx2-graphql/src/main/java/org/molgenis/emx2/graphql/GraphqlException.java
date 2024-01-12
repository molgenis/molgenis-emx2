package org.molgenis.emx2.graphql;

import org.molgenis.emx2.MolgenisException;

public class GraphqlException extends MolgenisException {
  public GraphqlException(String message) {
    super(message);
  }

  public GraphqlException(String title, Exception e) {
    super(title, e);
  }
}
