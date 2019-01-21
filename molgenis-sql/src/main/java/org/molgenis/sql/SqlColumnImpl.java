package org.molgenis.sql;

import org.jooq.DSLContext;
import org.jooq.Field;

import static org.jooq.impl.DSL.name;
import static org.molgenis.sql.SqlType.MREF;

class SqlColumnImpl implements SqlColumn {
  private DSLContext sql;
  private SqlTable table;
  // effectively this class wraps Jooq Field
  private String name;
  private Field field;
  private SqlTable refTable = null;
  private SqlTable mrefTable = null;
  private String mrefBack = null;

  /* simple column*/
  SqlColumnImpl(DSLContext sql, SqlTable table, Field field) {
    this.sql = sql;
    this.table = table;
    this.field = field;
    this.name = field.getName();
  }

  /*REF columm*/
  SqlColumnImpl(DSLContext sql, SqlTable table, Field field, SqlTable otherTable) {
    this(sql, table, field);
    this.refTable = otherTable;
  }

  /*MREF column*/
  SqlColumnImpl(
      DSLContext sql,
      SqlTable table,
      String name,
      SqlTable otherTable,
      SqlTable joinTable,
      String mrefBack) {
    this.sql = sql;
    this.table = table;
    this.name = name;
    this.refTable = otherTable;
    this.mrefTable = joinTable;
    this.mrefBack = mrefBack;
  }

  public SqlColumn setNullable(boolean nillable) throws SqlDatabaseException {
    if (MREF.equals(getType())) {
      throw new SqlDatabaseException("cannot setNullable on type mref");
    }
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
    return this.name;
  }

  @Override
  public SqlType getType() {
    if (mrefTable != null) {
      return MREF;
    }
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

  @Override
  public SqlTable getMrefTable() {
    return mrefTable;
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

  @Override
  public String getMrefBack() {
    return mrefBack;
  }

  public void setMrefBack(String mrefBack) {
    this.mrefBack = mrefBack;
  }
}
