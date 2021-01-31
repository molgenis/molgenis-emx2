package org.molgenis.emx2.sql;

import static org.molgenis.emx2.ColumnType.REFBACK;
import static org.molgenis.emx2.Command.*;
import static org.molgenis.emx2.sql.SqlColumnExecutor.executeRemoveRefConstraints;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN;
import static org.molgenis.emx2.sql.SqlDatabase.ANONYMOUS;
import static org.molgenis.emx2.sql.SqlSchemaMetadataExecutor.*;
import static org.molgenis.emx2.utils.TableSort.sortTableByDependency;

import java.util.*;
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
    if (tableMetadata.exists()) return new SqlTable(db, tableMetadata);
    else return null;
  }

  @Override
  public List<Table> getTablesSorted() {
    List<TableMetadata> tableMetadata = getMetadata().getTables();
    sortTableByDependency(tableMetadata);
    List<Table> result = new ArrayList<>();
    for (TableMetadata tm : tableMetadata) {
      result.add(new SqlTable(db, (SqlTableMetadata) tm));
    }
    return result;
  }

  @Override
  public void dropTable(String name) {
    getMetadata().drop(name);
  }

  @Override
  public void addMember(String user, String role) {
    executeAddMembers(getMetadata().getJooq(), this, new Member(user, role));
  }

  @Override
  public List<Member> getMembers() {
    // only admin or other members can see
    if (db.getActiveUser() == null
        || ADMIN.equals(db.getActiveUser())
        || getRoleForActiveUser() != null) {
      return executeGetMembers(getMetadata().getJooq(), getMetadata());
    } else {
      return new ArrayList<>();
    }
  }

  @Override
  public void removeMembers(List<Member> members) {
    tx(database -> executeRemoveMembers(getMetadata().getJooq(), this, members));
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
    if (user == null) user = ANONYMOUS;
    user = user.trim();
    for (Member m : executeGetMembers(getMetadata().getJooq(), getMetadata())) {
      if (m.getUser().equals(user)) return m.getRole();
    }
    return null;
  }

  @Override
  public List<String> getInheritedRolesForUser(String user) {
    if (user == null) user = ANONYMOUS;
    user = user.trim();
    // elevate permissions temporarily
    String current = db.getActiveUser();
    db.clearActiveUser();
    try {
      return SqlSchemaMetadataExecutor.getInheritedRoleForUser(
          db.getJooq(), this.getMetadata().getName(), user);
    } finally {
      db.setActiveUser(current);
    }
  }

  @Override
  public String getRoleForActiveUser() {
    return getRoleForUser(db.getActiveUser());
  }

  @Override
  public List<String> getInheritedRolesForActiveUser() {
    return getInheritedRolesForUser(db.getActiveUser());
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
  public Query query(String field, SelectColumn... selection) {
    return new SqlQuery(this.getMetadata(), field, selection);
  }

  @Override
  public void tx(Transaction transaction) {
    db.tx(transaction);
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
    tx(
        dsl -> {
          List<TableMetadata> tables = getMetadata().getTables();
          Collections.reverse(tables);

          // remove whole tables unless columns attached
          for (TableMetadata existingTable : tables) {
            // if no coluns then we delete whole table
            if (discardSchema.getTableMetadata(existingTable.getTableName()) != null) {
              TableMetadata discardTable =
                  discardSchema.getTableMetadata(existingTable.getTableName());
              if (discardTable.getLocalColumnNames().isEmpty()
                  || discardTable
                      .getLocalColumnNames()
                      .containsAll(existingTable.getLocalColumnNames())) {
                dropTable(existingTable.getTableName());
                MetadataUtils.deleteTable(getMetadata().getJooq(), existingTable);
              } else {
                // or column names
                for (String discardColumn : discardTable.getLocalColumnNames()) {
                  Column existingColumn = existingTable.getColumn(discardColumn);
                  existingTable.dropColumn(discardColumn);
                  MetadataUtils.deleteColumn(getMetadata().getJooq(), existingColumn);
                }
              }
            }
          }
        });
  }

  @Override
  public void merge(SchemaMetadata mergeSchema) {
    tx(
        database -> {
          List<TableMetadata> mergeTableList = new ArrayList<>();
          mergeSchema.setDatabase(database);
          for (String tableName : mergeSchema.getTableNames()) {
            mergeTableList.add(mergeSchema.getTableMetadata(tableName));
          }

          // sort dependency order
          sortTableByDependency(mergeTableList);

          // first add all tables not yet in schema
          for (TableMetadata mergeTable : mergeTableList) {
            if (getTable(mergeTable.getTableName()) == null) {
              this.create(new TableMetadata(mergeTable.getTableName()));
            }
          }

          //  add missing columns (except refback), remove constraints of type changes, remove
          // refback
          for (TableMetadata newTable : mergeTableList) {
            TableMetadata oldTable = this.getTable(newTable.getTableName()).getMetadata();

            // update inheritance
            if (newTable.getInherit() != null) {
              if (newTable.getImportSchema() != null) {
                oldTable.setImportSchema(newTable.getImportSchema());
              }
              oldTable.setInherit(newTable.getInherit());
            } else if (oldTable.getInherit() != null) {
              oldTable.removeInherit();
            }

            // update table settings
            oldTable.setSettings(newTable.getSettings());
            oldTable.setDescription(newTable.getDescription());
            oldTable.setJsonldType(newTable.getJsonldType());
            MetadataUtils.saveTableMetadata(db.getJooq(), oldTable);

            // add missing (except refback), remove triggers if existing column if type changed
            // drop ones marked with 'drop'
            for (Column newColumn : newTable.getColumns()) {
              Column oldColumn =
                  newColumn.getOldName() != null
                      ? oldTable.getColumn(newColumn.getOldName())
                      : oldTable.getColumn(newColumn.getName());
              if (oldColumn != null) {
                if (CREATE.equals(newColumn.getCommand())) {
                  throw new MolgenisException(
                      "Cannot create column "
                          + newColumn.getTableName()
                          + "."
                          + newColumn.getName()
                          + ": column exists");
                } else if (newColumn.getCommand() == null) {
                  newColumn.setCommand(ALTER);
                } else if (DROP.equals(newColumn.getCommand())) {
                  // execute drop
                  oldTable.dropColumn(oldColumn.getName());
                }
              } else {
                if (newColumn.getCommand() == null) {
                  if (newColumn.getOldName() == null) {
                    newColumn.setCommand(CREATE);
                  } else {
                    newColumn.setCommand(ALTER);
                  }
                }
              }
              if (CREATE.equals(newColumn.getCommand())) {
                // if column does not exist then create except refback and inheritance
                if (!(oldTable.getInherit() != null
                        && oldTable.getInheritedTable().getColumn(newColumn.getName()) != null)
                    && !newColumn.getColumnType().equals(REFBACK)) {
                  oldTable.add(newColumn);
                }
              } else if (oldColumn != null
                  && !newColumn.getColumnType().equals(oldColumn.getColumnType())) {
                // if column exist but type has changed remove triggers
                executeRemoveRefConstraints(getMetadata().getJooq(), oldColumn);
              }
            }
          }

          // second pass, update existing columns to the new types, and new names, reconnect refback
          for (TableMetadata newTable : mergeTableList) {
            TableMetadata oldTable = this.getTable(newTable.getTableName()).getMetadata();
            for (Column newColumn : newTable.getLocalColumns()) {
              Column oldColumn =
                  newColumn.getOldName() != null
                      ? oldTable.getColumn(newColumn.getOldName()) // when renaming
                      : oldTable.getColumn(newColumn.getName()); // when not renaming
              if (ALTER.equals(newColumn.getCommand())) {
                if (oldColumn == null) {
                  throw new MolgenisException(
                      "Cannot alter column "
                          + newColumn.getTableName()
                          + "."
                          + newColumn.getOldName()
                          + ": column '"
                          + newColumn.getOldName()
                          + "' doesn't exist");
                }
                oldTable.alterColumn(oldColumn.getName(), newColumn);
              }

              // create refback relations for new and existing
              if (newColumn.getColumnType().equals(REFBACK)) {
                this.getTable(newTable.getTableName()).getMetadata().add(newColumn);
              }
            }
          }
        });

    db.getListener().schemaChanged(this.metadata.getName());
  }

  public String getName() {
    return getMetadata().getName();
  }
}
