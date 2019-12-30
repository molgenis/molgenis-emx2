package org.molgenis.emx2.sql;

import org.jooq.DSLContext;
import org.molgenis.emx2.*;

import java.util.*;

import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.sql.SqlColumnUtils.executeRemoveConstraints;
import static org.molgenis.emx2.sql.SqlSchemaMetadataUtils.*;
import static org.molgenis.emx2.utils.TableSort.sortTableByDependency;

public class SqlSchema implements Schema {
  private SqlDatabase db;
  private SqlSchemaMetadata metadata;

  public SqlSchema(SqlDatabase db, SqlSchemaMetadata metadata) {
    this.db = db;
    this.metadata = metadata;
  }

  @Override
  public SqlTable getTable(String name) {
    TableMetadata tableMetadata = getMetadata().getTableMetadata(name);
    if (tableMetadata == null) return null;
    if (tableMetadata.exists()) return new SqlTable(db, tableMetadata);
    else return null;
  }

  @Override
  public void dropTable(String name) {
    getMetadata().drop(name);
  }

  @Override
  public void addMembers(Member... members) {
    this.addMembers(Arrays.asList(members));
  }

  @Override
  public void addMember(String user, String role) {
    this.addMembers(new Member(user, role));
  }

  @Override
  public void addMembers(List<Member> members) {
    tx(database -> executeAddMembers(getMetadata().getJooq(), this, members));
  }

  @Override
  public List<Member> getMembers() {
    return executeGetMembers(getMetadata().getJooq(), getMetadata());
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
    return executeGetRoles(getMetadata().getJooq(), this);
  }

  @Override
  public String getRoleForUser(String user) {
    if (user == null) return null;
    user = user.trim();
    for (Member m : getMembers()) {
      // todo can become expensive with many users
      if (m.getUser().equals(user)) return m.getRole();
    }
    return null;
  }

  @Override
  public Table create(TableMetadata metadata) {
    getMetadata().create(metadata);
    return getTable(metadata.getTableName());
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
    if (errors.size() > 0) {
      throw new MolgenisException(
          "Discard failed",
          "Discard of tables out of schema "
              + getMetadata().getName()
              + " failed: "
              + String.join("\n", errors));
    }

    // get all tables, sorted and use that as scaffold
    tx(
        db -> {
          List<TableMetadata> tables = getMetadata().getTables();
          Collections.reverse(tables);

          // remove whole tables unless columns attached
          for (TableMetadata existingTable : tables) {
            // if no coluns then we delete whole table
            if (discardSchema.getTableMetadata(existingTable.getTableName()) != null) {
              TableMetadata discardTable =
                  discardSchema.getTableMetadata(existingTable.getTableName());
              if (discardTable.getLocalColumnNames().size() == 0
                  || discardTable
                      .getLocalColumnNames()
                      .containsAll(existingTable.getLocalColumnNames())) {
                dropTable(existingTable.getTableName());
                MetadataUtils.deleteTable(getMetadata().getJooq(), existingTable);
              } else {
                // or column names
                for (String discardColumn : discardTable.getLocalColumnNames()) {
                  Column existingColumn = existingTable.getColumn(discardColumn);
                  existingTable.removeColumn(discardColumn);
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
          DSLContext jooq = getMetadata().getJooq();

          List<TableMetadata> mergeTableList = new ArrayList<>();
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

          // first add missing columns (except refback), remove constraints of type changes, remove
          // refback
          for (TableMetadata newTable : mergeTableList) {
            TableMetadata oldTable = this.getTable(newTable.getTableName()).getMetadata();

            // update inheritance
            if (newTable.getInherit() != null) {
              oldTable.setInherit(newTable.getInherit());
            } else if (oldTable.getInherit() != null) {
              oldTable.removeInherit();
            }

            // add missing (except refback), remove triggers if existing column if type changed
            for (Column newColumn : newTable.getLocalColumns()) {
              Column oldColumn = oldTable.getColumn(newColumn.getName());
              if (oldTable.getColumn(newColumn.getName()) == null) {
                // if column does not exist then create except refback
                if (!newColumn.getColumnType().equals(REFBACK)) {
                  oldTable.addColumn(newColumn);
                }
              } else if (!newColumn.getColumnType().equals(oldColumn.getColumnType())) {

                // if column exist but type has changed remove triggers
                executeRemoveConstraints(getMetadata().getJooq(), oldColumn);
              }
            }

            // update unique constraints if not yet exist
            if (newTable.getPrimaryKey() != null) oldTable.setPrimaryKey(newTable.getPrimaryKey());
            for (String[] unique : newTable.getUniques()) oldTable.addUnique(unique);
          }

          // second pass, update to the new types, reconnect refback
          for (TableMetadata newTable : mergeTableList) {
            TableMetadata oldTable = this.getTable(newTable.getTableName()).getMetadata();

            for (Column newColumn : newTable.getLocalColumns()) {
              Column oldColumn = oldTable.getColumn(newColumn.getName());
              // update the types
              if (oldColumn != null
                  && !newColumn.getColumnType().equals(oldColumn.getColumnType())) {
                oldTable.alterColumn(newColumn);

                // todo reconnect refback
              }

              // create new refback relations
              if (newColumn.getColumnType().equals(REFBACK)) {
                this.getTable(newTable.getTableName()).getMetadata().addColumn(newColumn);
              }
            }
          }
        });
  }
}
