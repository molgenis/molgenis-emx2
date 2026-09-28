package org.molgenis.emx2;

/**
 * Allows SqlDatabase to notify users that important changes have happened.
 *
 * <p>In particular, we use it now to ensure sessions are refreshed if users change, or if there are
 * transactions that may have changed schema structure and/or permissions.
 */
public abstract class DatabaseListener extends SchemaMetadataProviderListener {

  public abstract void onUserChange();
}
