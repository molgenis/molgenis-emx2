package org.molgenis.emx2.graphql;

import org.molgenis.emx2.Username;

public interface GraphqlSessionHandlerInterface {

  void createSession(Username username);

  void destroySession();

  Username getCurrentUser();
}
