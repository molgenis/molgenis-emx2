package org.molgenis.emx2.sql;

import org.molgenis.emx2.*;

import java.util.*;

import static org.molgenis.emx2.ColumnType.REFBACK;
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
      }
      for (String discardColumn : discardTable.getLocalColumnNames()) {
        if (!existingTable.getLocalColumnNames().contains(discardColumn))
          errors.add("Column '" + discardTable.getTableName() + "." + discardColumn + " not found");
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
          List<TableMetadata> mergeTableList = new ArrayList<>();
          for (String tableName : mergeSchema.getTableNames()) {
            mergeTableList.add(mergeSchema.getTableMetadata(tableName));
          }

          // uses TableMetadata.compareTo.  todo circular dependencies
          sortTableByDependency(mergeTableList);

          for (TableMetadata mergeTable : mergeTableList) {
            // todo check if table exists

            // first create all tables, and keep refback for last
            if (getTable(mergeTable.getTableName()) == null) {
              this.create(new TableMetadata(mergeTable.getTableName()));
            }
          }
          // first pass
          for (TableMetadata table : mergeTableList) {
            TableMetadata tm = this.getTable(table.getTableName()).getMetadata();
            if (table.getInherit() != null) tm.setInherit(table.getInherit());

            // exclude link-back relations
            for (Column c : table.getLocalColumns()) {
              if (!c.getColumnType().equals(REFBACK)) {
                tm.addColumn(c);
              }
            }

            // table settings
            if (table.getPrimaryKey() != null) tm.setPrimaryKey(table.getPrimaryKey());
            for (String[] unique : table.getUniques()) tm.addUnique(unique);
          }
          // second pass for all linkback relations
          for (TableMetadata table : mergeTableList) {
            for (Column c : table.getLocalColumns()) {
              if (c.getColumnType().equals(REFBACK)) {
                this.getTable(table.getTableName()).getMetadata().addColumn(c);
              }
            }
          }
        });
  }
}
