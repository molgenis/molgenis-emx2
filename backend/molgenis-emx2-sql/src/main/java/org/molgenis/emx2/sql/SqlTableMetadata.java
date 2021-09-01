package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Constants.MG_EDIT_ROLE;
import static org.molgenis.emx2.Constants.MG_TABLECLASS;
import static org.molgenis.emx2.sql.MetadataUtils.deleteColumn;
import static org.molgenis.emx2.sql.MetadataUtils.saveColumnMetadata;
import static org.molgenis.emx2.sql.SqlColumnExecutor.*;
import static org.molgenis.emx2.sql.SqlTableMetadataExecutor.*;

import java.util.List;
import org.jooq.DSLContext;
import org.molgenis.emx2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SqlTableMetadata extends TableMetadata {
  private static Logger logger = LoggerFactory.getLogger(SqlTableMetadata.class);

  SqlTableMetadata(SqlSchemaMetadata schema, TableMetadata metadata) {
    super(schema, metadata);
  }

  @Override
  public TableMetadata add(Column... column) {
    getDatabase()
        .tx(
            db -> {
              sync(addTransaction(db, getSchemaName(), getTableName(), column));
            });
    return this;
  }

  // static to ensure we don't touch 'this' until complete
  private static SqlTableMetadata addTransaction(
      Database db, String schemaName, String tableName, Column[] column) {
    SqlTableMetadata tm =
        (SqlTableMetadata) db.getSchema(schemaName).getMetadata().getTableMetadata(tableName);

    // first per-column actions, then multi-column action such as composite keys/refs
    int position = MetadataUtils.getMaxPosition(tm.getJooq(), schemaName) + 1;
    for (Column c : column) {
      long start = System.currentTimeMillis();
      if (tm.getLocalColumn(c.getName()) != null) {
        tm.alterColumn(c);
      } else {
        Column newColumn = new Column(tm, c);
        if (tm.getInherit() != null
            && tm.getInheritedTable().getColumn(c.getName()) != null
            // this column is replicated in all subclass tables
            && !c.getName().equals(MG_TABLECLASS)) {
          throw new MolgenisException(
              "Cannot add column "
                  + tm.getTableName()
                  + "."
                  + c.getName()
                  + ": column exists in inherited class "
                  + tm.getInherit());
        }
        if (!newColumn.isHeading()) {
          validateColumn(newColumn);
          if (newColumn.getPosition() == null) {
            // positions are asumed to number up in a schema
            newColumn.setPosition(position++);
          }
          executeCreateColumn(tm.getJooq(), newColumn);
          tm.columns.put(c.getName(), newColumn);
          if (newColumn.getKey() > 0) {
            createOrReplaceKey(
                tm.getJooq(),
                newColumn.getTable(),
                newColumn.getKey(),
                newColumn.getTable().getKeyFields(newColumn.getKey()));
          }
          executeCreateRefConstraints(tm.getJooq(), newColumn);
        } else {
          saveColumnMetadata(tm.getJooq(), newColumn);
          tm.columns.put(c.getName(), newColumn);
        }
        log(tm, start, "added column '" + newColumn.getName() + "' to table " + tm.getTableName());
      }
    }
    return tm;
  }

  @Override
  public TableMetadata alterName(String newName) {
    long start = System.currentTimeMillis();
    String oldName = getTableName();
    if (!getTableName().equals(newName)) {
      getDatabase()
          .tx(
              db -> {
                sync(alterNameTransaction(db, getSchemaName(), getTableName(), newName));
              });
      getDatabase().getListener().schemaChanged(getSchemaName());
      log(start, "altered table from '" + oldName + "' to  " + getTableName());
    }
    return this;
  }

  // ensure the transaction has no side effects on 'this' until completed
  private static SqlTableMetadata alterNameTransaction(
      Database db, String schemaName, String tableName, String newName) {
    SqlTableMetadata tm =
        (SqlTableMetadata) db.getSchema(schemaName).getMetadata().getTableMetadata(tableName);

    // drop triggers for this table
    for (Column column : tm.getStoredColumns()) {
      SqlColumnExecutor.executeRemoveRefConstraints(tm.getJooq(), column);
    }

    // rename table and triggers
    SqlTableMetadataExecutor.executeAlterName(tm.getJooq(), tm, newName);

    // update metadata
    MetadataUtils.alterTableName(tm.getJooq(), tm, newName);
    tm.tableName = newName;

    // recreate triggers for this table
    for (Column column : tm.getStoredColumns()) {
      SqlColumnExecutor.executeCreateRefConstraints(tm.getJooq(), column);
    }

    return tm;
  }

  @Override
  public TableMetadata alterColumn(String columnName, Column column) {
    // ignore mg_ columns
    if (column.getName().startsWith("mg_")) return this;

    Column oldColumn = getColumn(columnName);
    if (oldColumn == null) {
      throw new MolgenisException(
          "Alter column failed: Column  '"
              + getTableName()
              + "."
              + column.getName()
              + "' does not exist");
    }
    if (getInherit() != null && getInheritedTable().getColumn(columnName) != null) {
      throw new MolgenisException(
          "Alter column "
              + getTableName()
              + "."
              + columnName
              + " failed: column is part of inherited table "
              + getInherit());
    }
    if (getInherit() != null && getInheritedTable().getColumn(column.getName()) != null) {
      throw new MolgenisException(
          "Rename column from "
              + getTableName()
              + "."
              + columnName
              + " to "
              + getTableName()
              + "."
              + column.getName()
              + " failed: column '"
              + column.getName()
              + "' is part of inherited table "
              + getInherit());
    }
    getDatabase()
        .tx(
            db -> {
              sync(alterColumnTransaction(getSchemaName(), getTableName(), columnName, column, db));
            });
    return this;
  }

  // static to ensure we don't touch 'this' until transaction succesfull
  private static SqlTableMetadata alterColumnTransaction(
      String schemaName, String tableName, String columnName, Column column, Database db) {
    SqlTableMetadata tm =
        (SqlTableMetadata) db.getSchema(schemaName).getMetadata().getTableMetadata(tableName);
    Column newColumn = new Column(tm, column);
    Column oldColumn = tm.getColumn(columnName);
    validateColumn(newColumn);

    // check if reference and of different size
    if (newColumn.isRefArray()
        && !newColumn.getName().equals(oldColumn.getName())
        && !newColumn.getColumnType().equals(oldColumn.getColumnType())
        && newColumn.getRefTable().getPrimaryKeyFields().size() > 1) {
      throw new MolgenisException(
          "Alter column of '"
              + oldColumn.getName()
              + " failed: REF_ARRAY is not supported for composite keys of table "
              + newColumn.getRefTableName());
    }

    if (newColumn.getKey() == 1) {
      newColumn.setRequired(true);
    }

    // if changing 'ref' then check if not refBack exists
    if (!oldColumn.getColumnType().equals(newColumn.getColumnType())) {
      tm.checkNotRefback(columnName, oldColumn);
    }

    // drop old key, if touched
    if (oldColumn.getKey() > 0 && newColumn.getKey() != oldColumn.getKey()) {
      executeDropKey(tm.getJooq(), oldColumn.getTable(), oldColumn.getKey());
    }

    // drop referential constraints around this column
    executeRemoveRefConstraints(tm.getJooq(), oldColumn);

    // remove refBacks if exist
    executeRemoveRefback(oldColumn, newColumn);

    // rename and retype if needed
    executeAlterType(tm.getJooq(), oldColumn, newColumn);
    executeAlterName(tm.getJooq(), oldColumn, newColumn);

    // change required?
    // only applies to key=1
    if ((oldColumn.isPrimaryKey() || newColumn.isPrimaryKey())
        && oldColumn.isRequired()
        && !oldColumn.isRequired() == newColumn.isRequired()) {
      executeSetRequired(tm.getJooq(), newColumn);
    }
    // update the metadata so we can use it for new keys and references
    if (column.getPosition() == null) {
      column.setPosition(tm.columns.get(columnName).getPosition());
    }
    // remove the old
    tm.columns.remove(columnName);
    // add the new
    tm.columns.put(column.getName(), column);

    // reapply ref constrainst
    executeCreateRefConstraints(tm.getJooq(), newColumn);

    // check if refBack constraints need updating
    reapplyRefbackContraints(oldColumn, newColumn);

    // create/update key, if touched
    if (newColumn.getKey() != oldColumn.getKey()) {
      createOrReplaceKey(tm.getJooq(), tm, newColumn.getKey(), tm.getKeyFields(newColumn.getKey()));
    }

    // delete old column if name changed, then save any other metadata changes
    if (!oldColumn.getName().equals(newColumn.getName())) deleteColumn(tm.getJooq(), oldColumn);
    saveColumnMetadata(tm.getJooq(), newColumn);

    return tm;
  }

  private void checkNotRefback(String name, Column oldColumn) {
    if (oldColumn.isReference()) {
      for (Column c : oldColumn.getRefTable().getColumns()) {
        if (c.isRefback()
            && c.getRefTableName().equals(oldColumn.getTableName())
            && oldColumn.getName().equals(c.getRefBack())) {
          throw new MolgenisException(
              "Drop/alter column '"
                  + name
                  + "' failed: cannot remove reference while refBack for it exists ("
                  + c.getTableName()
                  + "."
                  + c.getRefBackColumn());
        }
      }
    }
  }

  @Override
  public void dropColumn(String name) {
    Column column = getColumn(name);
    if (column == null) {
      throw new MolgenisException("Drop column " + name + " failed: column does not exist");
    }
    // if changing 'ref' then check if not refBack exists
    checkNotRefback(name, column);

    long start = System.currentTimeMillis();
    if (getColumn(name) == null) return; // return silently, idempotent
    getDatabase()
        .tx(
            db -> {
              sync(dropColumnTransaction(db, getSchemaName(), getTableName(), name));
            });
    log(start, "removed column '" + name + "' from ");
  }

  private static SqlTableMetadata dropColumnTransaction(
      Database db, String schemaName, String tableName, String columnName) {
    SqlTableMetadata tm =
        (SqlTableMetadata) db.getSchema(schemaName).getTable(tableName).getMetadata();
    DSLContext jooq = ((SqlDatabase) db).getJooq();
    SqlColumnExecutor.executeRemoveColumn(jooq, tm.getColumn(columnName));
    tm.columns.remove(columnName);
    return tm;
  }

  @Override
  public TableMetadata setInherit(String otherTable) {
    long start = System.currentTimeMillis();
    if (getImportSchema() != null && getSchema().getTableMetadata(otherTable) != null) {
      throw new MolgenisException(
          "Inheritance failed: cannot extend schema.table '"
              + getImportSchema()
              + "."
              + otherTable
              + " because table of that name already exists in this schema ("
              + getSchemaName()
              + ')');
    }
    if (getInherit() != null) {
      if (getInherit().equals(otherTable)) {
        return this; // nothing to do
      } else {
        throw new MolgenisException(
            "Table '"
                + getTableName()
                + "'can only extend one table. Therefore it cannot extend '"
                + otherTable
                + "' because it already extends other table '"
                + getInherit()
                + "'");
      }
    }
    TableMetadata other;
    if (getImportSchema() != null) {
      // check for duplicate table name
      Schema otherSchema = getSchema().getDatabase().getSchema(getImportSchema());
      if (otherSchema == null || otherSchema.getMetadata().getTableMetadata(otherTable) == null) {
        throw new MolgenisException(
            "Inheritance failed. Other schema.table '"
                + getImportSchema()
                + "."
                + otherTable
                + "' does not exist in this database");
      }
      other = otherSchema.getMetadata().getTableMetadata(otherTable);
    } else {
      other = getSchema().getTableMetadata(otherTable);
      if (other == null)
        throw new MolgenisException(
            "Inheritance failed. Other table '" + otherTable + "' does not exist in this schema");
    }
    if (other.getPrimaryKeys().isEmpty())
      throw new MolgenisException(
          "Set inheritance failed: To extend table '"
              + otherTable
              + "' it must have primary key set");
    getDatabase()
        .tx(
            tdb -> {
              // extends means we copy primary key column from parent to child, make it foreign key
              // to
              // parent, and make it primary key of this table also.
              sync(
                  setInheritTransaction(
                      tdb,
                      getSchemaName(),
                      getTableName(),
                      getImportSchema() != null ? getImportSchema() : getSchemaName(),
                      otherTable));
            });
    log(start, "set inherit on ");
    super.setInherit(otherTable);
    return this;
  }

  // static function to ensure this is not altered until end of transaction
  private static SqlTableMetadata setInheritTransaction(
      Database db,
      String schemaName,
      String tableName,
      String inheritSchema,
      String inheritedName) {
    DSLContext jooq = ((SqlDatabase) db).getJooq();
    SqlTableMetadata tm =
        (SqlTableMetadata) db.getSchema(schemaName).getTable(tableName).getMetadata();
    TableMetadata om = db.getSchema(inheritSchema).getTable(inheritedName).getMetadata();
    executeSetInherit(jooq, tm, om);
    tm.inherit = inheritedName;
    MetadataUtils.saveTableMetadata(jooq, tm);
    return tm;
  }

  @Override
  public TableMetadata removeInherit() {
    throw new MolgenisException("removeInherit not yet implemented");
  }

  @Override
  public SqlTableMetadata setSettings(List<Setting> settings) {
    getDatabase()
        .tx(
            db -> {
              sync(
                  setSettingTransaction(
                      (SqlDatabase) db, getSchemaName(), getTableName(), settings));
            });
    getDatabase().getListener().schemaChanged(getSchemaName());
    return this;
  }

  private static SqlTableMetadata setSettingTransaction(
      SqlDatabase db, String schemaName, String tableName, List<Setting> settings) {
    SqlSchemaMetadata schema = db.getSchema(schemaName).getMetadata();
    SqlTableMetadata tm = schema.getTableMetadata(tableName);
    for (Setting setting : settings) {
      MetadataUtils.saveSetting(db.getJooq(), schema, tm, setting);
      tm.settings.put(setting.getKey(), setting);
    }
    return tm;
  }

  @Override
  public void removeSetting(String key) {
    getDatabase()
        .tx(
            db ->
                sync(
                    removeSettingTransaction(
                        (SqlDatabase) db, getSchemaName(), getTableName(), key)));
    getDatabase().getListener().schemaChanged(getSchemaName());
  }

  private static SqlTableMetadata removeSettingTransaction(
      SqlDatabase db, String schemaName, String tableName, String key) {
    SqlSchemaMetadata schema = db.getSchema(schemaName).getMetadata();
    SqlTableMetadata tm = schema.getTableMetadata(tableName);
    MetadataUtils.deleteSetting(db.getJooq(), schema, tm, new Setting(key, null));
    tm.settings.remove(key);
    return tm;
  }

  @Override
  public void enableRowLevelSecurity() {
    this.add(column(MG_EDIT_ROLE).setIndex(true));

    getJooq().execute("ALTER TABLE {0} ENABLE ROW LEVEL SECURITY", getJooqTable());
    getJooq()
        .execute(
            "CREATE POLICY {0} ON {1} USING (pg_has_role(session_user, {2}, 'member')) WITH CHECK (pg_has_role(session_user, {2}, 'member'))",
            name("RLS/" + getSchema().getName() + "/" + getTableName()),
            getJooqTable(),
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
    return getDatabase().getJooq();
  }

  private static void log(SqlTableMetadata tableMetadata, long start, String message) {
    String user = tableMetadata.getDatabase().getActiveUser();
    if (user == null) user = "molgenis";
    if (logger.isInfoEnabled()) {
      logger.info(
          "{} {} {} in {}ms",
          user,
          message,
          tableMetadata.getJooqTable(),
          (System.currentTimeMillis() - start));
    }
  }

  private void log(long start, String message) {
    String user = getDatabase().getActiveUser();
    if (user == null) user = "molgenis";
    if (logger.isInfoEnabled()) {
      logger.info(
          "{} {} {} in {}ms", user, message, getJooqTable(), (System.currentTimeMillis() - start));
    }
  }

  private SqlDatabase getDatabase() {
    return (SqlDatabase) getSchema().getDatabase();
  }

  @Override
  public void drop() {
    long start = System.currentTimeMillis();
    getDatabase()
        .tx(
            db -> {
              dropTransaction(db, getSchemaName(), getTableName());
            });
    getDatabase().getListener().schemaChanged(getSchemaName());
    log(start, "dropped");
  }

  private static void dropTransaction(Database db, String schemaName, String tableName) {
    DSLContext jooq = ((SqlDatabase) db).getJooq();
    TableMetadata tm = db.getSchema(schemaName).getTable(tableName).getMetadata();
    executeDropTable(jooq, tm);
    MetadataUtils.deleteTable(jooq, tm);
  }
}
