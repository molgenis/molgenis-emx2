package org.molgenis.emx2;

import java.util.HashSet;
import java.util.Set;

/**
 * Allows SqlDatabase to notify users that important changes have happened.
 *
 * <p>In particular, we use it now to ensure sessions are refreshed if users change, or if there are
 * transactions that may have changed schema structure and/or permissions.
 */
public class DatabaseListener {
  private boolean databaseChanged = false;
  private Set<String> schemaChanged = new HashSet<>();
  private Set<String> schemaRemoved = new HashSet<>();

  public void databaseChanged() {
    this.databaseChanged = true;
  }

  public void schemaRemoved(String schemaName) {
    this.schemaRemoved.add(schemaName);
  }

  public void schemaChanged(String schemaName) {
    this.schemaChanged.add(schemaName);
  }

  public boolean getDatabaseChanged() {
    return this.databaseChanged;
  }

  public Set<String> getSchemaChanged() {
    return this.schemaChanged;
  }

  public Set<String> getSchemaRemoved() {
    return this.schemaRemoved;
  }

  /** Abstract method, called on each commit. When override call to reset the listener */
  public void afterCommit() {
    this.databaseChanged = false;
    this.schemaRemoved.clear();
    this.schemaChanged.clear();
  }

  public boolean isDirty() {
    return this.databaseChanged || !this.schemaChanged.isEmpty() || !this.schemaRemoved.isEmpty();
  }
}
