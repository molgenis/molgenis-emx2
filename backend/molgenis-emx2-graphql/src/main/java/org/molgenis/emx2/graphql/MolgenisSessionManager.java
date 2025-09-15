package org.molgenis.emx2.graphql;

public interface MolgenisSessionManager {

  void createSession(String username);

  void destroySession();

  String getCurrentUser();

  void invalidateAll(String username);
}
