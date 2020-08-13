package org.molgenis.emx2;

import org.jooq.DataType;
import org.jooq.Field;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.utils.TypeUtils.toJooqType;

public class Reference {
  private String fromColumn;
  private String toColumn;
  private ColumnType type;
  private boolean nullable;
  // use when reference actually links to another column
  private boolean existing;

  public Reference(String fromColumn, String toColumn, ColumnType type, boolean nullable) {
    this.fromColumn = fromColumn;
    this.toColumn = toColumn;
    this.type = type;
    this.nullable = nullable;
  }

  public String getName() {
    return fromColumn;
  }

  public String getTo() {
    return toColumn;
  }

  public ColumnType getColumnType() {
    return type;
  }

  public DataType<?> getJooqType() {
    return toJooqType(getColumnType());
  }

  public boolean isNullable() {
    return nullable;
  }

  public Field asJooqField() {
    return field(name(getName()), getJooqType());
  }

  public boolean isExisting() {
    return existing;
  }

  public void setExisting(boolean existing) {
    this.existing = existing;
  }

  public void setName(String name) {
    this.fromColumn = name;
  }
}
