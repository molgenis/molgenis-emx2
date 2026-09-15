package org.molgenis.emx2;

public enum OntologyColumn {
  ORDER,
  NAME,
  LABEL,
  TAGS,
  PARENT,
  CODESYSTEM,
  CODE,
  ONTOLOGY_TERM_URI("ontologyTermURI"),
  DEFINITION,
  CHILDREN;

  private final String name;

  OntologyColumn() {
    this.name = name().toLowerCase();
  }

  OntologyColumn(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}
