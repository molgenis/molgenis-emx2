package org.molgenis.emx2;

import java.util.HashSet;
import java.util.Set;

public class DatabaseListener {
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

  /** Abstract method, called on each commit. When override call to reset the listener */
  public void afterCommit() {
    this.schemaRemoved.clear();
    this.schemaChanged.clear();
  }

  public boolean isDirty() {
    return !this.schemaChanged.isEmpty() || !this.schemaRemoved.isEmpty();
  }
}
