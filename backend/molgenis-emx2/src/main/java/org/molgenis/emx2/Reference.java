package org.molgenis.emx2;

import org.jooq.DataType;
import org.jooq.Field;

import java.util.List;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.utils.TypeUtils.toJooqType;

public class Reference {
  private Column column;
  private String fromColumn;
  private String toColumn;
  private List<String> path;

  public List<String> getPath() {
    return path;
  }

  private ColumnType type;
  private ColumnType primitiveType;
  private boolean nullable;

  public Reference(
      Column column,
      String fromColumn,
      String toColumn,
      ColumnType type,
      ColumnType primitiveType,
      boolean nullable,
      List<String> path) {
    this.column = column;
    this.fromColumn = fromColumn;
    this.toColumn = toColumn;
    this.type = type;
    this.primitiveType = primitiveType;
    this.nullable = nullable;
    this.path = path;
  }

  public String getName() {
    return fromColumn;
  }

  public String getRefTo() {
    return toColumn;
  }

  public ColumnType getColumnType() {
    return type;
  }

  public DataType getJooqType() {
    return toJooqType(getPrimitiveType());
  }

  public boolean isNullable() {
    return nullable;
  }

  public Field getJooqField() {
    return field(name(getName()), getJooqType());
  }

  public void setName(String name) {
    this.fromColumn = name;
  }

  public ColumnType getPrimitiveType() {
    return this.primitiveType;
  }

  public Reference getOverlapping() {
    for (Column c : column.getTable().getColumns()) {
      if (c.getName().equals(column.getName())) {
        return null; // should be before this one
      } else if (c.getName().equals(getName())) {
        return c.getReferences().get(0);
      } else {
        for (Reference ref : c.getReferences()) {
          if (ref.getName().equals(this.getName())) {
            return ref;
          }
        }
      }
    }
    return null;
  }

  public boolean isOverlapping() {
    return getOverlapping() != null;
  }
}
