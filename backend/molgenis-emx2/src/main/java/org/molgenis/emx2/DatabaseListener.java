package org.molgenis.emx2;

public interface DatabaseListener {
  void schemaRemoved(String name);

  void userChanged();

  void schemaChanged(String schemaName);
}
