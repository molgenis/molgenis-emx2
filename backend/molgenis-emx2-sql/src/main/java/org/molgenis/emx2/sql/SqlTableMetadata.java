package org.molgenis.emx2.sql;

import org.jooq.*;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.MolgenisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.sql.Constants.*;
import static org.molgenis.emx2.sql.SqlColumnUtils.reapplyRefbackContraints;
import static org.molgenis.emx2.sql.SqlTableMetadataExecutor.*;

class SqlTableMetadata extends TableMetadata {
  private static final String SET_INHERITANCE_FAILED = "Set inheritance failed";
  private Database db;
  private static Logger logger = LoggerFactory.getLogger(SqlTableMetadata.class);

  SqlTableMetadata(Database db, SqlSchemaMetadata schema, TableMetadata metadata) {
    super(schema, metadata);
    this.db = db;
  }

  void load() {
    long start = System.currentTimeMillis();
    clearCache();
    TableMetadata temp = new TableMetadata(getSchema(), getTableName());
    MetadataUtils.loadTableMetadata(getJooq(), temp);
    this.copy(temp);
    log(start, "RELOAD");
  }

  @Override
  public Column getColumn(String name) {
    Column c = super.getColumn(name);
    if (c == null) {
      this.load(); // if not cached then try to reload
    }
    return super.getColumn(name);
  }

  @Override
  public List<Column> getLocalColumns() {
    List<Column> result = super.getLocalColumns();
    if (result.isEmpty()) {
      this.load();
    }
    return result;
  }

  @Override
  public TableMetadata addColumn(Column column) {

    if (getColumn(column.getName()) != null) {
      // check if primary key not yet on local columns
      boolean found = false;
      if (column.getName().equals(getPrimaryKey())) {
        for (Column c : getLocalColumns()) {
          if (c.getName().equals(getPrimaryKey())) {
            found = true;
          }
        }
      }
      // if exists indeed duplicate, otherwise let it happen
      if (found) {
        throw new MolgenisException(
            "Add column failed",
            "Duplicate name; column with name "
                + getTableName()
                + "."
                + column.getName()
                + " already exists");
      }
    }
    long start = System.currentTimeMillis();
    db.tx(
        dsl -> {
          Column result = new Column(this, column);
          SqlColumnUtils.executeCreateColumn(getJooq(), result);
          super.addColumn(result);
        });
    log(start, "added column '" + column.getName() + "' to ");
    return this;
  }

  @Override
  public TableMetadata alterColumn(Column column) {
    Column oldColumn = getColumn(column.getName());
    if (oldColumn == null) {
      throw new MolgenisException(
          "Alter column failed",
          "Column  '" + getTableName() + "." + column.getName() + "' does not exist");
    }
    db.tx(
        dsl -> {
          Column newColumn = new Column(this, column);
          SqlColumnUtils.executeAlterColumn(getJooq(), oldColumn, newColumn);
          super.alterColumn(newColumn);
          reapplyRefbackContraints(oldColumn, newColumn);
        });
    return this;
  }

  @Override
  public void dropColumn(String name) {
    long start = System.currentTimeMillis();
    if (getColumn(name) == null) return; // return silently, idempotent
    db.tx(
        dsl -> {
          SqlColumnUtils.executeRemoveColumn(getJooq(), getColumn(name));
          super.columns.remove(name);
          SqlTableMetadataExecutor.updateSearchIndexTriggerFunction(getJooq(), this);
        });
    log(start, "removed column '" + name + "' from ");
  }

  @Override
  public TableMetadata setInherit(String otherTable) {
    // todo split first inherit from change
    long start = System.currentTimeMillis();
    if (getInherit() != null)
      throw new MolgenisException(
          SET_INHERITANCE_FAILED,
          "Table '"
              + getTableName()
              + "'can only extend one table. Therefore it cannot extend '"
              + otherTable
              + "' because it already extends other table '"
              + getInherit()
              + "'");
    TableMetadata other = getSchema().getTableMetadata(otherTable);
    if (other == null)
      throw new MolgenisException(
          SET_INHERITANCE_FAILED, "Other table '" + otherTable + "' does not exist in this schema");

    if (other.getPrimaryKey() == null)
      throw new MolgenisException(
          SET_INHERITANCE_FAILED,
          "To extend table '" + otherTable + "' it must have primary key set");
    db.tx(
        tdb -> {
          // extends means we copy primary key column from parent to child, make it foreign key to
          // parent, and make it primary key of this table also.
          executeSetInherit(getJooq(), this, other);
          super.inherit = otherTable;
        });
    log(start, "set inherit on ");
    return this;
  }

  @Override
  public TableMetadata removeInherit() {
    throw new RuntimeException("removeInherit not yet implemented");
  }

  @Override
  public TableMetadata setPrimaryKey(String columnName) {
    long start = System.currentTimeMillis();
    if (columnName == null)
      throw new MolgenisException("Set primary key failed", "Null was provided");
    if (columnName.equals(getPrimaryKey()) && this.getInherit() == null) return this;
    if (getInherit() != null && !columnName.equals(getInheritedTable().getPrimaryKey()))
      throw new MolgenisException(
          "Set primary key failed",
          "Primary key cannot be set on table '"
              + getTableName()
              + "' because inherits its primary key from other table '"
              + getInherit()
              + "'");
    db.tx(
        dsl -> {
          SqlTableMetadataExecutor.executeSetPrimaryKey(getJooq(), this, columnName);
          super.setPrimaryKey(columnName);
          MetadataUtils.saveColumnMetadata(getJooq(), getPrimaryKeyColumn());
        });
    log(start, "set primary key " + List.of(columnName) + " on ");
    return this;
  }

  @Override
  public TableMetadata addUnique(String... columnNames) {
    long start = System.currentTimeMillis();
    if (isUnique(columnNames)) return this; // idempotent, we silently ignore
    // check if the columns exists
    for (String columnName : columnNames) {
      Column c = getColumn(columnName);
      if (c == null)
        throw new MolgenisException(
            "Add unique failed",
            "Column '" + columnName + "' is not known in table " + getTableName());
    }
    // check if already exists
    db.tx(
        dsl -> {
          executeCreateUnique(getJooq(), this, columnNames);
          super.addUnique(columnNames);
        });
    log(start, "added unique '" + List.of(columnNames) + "' to ");
    return this;
  }

  @Override
  public void removeUnique(String... columnNames) {
    long start = System.currentTimeMillis();
    // try to find the right unique
    String[] correctOrderedNames = null;
    List list1 = Arrays.asList(columnNames);
    for (String[] unique : getUniques()) {
      List list2 = Arrays.asList(unique);
      if (list1.containsAll(list2) && list2.containsAll(list1)) {
        correctOrderedNames = unique;
      }
    }
    if (correctOrderedNames == null) {
      throw new MolgenisException(
          "Remove unique failed",
          "Unique constraint consisting of columns " + list1 + "could not be found. ");
    }
    final String[] finalNames = correctOrderedNames;
    db.tx(
        dsl -> {
          executeRemoveUnique(getJooq(), this, finalNames);
          super.removeUnique(finalNames);
        });
    log(start, "removed unique '" + columnNames + "' to ");
  }

  @Override
  public void enableRowLevelSecurity() {
    // todo, study if we need different row level security in inherited tables
    this.addColumn(column(MG_EDIT_ROLE).index(true));

    getJooq().execute("ALTER TABLE {0} ENABLE ROW LEVEL SECURITY", getJooqTable(this));
    getJooq()
        .execute(
            "CREATE POLICY {0} ON {1} USING (pg_has_role(session_user, {2}, 'member')) WITH CHECK (pg_has_role(session_user, {2}, 'member'))",
            name("RLS/" + getSchema().getName() + "/" + getTableName()),
            getJooqTable(this),
            name(MG_EDIT_ROLE));
    // set RLS on the table
    // add policy for 'viewer' and 'editor'.
  }

  @Override
  public boolean exists() {
    // first look at already loaded metadata, in case of no columns, check the underlying table
    if (!getColumns().isEmpty()) {
      return true;
    }
    // jooq doesn't have operator for this, so by hand. Might be slow
    return 0
        < getJooq()
            .select(count())
            .from(name("information_schema", "tables"))
            .where(
                field("table_schema")
                    .eq(getSchema().getName())
                    .and(field("table_name").eq(getTableName())))
            .fetchOne(0, Integer.class);
  }

  public DSLContext getJooq() {
    return ((SqlDatabase) db).getJooq();
  }

  private void log(long start, String message) {
    String user = db.getActiveUser();
    if (user == null) user = "molgenis";
    if (logger.isInfoEnabled()) {
      logger.info(
          "{} {} {} in {}ms",
          user,
          message,
          getJooqTable(this),
          (System.currentTimeMillis() - start));
    }
  }
}
