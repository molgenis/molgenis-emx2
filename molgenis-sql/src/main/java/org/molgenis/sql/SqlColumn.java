package org.molgenis.sql;

import org.jooq.DSLContext;
import org.molgenis.DatabaseException;
import org.molgenis.Table;
import org.molgenis.bean.ColumnBean;

public class SqlColumn extends ColumnBean {
  private DSLContext sql;

  public SqlColumn(DSLContext sql, Table table, String name, Type type) {
    super(table, name, type);
    this.sql = sql;
  }

  public SqlColumn(DSLContext sql, Table table, String name, Table otherTable) {
    super(table, name, otherTable);
    this.sql = sql;
  }

  public SqlColumn(DSLContext sql, Table table, String name, Table otherTable, String joinTable) {
    super(table, name, otherTable, joinTable);
    this.sql = sql;
  }

  @Override
  public SqlColumn setNullable(boolean nillable) throws DatabaseException {
    if (nillable)
      sql.alterTable(getTable().getName()).alterColumn(getName()).dropNotNull().execute();
    else sql.alterTable(getTable().getName()).alterColumn(getName()).setNotNull().execute();
    super.setNullable(isNullable());
    return this;
  }
}
