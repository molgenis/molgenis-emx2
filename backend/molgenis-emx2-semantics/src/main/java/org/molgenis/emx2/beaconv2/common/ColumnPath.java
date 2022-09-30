package org.molgenis.emx2.beaconv2.common;

import org.molgenis.emx2.Column;

/**
 * Used to find and report the GraphQL path towards a particular column relative to the Table it was
 * searched in
 */
public class ColumnPath {
  public ColumnPath(Column column, String path) {
    this.column = column;
    this.path = path;
  }

  private Column column;
  private String path;

  public Column getColumn() {
    return column;
  }

  public String getPath() {
    return path;
  }
}
