package org.molgenis.emx2.semantics;

import java.util.ArrayList;
import org.molgenis.emx2.Column;

/**
 * Used to find and report the GraphQL path towards a particular column relative to the Table it was
 * searched in
 */
public class ColumnPath {

  private Column column;
  private ArrayList<Column> path;

  public ColumnPath(Column column, ArrayList<Column> path) {
    this.column = column;
    this.path = path;
    path.add(column);
  }

  public Column getColumn() {
    return column;
  }

  public ArrayList<Column> getPath() {
    return path;
  }

  @Override
  public String toString() {
    StringBuffer stringBuffer = new StringBuffer();
    for (Column column : path) {
      stringBuffer.append("{" + column.getName() + ":");
    }
    stringBuffer.append("{");
    return stringBuffer.toString();
  }
}
