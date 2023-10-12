package org.molgenis.emx2.sql;

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
    if (tableMetadata == null) return null;
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
      return getMetadata().getIneritedRolesForUser(user);
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
    // check if all tables and columns are known
    List<String> errors = new ArrayList<>();
    for (TableMetadata discardTable : discardSchema.getTables()) {
      TableMetadata existingTable = getMetadata().getTableMetadata(discardTable.getTableName());
      if (existingTable == null) {
        errors.add("Table '" + discardTable.getTableName() + " not found");
      } else {
        for (String discardColumn : discardTable.getLocalColumnNames()) {
          if (!existingTable.getLocalColumnNames().contains(discardColumn))
            errors.add(
                "Column '" + discardTable.getTableName() + "." + discardColumn + " not found");
        }
      }
    }
    if (!errors.isEmpty()) {
      throw new MolgenisException(
          "Discard failed: Discard of tables out of schema "
              + getMetadata().getName()
              + " failed: "
              + String.join("\n", errors));
    }

    // get all tables, sorted and use that as scaffold
    tx(db -> discardTransaction((SqlDatabase) db, discardSchema.getName()));
    this.getDatabase().getListener().schemaChanged(this.getName());
  }

  private static void discardTransaction(SqlDatabase db, String schemaName) {
    Schema schema = db.getSchema(schemaName);
    SchemaMetadata schemaMetadata = db.getSchema(schemaName).getMetadata();
    List<TableMetadata> tables = schemaMetadata.getTables();
    Collections.reverse(tables);

    // remove whole tables unless columns attached
    for (TableMetadata existingTable : tables) {
      // if no coluns then we delete whole table
      if (schemaMetadata.getTableMetadata(existingTable.getTableName()) != null) {
        TableMetadata discardTable = schemaMetadata.getTableMetadata(existingTable.getTableName());
        if (discardTable.getLocalColumnNames().isEmpty()
            || discardTable
                .getLocalColumnNames()
                .containsAll(existingTable.getLocalColumnNames())) {
          schema.dropTable(existingTable.getTableName());
          MetadataUtils.deleteTable(db.getJooq(), existingTable);
        } else {
          // or column names
          for (String discardColumn : discardTable.getLocalColumnNames()) {
            Column existingColumn = existingTable.getColumn(discardColumn);
            existingTable.dropColumn(discardColumn);
            MetadataUtils.deleteColumn(db.getJooq(), existingColumn);
          }
        }
      }
    }
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
    SqlSchema targetSchema = (SqlSchema) database.getSchema(targetSchemaName);

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
      Table oldTableSource =
          mergeTable.getOldName() == null
              ? targetSchema.getTable(mergeTable.getTableName())
              : targetSchema.getTable(mergeTable.getOldName());
      TableMetadata oldTable = oldTableSource != null ? oldTableSource.getMetadata() : null;

      // set oldName in case table does exist, and oldName was not provided
      if (mergeTable.getOldName() == null && oldTable != null) {
        mergeTable.setOldName(oldTable.getTableName());
      }

      // create table if not exists
      if (oldTable == null && !mergeTable.isDrop()) {
        targetSchema.create(
            new TableMetadata(mergeTable.getTableName())
                .setTableType(mergeTable.getTableType())); // only the name and type
      } else if (oldTable != null && !oldTable.getTableName().equals(mergeTable.getTableName())) {
        targetSchema.getMetadata().renameTable(oldTable, mergeTable.getTableName());
      }
    }

    // for create/alter
    //  add missing columns (except refback),
    //  remove triggers in case of table name or column type changes
    //  remove refback
    List<String> created = new ArrayList<>();
    for (TableMetadata mergeTable : mergeTableList) {

      if (!mergeTable.isDrop()) {
        TableMetadata oldTable = targetSchema.getTable(mergeTable.getTableName()).getMetadata();

        // set inheritance
        if (mergeTable.getInherit() != null) {
          if (mergeTable.getImportSchema() != null) {
            oldTable.setImportSchema(mergeTable.getImportSchema());
          }
          oldTable.setInherit(mergeTable.getInherit());
        } else if (oldTable.getInherit() != null) {
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
        MetadataUtils.saveTableMetadata(targetSchema.getMetadata().getJooq(), oldTable);

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
          }
        }
      }
    }

    // second pass,
    // update existing columns to the new types, and new names, reconnect refback
    for (TableMetadata newTable : mergeTableList) {
      if (!newTable.isDrop()) {
        TableMetadata oldTable = targetSchema.getTable(newTable.getTableName()).getMetadata();
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
            targetSchema.getTable(newTable.getTableName()).getMetadata().add(newColumn);
          }
        }
      }
    }

    // finally, drop tables, in reverse dependency order
    Collections.reverse(mergeTableList);
    for (TableMetadata mergeTable : mergeTableList) {
      // idempotent so we only drop if exists
      if (mergeTable.isDrop() && targetSchema.getTable(mergeTable.getOldName()) != null) {
        targetSchema.getTable(mergeTable.getOldName()).getMetadata().drop();
      }
    }

    // finally, update settings if changes are provided
    if (!mergeSchema.getSettings().isEmpty()) {
      targetSchema.getMetadata().setSettings(mergeSchema.getSettings());
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

  public DSLContext getJooq() {
    return ((SqlDatabase) getDatabase()).getJooq();
  }
}
