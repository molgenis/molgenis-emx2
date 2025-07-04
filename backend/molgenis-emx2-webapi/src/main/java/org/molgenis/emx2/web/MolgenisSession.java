package org.molgenis.emx2.web;

import graphql.GraphQL;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.DatabaseListener;
import org.molgenis.emx2.graphql.GraphqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MolgenisSession extends GraphqlSession {
  private static final Logger logger = LoggerFactory.getLogger(MolgenisSession.class);
  private ApiPerUserCache cache;
  private DatabaseListener databaseListener;

  public MolgenisSession(ApiPerUserCache cache, MolgenisSessionManager sessionManager) {
    super();
    this.cache = cache;
    this.databaseListener = new MolgenisSessionManagerDatabaseListener(sessionManager, this);
  }

  @Override
  public Database getDatabase() {
    return cache.getDatabase(this);
  }

  public GraphQL getGraphqlForDatabase() {
    return cache.getGraphqlForDatabase(this);
  }

  public GraphQL getGraphqlForSchema(String schemaName) {
    return cache.getGraphqlForSchema(this, schemaName);
  }

  public void clearCache() {
    cache.clearCache();
    logger.info("cleared database and caches for user {}", getSessionUser());
  }

  public DatabaseListener getDatabaseChangeListener() {
    return databaseListener;
  }
}
