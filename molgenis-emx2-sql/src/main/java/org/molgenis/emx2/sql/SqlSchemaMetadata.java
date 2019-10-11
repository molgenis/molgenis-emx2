package org.molgenis.emx2.sql;

import org.jooq.CreateSchemaFinalStep;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.molgenis.emx2.DefaultRoles;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.utils.MolgenisException;

import java.util.Collection;

import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.sql.Constants.MG_USER_PREFIX;

public class SqlSchemaMetadata extends SchemaMetadata {
  private SqlDatabase db;

  public SqlSchemaMetadata(SqlDatabase db, String name) {
    super(name);
    this.db = db;
  }

  public boolean exists() {
    return MetadataUtils.schemaExists(db.getJooq(), this.getName());
  }

  @Override
  public SqlTableMetadata getTableMetadata(String name) throws MolgenisException {
    try {
      return (SqlTableMetadata) super.getTableMetadata(name);
    } catch (Exception e) {
      // else retrieve from metadata
      SqlTableMetadata table = new SqlTableMetadata(db, this, name);
      table.load();
      if (table.exists()) {
        this.tables.put(name, table);
        return table;
      } else {
        throw new MolgenisException(
            "undefined_table",
            "Table not found",
            "Table '" + name + "' couldn't not be found in schema " + getName());
      }
    }
  }

  void createSchema() throws MolgenisException {
    String schemaName = getName();

    try (CreateSchemaFinalStep step = db.getJooq().createSchema(schemaName)) {
      step.execute();

      String member = getRolePrefix() + DefaultRoles.VIEWER;
      String editor = getRolePrefix() + DefaultRoles.EDITOR;
      String manager = getRolePrefix() + DefaultRoles.MANAGER;
      String owner = getRolePrefix() + DefaultRoles.OWNER;

      db.createRole(member);
      db.createRole(editor);
      db.createRole(manager);
      db.createRole(owner);

      db.getJooq().execute("GRANT {0} TO {1}", name(member), name(editor));
      db.getJooq().execute("GRANT {0},{1} TO {2}", name(member), name(editor), name(manager));
      db.getJooq()
          .execute(
              "GRANT {0},{1},{2} TO {3} WITH ADMIN OPTION",
              name(member), name(editor), name(manager), name(owner));

      db.getJooq().execute("GRANT USAGE ON SCHEMA {0} TO {1}", name(schemaName), name(member));
      db.getJooq().execute("GRANT ALL ON SCHEMA {0} TO {1}", name(schemaName), name(manager));

      // make current user a owner
      if (db.getActiveUser() != null) {
        db.getJooq()
            .execute("GRANT {0} TO {1}", name(owner), name(MG_USER_PREFIX + db.getActiveUser()));
      }
    } catch (DataAccessException e) {
      throw new SqlMolgenisException(
          "schema_create_failed", "Schema createTableIfNotExists failed", e);
    }
    MetadataUtils.saveSchemaMetadata(db.getJooq(), this);
  }

  String getRolePrefix() {
    return Constants.MG_ROLE_PREFIX + getName().toUpperCase() + "/";
  }

  @Override
  public SqlTableMetadata createTableIfNotExists(String name) throws MolgenisException {
    try {
      return getTableMetadata(name);
    } catch (Exception e) {
      SqlTableMetadata table = new SqlTableMetadata(db, this, name);
      table.createTable();
      super.tables.put(name, table);
      return table;
    }
  }

  @Override
  public Collection<String> getTableNames() {
    Collection<String> result = super.getTableNames();
    if (result.isEmpty()) {
      result = MetadataUtils.loadTableNames(this);
      for (String r : result) {
        this.tables.put(r, null);
      }
    }
    return result;
  }

  @Override
  public void dropTable(String tableName) throws MolgenisException {
    SqlTableMetadata tableMetadata = getTableMetadata(tableName);
    tableMetadata.dropTable();
    super.dropTable(tableName);
  }

  protected DSLContext getJooq() {
    return db.getJooq();
  }
}
