package org.molgenis.emx2.beaconv2.endpoints;

import java.util.List;
import org.molgenis.emx2.Column;

/**
 * Used to find and report the GraphQL path towards a particular column relative to the Table it was
 * searched in
 */
public class ColumnPath {

  private Column column;
  private List<Column> path;

  public ColumnPath(Column column, List<Column> path) {
    this.column = column;
    this.path = path;
    path.add(column);
  }

  public Column getColumn() {
    return column;
  }

  public List<Column> getPath() {
    return path;
  }

  @Override
  public String toString() {
    StringBuilder stringBuffer = new StringBuilder();
    for (Column col : path) {
      stringBuffer.append("{" + col.getName() + ":");
    }
    stringBuffer.append("{");
    return stringBuffer.toString();
  }
}
