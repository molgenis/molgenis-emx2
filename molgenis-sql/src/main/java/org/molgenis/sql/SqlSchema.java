package org.molgenis.sql;

import org.jooq.DSLContext;
import org.jooq.impl.SQLDataType;
import org.molgenis.MolgenisException;
import org.molgenis.beans.SchemaBean;

import static org.jooq.impl.DSL.constraint;
import static org.molgenis.Row.MOLGENISID;

public class SqlSchema extends SchemaBean {
  private DSLContext sql;

  public SqlSchema(DSLContext sql) throws MolgenisException {
    this.sql = sql;
    for (org.jooq.Table t : sql.meta().getTables()) {
      tables.put(t.getName(), new SqlTable(this, sql, t.getName()));
    }
  }

  @Override
  public SqlTable createTable(String name) throws MolgenisException {
    sql.createTableIfNotExists(name)
        .column(MOLGENISID, SQLDataType.UUID)
        .constraints(constraint("PK_" + name).primaryKey(MOLGENISID))
        .execute();
    SqlTable t = new SqlTable(this, sql, name);
    tables.put(name, t);
    return t;
  }

  @Override
  public void dropTable(String tableId) {
    sql.dropTable(tableId).execute();
    tables.remove(tableId);
  }
}
