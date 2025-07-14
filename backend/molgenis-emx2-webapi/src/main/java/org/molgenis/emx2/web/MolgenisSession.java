package org.molgenis.emx2.web;

import org.molgenis.emx2.graphql.GraphqlSession;

public class MolgenisSession extends GraphqlSession {

  public MolgenisSession(String user) {
    super(user);
  }
}
