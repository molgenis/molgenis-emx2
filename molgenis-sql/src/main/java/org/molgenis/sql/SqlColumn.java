package org.molgenis.sql;

import org.jooq.DSLContext;
import org.molgenis.MolgenisException;
import org.molgenis.Table;
import org.molgenis.beans.ColumnBean;

import static org.jooq.impl.DSL.name;

public class SqlColumn extends ColumnBean {
  private DSLContext sql;

  /** constructor for primitive types */
  public SqlColumn(DSLContext sql, Table table, String name, Type type) {
    super(table, name, type);
    this.sql = sql;
  }

  /** constructor for REF */
  public SqlColumn(DSLContext sql, Table table, String name, Table otherTable) {
    super(table, name, otherTable);
    this.sql = sql;
  }

  /** constructor for MREF */
  public SqlColumn(
      DSLContext sql,
      Table table,
      String name,
      Table otherTable,
      String mrefTable,
      String mrefBack) {
    super(table, name, otherTable, mrefTable, mrefBack);
    this.sql = sql;
  }

  @Override
  public SqlColumn setNullable(boolean nillable) throws MolgenisException {
    if (nillable)
      sql.alterTable(name(getTable().getSchema().getName(), getTable().getName()))
          .alterColumn(getName())
          .dropNotNull()
          .execute();
    else
      sql.alterTable(name(getTable().getSchema().getName(), getTable().getName()))
          .alterColumn(getName())
          .setNotNull()
          .execute();
    super.setNullable(isNullable());
    return this;
  }
}
