package org.molgenis.sql;

import org.jooq.DSLContext;
import org.jooq.Field;

import static org.jooq.impl.DSL.name;

class SqlColumnImpl implements SqlColumn {
  private DSLContext sql;
  private SqlTable table;
  // effectively this class wraps Jooq Field
  private Field field;
  private SqlTable refTable = null;

  SqlColumnImpl(DSLContext sql, SqlTable table, Field field, SqlTable otherTable) {
    this.sql = sql;
    this.table = table;
    this.field = field;
    this.refTable = otherTable;
  }

  public SqlColumn setNullable(boolean nillable) {
    if (nillable) {
      sql.alterTable(name(table.getName())).alter(field).dropNotNull().execute();
    } else {
      sql.alterTable(table.getName()).alter(field).setNotNull().execute();
    }
    // update state
    this.field.getDataType().nullable(nillable);
    return this;
  }

  @Override
  public SqlTable getTable() {
    return this.table;
  }

  @Override
  public String getName() {
    return field.getName();
  }

  @Override
  public SqlType getType() {
    if (refTable != null) {
      return SqlType.REF;
    }
    return SqlTypeUtils.getSqlType(field);
  }

  @Override
  public Boolean isNullable() {
    return field.getDataType().nullable();
  }

  @Override
  public SqlTable getRefTable() {
    return refTable;
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(getName()).append(" ");
    if (SqlType.REF.equals(getType()))
      builder.append("ref(").append(refTable.getName()).append(")");
    else builder.append(getType().toString().toLowerCase());
    if (isNullable()) builder.append(" nullable");
    return builder.toString();
  }

  public Field getJooqField() {
    return field;
  }
}
