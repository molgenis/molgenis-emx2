package org.molgenis.emx2;

public enum TableType {
  DATA,
  ONTOLOGIES,
  MODULE;

  public boolean isModule() {
    return this == MODULE;
  }
}
