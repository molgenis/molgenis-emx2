package org.molgenis.emx2.web;

import org.molgenis.emx2.Database;
import org.molgenis.emx2.DatabaseListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** To clear caches if schema or users change, which changes what users can see */
public class MolgenisSessionManagerDatabaseListener extends DatabaseListener {
  private static final Logger logger =
      LoggerFactory.getLogger(MolgenisSessionManagerDatabaseListener.class);
  private Database database;
  private MolgenisSessionManager sessionManager;

  public MolgenisSessionManagerDatabaseListener(
      MolgenisSessionManager sessionManager, Database database) {
    this.sessionManager = sessionManager;
    this.database = database;
  }

  @Override
  public void afterCommit() {
    super.afterCommit();
    database.clearCache();
    sessionManager.clearAllCaches();
    logger.info("cleared caches after commit that includes changes on schema(s)");
  }
}
