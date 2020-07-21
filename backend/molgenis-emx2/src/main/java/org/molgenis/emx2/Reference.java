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

  public Reference(String fromColumn, String toColumn, ColumnType type) {
    this.fromColumn = fromColumn;
    this.toColumn = toColumn;
    this.type = type;
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

  public DataType getJooqType() {
    return toJooqType(getColumnType());
  }

  public Field asJooqField() {
    return field(name(getName()), getJooqType());
  }
}
