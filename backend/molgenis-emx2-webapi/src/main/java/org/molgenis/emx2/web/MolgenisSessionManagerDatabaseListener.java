package org.molgenis.emx2.web;

import org.molgenis.emx2.DatabaseListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** To clear caches if schema or users change, which changes what users can see */
public class MolgenisSessionManagerDatabaseListener extends DatabaseListener {
  private static final Logger logger =
      LoggerFactory.getLogger(MolgenisSessionManagerDatabaseListener.class);
  private MolgenisSessionManager sessionManager;
  private MolgenisSession session;

  public MolgenisSessionManagerDatabaseListener(
      MolgenisSessionManager sessionManager, MolgenisSession session) {
    this.sessionManager = sessionManager;
    this.session = session;
  }

  @Override
  public void userChanged() {
    logger.info("cleared cache for this session because user changed");
    session.clearCache();
  }

  @Override
  public void afterCommit() {
    super.afterCommit();
    sessionManager.clearAllCaches();
    logger.info(
        "cleared all caches after commit that may include changes on schema(s) or permissions");
  }
}
