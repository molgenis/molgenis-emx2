package org.molgenis.emx2.sql;

import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.MolgenisException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jooq.impl.DSL.createTableIfNotExists;
import static org.jooq.impl.DSL.name;

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
  public Table create(TableMetadata metadata) throws MolgenisException {
    transaction(
        database -> {
          TableMetadata table = this.createTableIfNotExists(metadata.getTableName()).getMetadata();
          for (Column c : metadata.getColumns()) {
            table.addColumn(c);
          }
          if (metadata.getPrimaryKey().length > 0) table.setPrimaryKey(metadata.getPrimaryKey());
          for (String[] unique : metadata.getUniques()) {
            table.addUnique(unique);
          }
        });
    return getTable(metadata.getTableName());
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
    if (Permission.ADMIN.equals(permission)) {
      db.getJooq()
          .execute(
              "GRANT {0} TO {1} WITH ADMIN OPTION",
              name(
                  SqlTable.MG_ROLE_PREFIX
                      + getMetadata().getName().toUpperCase()
                      + Permission.ADMIN),
              name(user));
    } else {
      db.grantRole(
          SqlTable.MG_ROLE_PREFIX + getMetadata().getName().toUpperCase() + permission, user);
    }
  }

  @Override
  public void revokePermission(Permission permission, String user) throws MolgenisException {
    db.revokeRole(
        SqlTable.MG_ROLE_PREFIX + getMetadata().getName().toUpperCase() + permission, user);
  }

  @Override
  public Query query(String tableName) throws MolgenisException {
    return getTable(tableName).query();
  }

  @Override
  public void transaction(Transaction transaction) throws MolgenisException {
    db.transaction(transaction);
  }

  @Override
  public void transaction(String role, Transaction transaction) throws MolgenisException {
    db.transaction(role, transaction);
  }

  @Override
  public void copy(SchemaMetadata from) throws MolgenisException {
    transaction(
        database -> {
          List<TableMetadata> tableList = new ArrayList<>();
          for (String tableName : from.getTableNames()) {
            tableList.add(from.getTableMetadata(tableName));
          }

          // todo circular dependencies
          Collections.sort(
              tableList,
              (a, b) -> {
                String aName = a.getTableName();
                String bName = b.getTableName();
                for (Column c : a.getColumns()) {
                  if (bName.equals(c.getRefTableName())) return 1;
                }
                for (Column c : b.getColumns()) {
                  if (aName.equals(c.getRefTableName())) return -1;
                }
                return 0;
              });

          for (TableMetadata table : tableList) {
            this.create(table);
          }
        });
  }
}
