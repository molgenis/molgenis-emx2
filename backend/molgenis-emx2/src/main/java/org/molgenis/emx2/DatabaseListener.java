package org.molgenis.emx2;

import java.util.HashSet;
import java.util.Set;

/**
 * Allows SqlDatabase to notify users that important changes have happened.
 *
 * <p>In particular, we use it now to ensure sessions are refreshed if users change, or if there are
 * transactions that may have changed schema structure and/or permissions.
 */
public abstract class DatabaseListener {
  private Set<String> schemaChanged = new HashSet<>();
  private Set<String> schemaRemoved = new HashSet<>();

  public void schemaRemoved(String schemaName) {
    this.schemaRemoved.add(schemaName);
  }

  public void schemaChanged(String schemaName) {
    this.schemaChanged.add(schemaName);
  }

  public Set<String> getSchemaChanged() {
    return this.schemaChanged;
  }

  public Set<String> getSchemaRemoved() {
    return this.schemaRemoved;
  }

  public abstract void userChanged();

  /** Abstract method, called on each commit. When override call to reset the listener */
  public void afterCommit() {
    this.schemaRemoved.clear();
    this.schemaChanged.clear();
  }

  public boolean isDirty() {
    return !this.schemaChanged.isEmpty() || !this.schemaRemoved.isEmpty();
  }
}
