package org.molgenis.sql;

import org.jooq.DSLContext;
import org.jooq.Schema;
import org.jooq.impl.SQLDataType;
import org.molgenis.Database;
import org.molgenis.MolgenisException;
import org.molgenis.Query;
import org.molgenis.Table;
import org.molgenis.beans.SchemaBean;

import java.util.List;

import static org.jooq.impl.DSL.constraint;
import static org.jooq.impl.DSL.name;
import static org.molgenis.Row.MOLGENISID;

public class SqlSchema extends SchemaBean {
  private Database db;
  private DSLContext sql;

  public SqlSchema(Database db, DSLContext sql, String name) throws MolgenisException {
    super(name);
    this.sql = sql;
    this.db = db;
    List<Schema> schemas = sql.meta().getSchemas(name);
    if (schemas.size() == 1) { // otherwise in transaction (probably)

      for (org.jooq.Table t : schemas.get(0).getTables()) {
        addTable(new SqlTable(this, sql, t.getName()));
      }

      // load columns
      for (Table t : getTables()) {
        ((SqlTable) t).loadColumns();
      }

      // load uniques
      for (Table t : getTables()) {
        ((SqlTable) t).loadUniques();
      }

      // load mrefs
      for (Table t : getTables()) {
        ((SqlTable) t).loadMrefs();
      }
    }
  }

  @Override
  public SqlTable createTable(String name) throws MolgenisException {
    sql.createTableIfNotExists(name(getName(), name))
        .column(MOLGENISID, SQLDataType.UUID)
        .constraints(constraint("PK_" + name).primaryKey(MOLGENISID))
        .execute();
    SqlTable t = new SqlTable(this, sql, name);
    super.addTable(t);
    return t;
  }

  @Override
  public Query query(String table) {
    return new SqlQuery(table, this, sql);
  }

  @Override
  public void dropTable(String tableId) {
    sql.dropTable(name(getName(), tableId)).execute();
    super.dropTable(tableId);
  }
}
