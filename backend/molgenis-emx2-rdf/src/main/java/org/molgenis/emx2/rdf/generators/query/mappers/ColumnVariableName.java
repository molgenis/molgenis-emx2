package org.molgenis.emx2.rdf.generators.query.mappers;

import org.molgenis.emx2.Column;

public class ColumnVariableName {

  private final Column column;

  public ColumnVariableName(Column column) {
    this.column = column;
  }

  public String getSparqlName() {
    return column.getName().replace(" ", "___");
  }
}
