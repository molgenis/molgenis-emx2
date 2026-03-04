package org.molgenis.emx2.graphql;

public interface GraphqlSessionHandlerInterface {

  void createSession(String username);

  void destroySession();

  String getCurrentUser();
}
