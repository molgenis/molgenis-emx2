package org.molgenis.emx2.sql;

import static org.molgenis.emx2.sql.SqlColumnExecutor.executeRemoveRefConstraints;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN_USER;
import static org.molgenis.emx2.sql.SqlSchemaMetadataExecutor.*;
import static org.molgenis.emx2.utils.TableSort.sortTableByDependency;

import java.util.*;
import org.jooq.DSLContext;
import org.molgenis.emx2.*;

public class SqlSchema implements Schema {
  private SqlDatabase db;
  private SqlSchemaMetadata metadata;

  public SqlSchema(SqlDatabase db, SqlSchemaMetadata metadata) {
    this.db = db;
    this.metadata = metadata;
  }

  @Override
  public SqlTable getTable(String name) {
    SqlTableMetadata tableMetadata = getMetadata().getTableMetadata(name);
    if (tableMetadata == null) return getTableById(name);
    if (tableMetadata.exists()) {
      return new SqlTable(db, tableMetadata, db.getTableListener(getName(), name));
    } else return null;
  }

  @Override
  public List<Table> getTablesSorted() {
    List<TableMetadata> tableMetadata = getMetadata().getTables();
    sortTableByDependency(tableMetadata);
    List<Table> result = new ArrayList<>();
    for (TableMetadata tm : tableMetadata) {
      result.add(
          new SqlTable(
              db, (SqlTableMetadata) tm, db.getTableListener(getName(), tm.getTableName())));
    }
    return result;
  }

  @Override
  public void dropTable(String name) {
    getMetadata().drop(name);
  }

  @Override
  public void addMember(String user, String role) {
    tx(
        db ->
            executeAddMembers(
                ((SqlDatabase) db).getJooq(), db.getSchema(getName()), new Member(user, role)));
  }

  @Override
  public List<Member> getMembers() {
    // only admin or other members can see
    if (db.getActiveUser() == null || db.isAdmin() || getRoleForActiveUser() != null) {
      return executeGetMembers(getMetadata().getJooq(), getMetadata());
    } else {
      return new ArrayList<>();
    }
  }

  @Override
  public void removeMembers(List<Member> members) {
    tx(database -> executeRemoveMembers((SqlDatabase) database, getName(), members));
  }

  @Override
  public void removeMember(String user) {
    removeMembers(new Member(user, null));
  }

  @Override
  public void removeMembers(Member... members) {
    removeMembers(Arrays.asList(members));
  }

  @Override
  public List<String> getRoles() {
    return executeGetRoles(getMetadata().getJooq(), this.getMetadata().getName());
  }

  @Override
  public String getRoleForUser(String user) {
    return getMetadata().getRoleForUser(user);
  }

  @Override
  public List<String> getInheritedRolesForUser(String user) {
    // moved implementation to SqlSchemaMetadata so can be cached
    // while being reloaded in case of transactions
    if (user.equals(ADMIN_USER) || user == null) {
      return getRoles();
    } else {
      return getMetadata().getInheritedRolesForUser(user);
    }
  }

  @Override
  public String getRoleForActiveUser() {
    return getRoleForUser(db.getActiveUser());
  }

  @Override
  public List<String> getInheritedRolesForActiveUser() {
    return getMetadata().getInheritedRolesForActiveUser();
  }

  @Override
  public boolean hasActiveUserRole(Privileges privileges) {
    return getInheritedRolesForActiveUser().contains(privileges.toString());
  }

  @Override
  public Table create(TableMetadata metadata) {
    getMetadata().create(metadata);
    return getTable(metadata.getTableName());
  }

  @Override
  public void create(TableMetadata... metadata) {
    getMetadata().create(metadata);
  }

  @Override
  public Database getDatabase() {
    return db;
  }

  @Override
  public SqlSchemaMetadata getMetadata() {
    return metadata;
  }

  @Override
  public Collection<String> getTableNames() {
    return getMetadata().getTableNames();
  }

  @Override
  public Query query(String tableName) {
    return getTable(tableName).query();
  }

  @Override
  public List<Row> retrieveSql(String sql) {
    return retrieveSql(sql, Map.of());
  }

  @Override
  public List<Row> retrieveSql(String sql, Map<String, ?> parameters) {
    if (getRoles().contains("Viewer")) {
      return new SqlRawQueryForSchema(this).executeSql(sql, parameters);
    } else {
      throw new MolgenisException("No view permissions on this schema");
    }
  }

  @Override
  public Query agg(String tableName) {
    return getTable(tableName).agg();
  }

  @Override
  public Query groupBy(String tableName) {
    return getTable(tableName).groupBy();
  }

  @Override
  public Query query(String field, SelectColumn... selection) {
    return new SqlQuery(this.getMetadata(), field, selection);
  }

  @Override
  public void tx(Transaction transaction) {
    db.tx(transaction);
    // copy state in case changed
    this.metadata.sync(db.getSchema(getName()).getMetadata());
  }

  @Override
  public void discard(SchemaMetadata discardSchema) {
    // use migrate
    SchemaMetadata mergeSchema = new SchemaMetadata(discardSchema);
    mergeSchema
        .getTables()
        .forEach(
            t -> {
              t.drop();
              t.getColumns().forEach(c -> c.drop());
            });
    migrate(mergeSchema);
  }

  @Override
  public void migrate(SchemaMetadata mergeSchema) {
    tx(
        database -> {
          migrateTransaction(getName(), mergeSchema, database);
        });
    this.getMetadata().reload();
    db.getListener().schemaChanged(getName());
  }

  private static void migrateTransaction(
      String targetSchemaName, SchemaMetadata mergeSchema, Database database) {
    SqlSchemaMetadata targetSchema =
        (SqlSchemaMetadata) database.getSchema(targetSchemaName).getMetadata();

    // create list, sort dependency order
    List<TableMetadata> mergeTableList = new ArrayList<>();
    mergeSchema.setDatabase(database);
    for (String tableName : mergeSchema.getTableNames()) {
      mergeTableList.add(mergeSchema.getTableMetadata(tableName));
    }
    sortTableByDependency(mergeTableList);

    // first loop
    // create, alter
    // (drop is last thing we do, as columns might need deleting)
    // todo, fix if we rename to existing tables, then order matters
    for (TableMetadata mergeTable : mergeTableList) {

      // get the old table, if exists
      TableMetadata oldTable =
          mergeTable.getOldName() == null
              ? targetSchema.getTableMetadata(mergeTable.getTableName())
              : targetSchema.getTableMetadata(mergeTable.getOldName());

      // set oldName in case table does exist, and oldName was not provided
      if (mergeTable.getOldName() == null && oldTable != null) {
        mergeTable.setOldName(oldTable.getTableName());
      }

      // create table if not exists
      if (oldTable == null && !mergeTable.isDrop()) {
        TableMetadata table =
            new TableMetadata(mergeTable.getTableName()).setTableType(mergeTable.getTableType());
        if (mergeTable.getImportSchema() != null) {
          table.setImportSchema(mergeTable.getImportSchema());
        }
        table.setInheritName(mergeTable.getInheritName());
        TableMetadata newTable = targetSchema.create(table);
        // create primary keys immediately to prevent foreign key dependency issues
        if (mergeTable.getInheritName() == null) {
          mergeTable.getColumns().stream()
              .filter(c -> c.isPrimaryKey())
              .forEach(c -> newTable.add(c));
        }
      } else if (oldTable != null && !oldTable.getTableName().equals(mergeTable.getTableName())) {
        targetSchema.getTableMetadata(oldTable.getTableName()).alterName(mergeTable.getTableName());
      }
    }

    // for create/alter
    //  add missing columns (except refback),
    //  remove triggers in case of table name or column type changes
    //  remove refback
    List<String> created = new ArrayList<>();
    for (TableMetadata mergeTable : mergeTableList) {

      if (!mergeTable.isDrop()) {
        TableMetadata oldTable = targetSchema.getTableMetadata(mergeTable.getTableName());

        // set inheritance
        if (mergeTable.getInheritName() != null) {
          if (mergeTable.getImportSchema() != null) {
            oldTable.setImportSchema(mergeTable.getImportSchema());
          }
          oldTable.setInheritName(mergeTable.getInheritName());
        } else if (oldTable.getInheritName() != null) {
          oldTable.removeInherit();
        }

        // update table settings
        if (!mergeTable.getSettings().isEmpty()) {
          oldTable.setSettings(mergeTable.getSettings());
        }
        if (!mergeTable.getDescriptions().isEmpty()) {
          oldTable.setDescriptions(mergeTable.getDescriptions());
        }
        if (!mergeTable.getLabels().isEmpty()) {
          oldTable.setLabels(mergeTable.getLabels());
        }
        if (mergeTable.getSemantics() != null) {
          oldTable.setSemantics(mergeTable.getSemantics());
        }
        // TableType is DATA by default and therefore never null
        oldTable.setTableType(mergeTable.getTableType());
        MetadataUtils.saveTableMetadata(targetSchema.getJooq(), oldTable);

        // add missing (except refback),
        // remove triggers if existing column if type changed
        // drop columns marked with 'drop'
        for (Column newColumn : mergeTable.getNonInheritedColumns()) {
          Column oldColumn =
              newColumn.getOldName() != null
                  ? oldTable.getLocalColumn(newColumn.getOldName())
                  : oldTable.getLocalColumn(newColumn.getName());

          // drop columns that need dropping
          if (newColumn.isDrop()) {
            oldTable.dropColumn(oldColumn.getName());
          } else
          // if new column and not inherited
          if (oldColumn == null && !newColumn.isRefback()) {
            oldTable.add(newColumn);
            created.add(newColumn.getTableName() + "." + newColumn.getName());
          } else
          // if column exist but type has changed remove triggers
          if (oldColumn != null
              && !newColumn
                  .getColumnType()
                  .getBaseType()
                  .equals(oldColumn.getColumnType().getBaseType())) {
            executeRemoveRefConstraints(targetSchema.getJooq(), oldColumn);
          }
        }
      }
    }

    // second pass,
    // update existing columns to the new types, and new names, reconnect refback
    for (TableMetadata newTable : mergeTableList) {
      if (!newTable.isDrop()) {
        TableMetadata oldTable = targetSchema.getTableMetadata(newTable.getTableName());
        for (Column newColumn : newTable.getNonInheritedColumns()) {
          Column oldColumn =
              newColumn.getOldName() != null
                  ? oldTable.getColumn(newColumn.getOldName()) // when renaming
                  : oldTable.getColumn(newColumn.getName()); // when not renaming

          if (oldColumn != null && !newColumn.isDrop()) {
            if (!created.contains(newColumn.getTableName() + "." + newColumn.getName())) {
              oldTable.alterColumn(oldColumn.getName(), newColumn);
            }
          } else
          // don't forget to add the refbacks
          if (oldColumn == null && newColumn.isRefback()) {
            targetSchema.getTableMetadata(newTable.getTableName()).add(newColumn);
          }
        }
      }
    }

    // finally, drop tables, in reverse dependency order
    Collections.reverse(mergeTableList);
    for (TableMetadata mergeTable : mergeTableList) {
      // idempotent so we only drop if exists
      if (mergeTable.isDrop() && targetSchema.getTableMetadata(mergeTable.getOldName()) != null) {
        targetSchema.getTableMetadata(mergeTable.getOldName()).drop();
      }
    }

    // finally, update settings if changes are provided
    if (!mergeSchema.getSettings().isEmpty()) {
      targetSchema.setSettings(mergeSchema.getSettings());
    }
  }

  public String getName() {
    return getMetadata().getName();
  }

  @Override
  public List<Change> getChanges(int limit) {
    return metadata.getChanges(limit);
  }

  @Override
  public Integer getChangesCount() {
    return metadata.getChangesCount();
  }

  @Override
  public String getSettingValue(String key) {
    String setting = metadata.getSetting(key);
    if (setting == null) {
      throw new MolgenisException("Setting " + key + " not found");
    }
    return setting;
  }

  public boolean hasSetting(String key) {
    return metadata.getSetting(key) != null;
  }

  @Override
  public SqlTable getTableById(String id) {
    Optional<Table> table =
        getTablesSorted().stream().filter(t -> t.getIdentifier().equals(id)).findFirst();
    if (table.isPresent()) return (SqlTable) table.get();
    else return null;
  }

  @Override
  public Table getTableByNameOrIdCaseInsensitive(String tableName) {
    Table table = getTable(tableName);
    if (table == null) {
      table = getTableById(tableName);
    }
    if (table == null) {
      Optional<String> name =
          getTableNames().stream()
              .filter(
                  value ->
                      value
                          .toLowerCase()
                          .replace(" ", "")
                          .equals(tableName.replace(" ", "").toLowerCase()))
              .findFirst();
      if (name.isPresent()) {
        table = getTable(name.get());
      }
    }
    return table;
  }

  @Override
  public boolean hasTableWithNameOrIdCaseInsensitive(String tableName) {
    return getTableNames().stream()
        .anyMatch(
            value ->
                value
                    .toLowerCase()
                    .replace(" ", "")
                    .equals(tableName.replace(" ", "").toLowerCase()));
  }

  public DSLContext getJooq() {
    return ((SqlDatabase) getDatabase()).getJooq();
  }
}
