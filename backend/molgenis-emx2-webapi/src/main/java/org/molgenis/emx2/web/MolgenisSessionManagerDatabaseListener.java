package org.molgenis.emx2.web;

import org.molgenis.emx2.Database;
import org.molgenis.emx2.DatabaseListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** To clear caches if schema or users change, which changes what users can see */
public class MolgenisSessionManagerDatabaseListener implements DatabaseListener {
  private static final Logger logger =
      LoggerFactory.getLogger(MolgenisSessionManagerDatabaseListener.class);
  private Database database;
  private MolgenisSessionManager sessionManager;
  private boolean onEndTransactionClear = false;

  public MolgenisSessionManagerDatabaseListener(
      MolgenisSessionManager sessionManager, Database database) {
    this.sessionManager = sessionManager;
    this.database = database;
  }

  @Override
  public void userChanged() {
    database.clearCache();
  }

  @Override
  public void schemaRemoved(String schemaName) {
    // schema change might affect all users,
    // todo make smarter is to inefficient to reload all caches
    logger.info("clear caches on schemaRemove");
    sessionManager.clearAllCaches();
  }

  @Override
  public void schemaChanged(String schemaName) {
    // schema change might affect all users,
    // todo make smarter is to inefficient to reload all caches
    if (!database.inTx()) {
      sessionManager.clearAllCaches();
      logger.info("clear caches on schemaChanged");
    } else {
      onEndTransactionClear = true;
    }
  }

  @Override
  public void afterCommit() {
    if (onEndTransactionClear) {
      sessionManager.clearAllCaches();
      onEndTransactionClear = false;
      logger.info("clear caches on schemaChanged (waited for commit)");
    }
  }
}
