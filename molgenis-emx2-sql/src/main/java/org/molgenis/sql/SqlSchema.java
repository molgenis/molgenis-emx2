package org.molgenis.sql;

import org.jooq.CreateSchemaFinalStep;
import org.jooq.DSLContext;
import org.molgenis.MolgenisException;
import org.molgenis.Query;
import org.molgenis.Schema;
import org.molgenis.beans.SchemaMetadata;

import java.util.Collection;

import static org.jooq.impl.DSL.name;
import static org.molgenis.Role.*;
import static org.molgenis.sql.MetadataUtils.*;
import static org.molgenis.sql.SqlTable.MG_ROLE_PREFIX;

public class SqlSchema extends SchemaMetadata implements Schema {
  private SqlDatabase db;
  private DSLContext jooq;

  public SqlSchema(SqlDatabase db, String name) {
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
      table.load();
      if (table.exists()) {
        this.tables.put(name, table);
        return table;
      } else throw new MolgenisException("Table '" + name + "' doesn't exists");
    }
  }

  void createSchema() throws MolgenisException {
    String schemaName = getName();

    try (CreateSchemaFinalStep step = jooq.createSchema(schemaName)) {
      step.execute();

      String viewer = MG_ROLE_PREFIX + schemaName.toUpperCase() + VIEWER;
      String editor = MG_ROLE_PREFIX + schemaName.toUpperCase() + EDITOR;
      String manager = MG_ROLE_PREFIX + schemaName.toUpperCase() + MANAGER;
      String admin = MG_ROLE_PREFIX + schemaName.toUpperCase() + ADMIN;

      db.createRole(viewer);
      db.createRole(editor);
      db.createRole(manager);
      db.createRole(admin);

      jooq.execute("GRANT {0} TO {1}", name(viewer), name(editor));
      jooq.execute("GRANT {0},{1} TO {2}", name(viewer), name(editor), name(manager));
      jooq.execute(
          "GRANT {0},{1},{2} TO {3} WITH ADMIN OPTION",
          name(viewer), name(editor), name(manager), name(admin));

      jooq.execute("GRANT USAGE ON SCHEMA {0} TO {1}", name(schemaName), name(viewer));
      jooq.execute("GRANT ALL ON SCHEMA {0} TO {1}", name(schemaName), name(manager));
    } catch (Exception e) {
      throw new MolgenisException(e);
    }
    saveSchemaMetadata(jooq, this);
  }

  @Override
  public SqlTable createTableIfNotExists(String name) throws MolgenisException {
    SqlTable table = new SqlTable(this, name);
    table.createTable();
    super.tables.put(name, table);
    return table;
  }

  @Override
  public Collection<String> getTableNames() throws MolgenisException {
    Collection<String> result = super.getTableNames();
    if (result.isEmpty()) {
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
        name(MG_ROLE_PREFIX + getName().toUpperCase() + ADMIN), name(user));
  }

  @Override
  public void grantManage(String user) throws MolgenisException {
    db.grantRole(MG_ROLE_PREFIX + getName().toUpperCase() + MANAGER, user);
  }

  @Override
  public void grantEdit(String user) throws MolgenisException {
    db.grantRole(MG_ROLE_PREFIX + getName().toUpperCase() + EDITOR, user);
  }

  @Override
  public void grantView(String user) throws MolgenisException {
    db.grantRole(MG_ROLE_PREFIX + getName().toUpperCase() + VIEWER, user);
  }

  @Override
  public void dropTable(String tableName) throws MolgenisException {
    getTable(tableName).dropTable();
    super.dropTable(tableName);
  }

  @Override
  public Query query(String tableName) throws MolgenisException {
    return getTable(tableName).query();
  }

  protected DSLContext getJooq() {
    return db.getJooq();
  }
}
