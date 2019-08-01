package org.molgenis.sql;

import org.jooq.*;
import org.molgenis.*;
import org.molgenis.beans.SchemaBean;

import java.util.Collection;

import static org.jooq.impl.DSL.name;
import static org.molgenis.Database.Prefix.MGROLE_;
import static org.molgenis.Database.Roles.*;
import static org.molgenis.sql.MetadataUtils.*;

public class SqlSchema extends SchemaBean {
  private SqlDatabase db;
  DSLContext jooq;

  public SqlSchema(SqlDatabase db, String name) throws MolgenisException {
    super(name);
    this.db = db;
    this.jooq = db.getJooq();
  }

  public boolean exists() {
    return schemaExists(this);
  }

  @Override
  public SqlTable getTable(String name) throws MolgenisException {
    try {
      return (SqlTable) super.getTable(name);
    } catch (Exception e) {
      // else retrieve from metadata
      SqlTable table = new SqlTable(this, name);
      if (table.exists()) {
        this.tables.put(name, table);
        return table;
      } else throw new MolgenisException("Table '" + name + "' doesn't exists");
    }
  }

  void createSchema() throws MolgenisException {
    DSLContext jooq = db.getJooq();
    String schemaName = getName();

    try {
      jooq.createSchema(schemaName).execute();

      Name viewer = name(MGROLE_ + schemaName.toUpperCase() + _VIEWER);
      Name editor = name(MGROLE_ + schemaName.toUpperCase() + _EDITOR);
      Name manager = name(MGROLE_ + schemaName.toUpperCase() + _MANAGER);
      Name admin = name(MGROLE_ + schemaName.toUpperCase() + _ADMIN);

      jooq.execute("CREATE ROLE {0}", viewer);
      jooq.execute("CREATE ROLE {0}", editor);
      jooq.execute("CREATE ROLE {0}", manager);
      jooq.execute("CREATE ROLE {0}", admin);

      jooq.execute("GRANT {0} TO {1}", viewer, editor);
      jooq.execute("GRANT {0},{1} TO {2}", viewer, editor, manager);
      jooq.execute("GRANT {0},{1},{2} TO {3} WITH ADMIN OPTION", viewer, editor, manager, admin);

      jooq.execute("GRANT USAGE ON SCHEMA {0} TO {1}", name(schemaName), viewer);
      jooq.execute("GRANT ALL ON SCHEMA {0} TO {1}", name(schemaName), manager);
    } catch (Exception e) {
      throw new MolgenisException(e);
    }
    saveSchemaMetadata(jooq, this);
  }

  @Override
  public SqlTable createTable(String name) throws MolgenisException {
    SqlTable table = new SqlTable(this, name);
    table.createTable();
    super.tables.put(name, table);
    return table;
  }

  @Override
  public Collection<String> getTableNames() throws MolgenisException {
    Collection<String> result = super.getTableNames();
    if (result.size() == 0) {
      result = loadTableNames(this);
      for (String r : result) {
        this.tables.put(r, null);
      }
    }
    return result;
  }

  @Override
  public void grantAdmin(String user) {
    jooq.execute(
        "GRANT {0} TO {1} WITH ADMIN OPTION",
        name(MGROLE_ + getName().toUpperCase() + _ADMIN), name(user));
  }

  @Override
  public void grantManage(String user) {
    jooq.execute(
        "GRANT {0} TO {1}", name(MGROLE_ + getName().toUpperCase() + _MANAGER), name(user));
  }

  @Override
  public void grantEdit(String user) {
    jooq.execute("GRANT {0} TO {1}", name(MGROLE_ + getName().toUpperCase() + _EDITOR), name(user));
  }

  @Override
  public void grantView(String user) {
    jooq.execute("GRANT {0} TO {1}", name(MGROLE_ + getName().toUpperCase() + _VIEWER), name(user));
  }

  @Override
  public void dropTable(String tableName) throws MolgenisException {
    getTable(tableName).dropTable();
    super.dropTable(tableName);
  }

  DSLContext getJooq() {
    return jooq;
  }
}
