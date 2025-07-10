package org.molgenis.emx2.web;

import org.molgenis.emx2.graphql.GraphqlApiPerUserCache;
import org.molgenis.emx2.graphql.GraphqlSession;

public class MolgenisSession extends GraphqlSession {

  public MolgenisSession(GraphqlApiPerUserCache cache, MolgenisSessionManager sessionManager) {
    // this.databaseListener = new MolgenisSessionManagerDatabaseListener(sessionManager, this);
  }
}
