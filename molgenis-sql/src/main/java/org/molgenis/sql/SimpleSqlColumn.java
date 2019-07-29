package org.molgenis.sql;

import org.jooq.DSLContext;
import org.jooq.DataType;
import org.jooq.Field;
import org.molgenis.MolgenisException;
import org.molgenis.Table;
import org.molgenis.Type;
import org.molgenis.beans.ColumnBean;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

public class SimpleSqlColumn extends ColumnBean {
  protected DSLContext sql;

  public SimpleSqlColumn(DSLContext sql, Table table, String name, Type type)
      throws MolgenisException {
    super(table, name, type);
    this.sql = sql;
  }

  // for ref subclass
  protected SimpleSqlColumn(
      DSLContext sql, Table table, String name, Type type, String refTable, String refColumn) {
    super(table, name, type, refTable, refColumn);
    this.sql = sql;
  }

  public SimpleSqlColumn(
      DSLContext sql, SqlTable sqlTable, String name, Type mref, String toTable, String toColumn) {
    super(sqlTable, name, mref, toTable, toColumn);
    this.sql = sql;
  }

  /** constructor for REF */
  public SimpleSqlColumn createColumn() throws MolgenisException {
    DataType thisType = SqlTypeUtils.jooqTypeOf(this);
    Field thisColumn = field(name(getName()), thisType);
    sql.alterTable(asJooqTable()).addColumn(thisColumn).execute();
    sql.alterTable(asJooqTable())
        .alterColumn(thisColumn)
        .setNotNull()
        .execute(); // seperate to not interfere with type
    return this;
  }

  @Override
  public SimpleSqlColumn setNullable(boolean nillable) throws MolgenisException {
    if (nillable) sql.alterTable(asJooqTable()).alterColumn(getName()).dropNotNull().execute();
    else sql.alterTable(asJooqTable()).alterColumn(getName()).setNotNull().execute();
    super.setNullable(isNullable());
    return this;
  }

  protected org.jooq.Table asJooqTable() {
    return table(name(getTable().getSchema().getName(), getTable().getName()));
  }

  public SimpleSqlColumn setIndexed(boolean indexed) {
    sql.createIndexIfNotExists(name("INDEX_" + getName()))
        .on(asJooqTable(), field(name(getName())))
        .execute();
    return this;
  }
}
