package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.Constants.MG_TABLECLASS;
import static org.molgenis.emx2.sql.Constants.MG_EDIT_ROLE;
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
    long start = System.currentTimeMillis();
    getDatabase()
        .tx(
            dsl -> {
              // first per-column actions, then multi-column action such as composite keys/refs
              for (Column c : column) {
                if (getLocalColumn(c.getName()) != null) {
                  alterColumn(c);
                } else {
                  Column newColumn = new Column(this, c);
                  if (getInherit() != null
                      && getInheritedTable().getColumn(c.getName()) != null
                      // this column is replicated in all subclass tables
                      && !c.getName().equals(MG_TABLECLASS)) {
                    throw new MolgenisException(
                        "Cannot add column "
                            + getTableName()
                            + "."
                            + c.getName()
                            + ": column exists in inherited class "
                            + getInherit());
                  }
                  if (!CONSTANT.equals(newColumn.getColumnType())) {
                    validateColumn(newColumn);
                    updatePositions(newColumn, this);
                    executeCreateColumn(getJooq(), newColumn);
                    super.add(newColumn);
                    if (newColumn.getKey() > 0) {
                      createOrReplaceKey(
                          getJooq(),
                          newColumn.getTable(),
                          newColumn.getKey(),
                          newColumn.getTable().getKeyFields(newColumn.getKey()));
                    }
                    executeCreateRefConstraints(getJooq(), newColumn);
                  } else {
                    super.add(newColumn);
                  }
                  log(
                      start,
                      "added column '" + newColumn.getName() + "' to table " + getTableName());
                }
              }
            });
    getDatabase().getListener().schemaChanged(getSchemaName());
    return this;
  }

  @Override
  public TableMetadata alterName(String newName) {
    long start = System.currentTimeMillis();
    String oldName = getTableName();
    if (!getTableName().equals(newName)) {
      getDatabase()
          .tx(
              dsl -> {
                DSLContext jooq = ((SqlDatabase) dsl).getJooq();

                // drop triggers for this table
                for (Column column : getStoredColumns()) {
                  SqlColumnExecutor.executeRemoveRefConstraints(jooq, column);
                }

                // rename table and triggers
                SqlTableMetadataExecutor.executeAlterName(jooq, this, newName);

                // update metadata
                MetadataUtils.alterTableName(jooq, this, newName);
                super.alterName(newName);

                // recreate triggers for this table
                for (Column column : getStoredColumns()) {
                  SqlColumnExecutor.executeCreateRefConstraints(jooq, column);
                }

                // reroute inherits in meta of other tables
              });
      getDatabase().getListener().schemaChanged(getSchemaName());
      log(start, "altered table from '" + oldName + "' to  " + getTableName());
    }
    return this;
  }

  @Override
  public TableMetadata alterColumn(String name, Column column) {
    // ignore mg_ columns
    if (column.getName().startsWith("mg_")) return this;

    Column oldColumn = getColumn(name);
    if (oldColumn == null) {
      throw new MolgenisException(
          "Alter column failed: Column  '"
              + getTableName()
              + "."
              + column.getName()
              + "' does not exist");
    }
    if (getInherit() != null && getInheritedTable().getColumn(name) != null) {
      throw new MolgenisException(
          "Alter column "
              + getTableName()
              + "."
              + name
              + " failed: column is part of inherited table "
              + getInherit());
    }
    if (getInherit() != null && getInheritedTable().getColumn(column.getName()) != null) {
      throw new MolgenisException(
          "Rename column from "
              + getTableName()
              + "."
              + name
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
              DSLContext jooq = ((SqlDatabase) db).getJooq();
              Column newColumn = new Column(this, column);
              validateColumn(newColumn);

              // check if reference and of different size
              if (REF_ARRAY.equals(newColumn.getColumnType())
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
                checkNotRefback(name, oldColumn);
              }

              // change positions if needed
              if (!oldColumn.getPosition().equals(newColumn.getPosition())) {
                updatePositions(newColumn, this);
              }

              // drop old key, if touched
              if (oldColumn.getKey() > 0 && newColumn.getKey() != oldColumn.getKey()) {
                executeDropKey(jooq, oldColumn.getTable(), oldColumn.getKey());
              }

              // drop referential constraints around this column
              executeRemoveRefConstraints(jooq, oldColumn);

              // remove refBacks if exist
              executeRemoveRefback(oldColumn, newColumn);

              // rename and retype if needed
              executeAlterType(jooq, oldColumn, newColumn);
              executeAlterName(jooq, oldColumn, newColumn);

              // change required?
              // only applies to key=1
              if ((oldColumn.isPrimaryKey() || newColumn.isPrimaryKey())
                  && oldColumn.isRequired()
                  && !oldColumn.isRequired() == newColumn.isRequired()) {
                executeSetRequired(jooq, newColumn);
              }

              // update the metadata so we can use it for new keys and references
              super.alterColumn(name, newColumn);

              // reapply ref constrainst
              executeCreateRefConstraints(jooq, newColumn);

              // check if refBack constraints need updating
              reapplyRefbackContraints(oldColumn, newColumn);

              // create/update key, if touched
              if (newColumn.getKey() != oldColumn.getKey()) {
                createOrReplaceKey(
                    jooq, this, newColumn.getKey(), getKeyFields(newColumn.getKey()));
              }

              // delete old column if name changed, then save any other metadata changes
              if (!oldColumn.getName().equals(newColumn.getName())) deleteColumn(jooq, oldColumn);
              saveColumnMetadata(jooq, newColumn);
            });
    getDatabase().getListener().schemaChanged(getSchemaName());

    return this;
  }

  private void checkNotRefback(String name, Column oldColumn) {
    if (oldColumn.isReference()) {
      for (Column c : oldColumn.getRefTable().getColumns()) {
        if (REFBACK.equals(c.getColumnType())
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
            dsl -> {
              SqlColumnExecutor.executeRemoveColumn(getJooq(), getColumn(name));
              super.columns.remove(name);
            });
    getDatabase().getListener().schemaChanged(getSchemaName());
    log(start, "removed column '" + name + "' from ");
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
              executeSetInherit(getJooq(), this, other);
              super.setInherit(otherTable);
              MetadataUtils.saveTableMetadata(getJooq(), this);
            });
    getDatabase().getListener().schemaChanged(getSchemaName());
    log(start, "set inherit on ");
    return this;
  }

  @Override
  public TableMetadata removeInherit() {
    throw new MolgenisException("removeInherit not yet implemented");
  }

  @Override
  public SqlTableMetadata setSettings(List<Setting> settings) {
    super.setSettings(settings);
    for (Setting setting : settings) {
      MetadataUtils.saveSetting(getJooq(), this.getSchema(), this, setting);
    }
    getDatabase().getListener().schemaChanged(getSchemaName());
    return this;
  }

  @Override
  public void removeSetting(String key) {
    MetadataUtils.deleteSetting(getJooq(), getSchema(), this, new Setting(key, null));
    super.removeSetting(key);
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
  public TableMetadata drop() {
    long start = System.currentTimeMillis();
    getDatabase()
        .tx(
            db -> {
              DSLContext jooq = ((SqlDatabase) db).getJooq();
              executeDropTable(jooq, this);
              MetadataUtils.deleteTable(jooq, this);
            });
    getDatabase().getListener().schemaChanged(getSchemaName());
    log(start, "dropped");
    return this;
  }
}
