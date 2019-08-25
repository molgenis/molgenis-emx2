package org.molgenis.sql;

import org.molgenis.MolgenisException;
import org.molgenis.Permission;
import org.molgenis.metadata.SchemaMetadata;
import org.molgenis.query.Query;
import org.molgenis.data.Schema;

import java.util.Collection;

import static org.jooq.impl.DSL.name;
import static org.molgenis.Permission.*;
import static org.molgenis.sql.SqlTable.MG_ROLE_PREFIX;

public class SqlSchema implements Schema {
  private SqlDatabase db;
  private SchemaMetadata metadata;

  public SqlSchema(SqlDatabase db, SchemaMetadata metadata) {
    this.db = db;
    this.metadata = metadata;
  }

  @Override
  public SqlTable getTable(String name) throws MolgenisException {
    return new SqlTable(db, getMetadata().getTableMetadata(name));
  }

  @Override
  public SqlTable createTableIfNotExists(String name) throws MolgenisException {
    try {
      return getTable(name);
    } catch (Exception e) {
      getMetadata().createTableIfNotExists(name);
      return getTable(name);
    }
  }

  @Override
  public SchemaMetadata getMetadata() {
    return metadata;
  }

  @Override
  public Collection<String> getTableNames() throws MolgenisException {
    return getMetadata().getTableNames();
  }

  @Override
  public void grant(Permission permission, String user) throws MolgenisException {
    if (ADMIN.equals(permission)) {
      db.getJooq()
          .execute(
              "GRANT {0} TO {1} WITH ADMIN OPTION",
              name(MG_ROLE_PREFIX + getMetadata().getName().toUpperCase() + ADMIN), name(user));
    } else {
      db.grantRole(MG_ROLE_PREFIX + getMetadata().getName().toUpperCase() + permission, user);
    }
  }

  @Override
  public void revokePermission(Permission permission, String user) throws MolgenisException {
    db.revokeRole(MG_ROLE_PREFIX + getMetadata().getName().toUpperCase() + permission, user);
  }

  @Override
  public Query query(String tableName) throws MolgenisException {
    return getTable(tableName).query();
  }
}
