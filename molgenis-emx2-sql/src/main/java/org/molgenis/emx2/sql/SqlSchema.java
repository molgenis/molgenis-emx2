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
  public void discard(SchemaMetadata toBeRemoved) {
    // check if all tables and columns are known
    List<String> errors = new ArrayList<>();
    for (TableMetadata toBeRemovedTable : toBeRemoved.getTables()) {
      TableMetadata t = getMetadata().getTableMetadata(toBeRemovedTable.getTableName());
      if (t == null) {
        errors.add("Table '" + t.getTableName() + " not found");
      }
      for (String name : toBeRemovedTable.getLocalColumnNames()) {
        if (!t.getLocalColumnNames().contains(name))
          errors.add("Column '" + toBeRemovedTable.getTableName() + "." + name + " not found");
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
          for (TableMetadata t : tables) {
            // if no coluns then we delete whole table
            if (toBeRemoved.getTableMetadata(t.getTableName()) != null) {
              TableMetadata toBeRemovedTable = toBeRemoved.getTableMetadata(t.getTableName());
              if (toBeRemovedTable.getLocalColumnNames().size() == 0
                  || toBeRemovedTable.getLocalColumnNames().containsAll(t.getLocalColumnNames())) {
                dropTable(t.getTableName());
                MetadataUtils.deleteTable(getMetadata().getJooq(), t);
              } else {
                // or column names
                for (String name : toBeRemovedTable.getLocalColumnNames()) {
                  Column c = t.getColumn(name);
                  t.removeColumn(name);
                  MetadataUtils.deleteColumn(getMetadata().getJooq(), c);
                }
              }
            }
          }
        });
  }

  @Override
  public void merge(SchemaMetadata from) {
    tx(
        database -> {
          List<TableMetadata> tableList = new ArrayList<>();
          for (String tableName : from.getTableNames()) {
            tableList.add(from.getTableMetadata(tableName));
          }

          // uses TableMetadata.compareTo.  todo circular dependencies
          sortTableByDependency(tableList);

          for (TableMetadata table : tableList) {
            // first create all tables, and keep refback for last
            if (getTable(table.getTableName()) == null) {
              this.create(new TableMetadata(table.getTableName()));
            }
          }
          // first pass
          for (TableMetadata table : tableList) {
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
          for (TableMetadata table : tableList) {
            for (Column c : table.getLocalColumns()) {
              if (c.getColumnType().equals(REFBACK)) {
                this.getTable(table.getTableName()).getMetadata().addColumn(c);
              }
            }
          }
        });
  }
}
