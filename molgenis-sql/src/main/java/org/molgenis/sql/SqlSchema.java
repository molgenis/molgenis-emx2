package org.molgenis.sql;

import org.jooq.DSLContext;
import org.jooq.Name;
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
import static org.molgenis.Database.Prefix.MGROLE_;
import static org.molgenis.Database.Roles.*;
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
    Name tableName = name(getName(), name);
    sql.createTableIfNotExists(tableName)
        .column(MOLGENISID, SQLDataType.UUID)
        .constraints(constraint("PK_" + name).primaryKey(MOLGENISID))
        .execute();
    // immediately make the 'admin' owner
    sql.execute(
        "ALTER TABLE {0} OWNER TO {1}",
        tableName, name(MGROLE_ + getName().toUpperCase() + _MANAGER));
    sql.execute(
        "GRANT SELECT ON {0} TO {1}", tableName, name(MGROLE_ + getName().toUpperCase() + _VIEWER));
    sql.execute(
        "GRANT INSERT, UPDATE, DELETE, REFERENCES, TRUNCATE ON {0} TO {1}",
        tableName, name(MGROLE_ + getName().toUpperCase() + _EDITOR));
    SqlTable t = new SqlTable(this, sql, name);
    super.addTable(t);
    return t;
  }

  @Override
  public void grantAdmin(String user) {
    sql.execute(
        "GRANT {0} TO {1} WITH ADMIN OPTION",
        name(MGROLE_ + getName().toUpperCase() + _ADMIN), name(user));
  }

  @Override
  public void grantManage(String user) {
    sql.execute("GRANT {0} TO {1}", name(MGROLE_ + getName().toUpperCase() + _MANAGER), name(user));
  }

  @Override
  public void grantEdit(String user) {
    sql.execute("GRANT {0} TO {1}", name(MGROLE_ + getName().toUpperCase() + _EDITOR), name(user));
  }

  @Override
  public void grantView(String user) {
    sql.execute("GRANT {0} TO {1}", name(MGROLE_ + getName().toUpperCase() + _VIEWER), name(user));
  }

  @Override
  public void dropTable(String tableId) {
    sql.dropTable(name(getName(), tableId)).execute();
    super.dropTable(tableId);
  }
}
