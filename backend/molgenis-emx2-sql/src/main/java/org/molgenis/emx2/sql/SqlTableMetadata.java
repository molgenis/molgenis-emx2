package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.Constants.MG_TABLECLASS;
import static org.molgenis.emx2.sql.MetadataUtils.deleteColumn;
import static org.molgenis.emx2.sql.MetadataUtils.saveColumnMetadata;
import static org.molgenis.emx2.sql.SqlColumnExecutor.*;
import static org.molgenis.emx2.sql.SqlSchemaMetadata.validateTableIdentifierIsUnique;
import static org.molgenis.emx2.sql.SqlTableMetadataExecutor.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    getDatabase().tx(db -> sync(addTransaction(db, getSchemaName(), getTableName(), column)));
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
      validateColumnIdentifierIsUnique(tm, c);
      long start = System.currentTimeMillis();
      if (tm.getLocalColumn(c.getName()) != null) {
        tm.alterColumn(c);
      } else {
        Column newColumn = new Column(tm, c);
        Optional<TableMetadata> conflictingParent = findConflictingParentByName(tm, c.getName());
        if (conflictingParent.isPresent() && !c.getName().equals(MG_TABLECLASS)) {
          throw new MolgenisException(
              "Cannot add column "
                  + tm.getTableName()
                  + "."
                  + c.getName()
                  + ": column exists in inherited class "
                  + conflictingParent.get().getTableName());
        }
        checkNoColumnWithSameNameExistsInSubclass(c.getName(), tm, tm.getJooq());

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

  private static Optional<TableMetadata> findConflictingParentByName(
      TableMetadata tm, String columnName) {
    for (TableMetadata parent : tm.getInheritedTables()) {
      if (parent.getColumn(columnName) != null) {
        return Optional.of(parent);
      }
    }
    return Optional.empty();
  }

  private static void validateColumnIdentifierIsUnique(
      SqlTableMetadata existingTableMetadata, Column column) {
    for (Column existingColumn : existingTableMetadata.getColumns()) {
      if (!column.getName().equals(MG_TABLECLASS)
          && !column.getName().equals(existingColumn.getName())
          && existingColumn.getIdentifier().equals(column.getIdentifier())) {
        throw new MolgenisException(
            String.format(
                "Cannot create/alter column because name resolves to same identifier: '%s' has same identifier as '%s' (both resolve to identifier '%s')",
                column.getName(), existingColumn.getName(), column.getIdentifier()));
      }
    }
  }

  @Override
  public TableMetadata alterName(String newName) {
    long start = System.currentTimeMillis();
    String oldName = getTableName();
    if (!getTableName().equals(newName)) {
      getDatabase()
          .tx(db -> sync(alterNameTransaction(db, getSchemaName(), getTableName(), newName)));
      ((SqlSchemaMetadata) getSchema()).reload();
      getDatabase().getListener().schemaChanged(getSchemaName());
      log(start, "altered table from '" + oldName + "' to  " + getTableName());
    }
    return this;
  }

  // ensure the transaction has no side effects on 'this' until completed
  private static SqlTableMetadata alterNameTransaction(
      Database db, String schemaName, String tableName, String newName) {
    SqlSchemaMetadata sm = (SqlSchemaMetadata) db.getSchema(schemaName).getMetadata();
    SqlTableMetadata tm = sm.getTableMetadata(tableName);

    validateTableIdentifierIsUnique(sm, new TableMetadata(newName));

    // drop triggers for this table
    for (Column column : tm.getStoredColumns()) {
      SqlColumnExecutor.executeRemoveRefConstraints(tm.getJooq(), column);
    }

    // get references pointing to 'me'
    List<Column> refColumns =
        MetadataUtils.getReferencesToTable(tm.getJooq(), schemaName, tableName);

    // get refbacks pointing to 'me'
    List<Column> refbackColumns =
        tm.getColumns().stream()
            .filter(c -> c.getReferenceRefback() != null)
            .map(c -> c.getReferenceRefback())
            .toList();

    // rename table and triggers
    SqlTableMetadataExecutor.executeAlterName(tm.getJooq(), tm, newName);

    // update metadata
    MetadataUtils.alterTableName(tm.getJooq(), tm, newName);
    tm.tableName = newName;

    // rename refs.refTable
    refColumns.forEach(
        ref -> {
          ref.setRefTable(newName);
          MetadataUtils.saveColumnMetadata(tm.getJooq(), ref);
          db.getListener().schemaChanged(ref.getSchemaName());
        });

    // rename refbacks.refTable
    refbackColumns.forEach(
        refBack -> {
          refBack.setRefTable(newName);
          MetadataUtils.saveColumnMetadata(tm.getJooq(), refBack);
          db.getListener().schemaChanged(refBack.getSchemaName());
        });

    // recreate triggers for this table
    for (Column column : tm.getStoredColumns()) {
      SqlColumnExecutor.executeCreateRefConstraints(tm.getJooq(), column);
    }

    return tm;
  }

  @Override
  public TableMetadata alterColumn(String columnName, Column column) {
    // ignore mg_ columns
    if (column.isSystemColumn()) return this;

    Column oldColumn = getColumn(columnName);

    validateColumnIdentifierIsUnique(this, column);

    if (oldColumn == null) {
      throw new MolgenisException(
          "Alter column failed: Column  '"
              + getTableName()
              + "."
              + column.getName()
              + "' does not exist");
    }
    Optional<TableMetadata> ownerOfExistingColumn = findConflictingParentByName(this, columnName);
    if (ownerOfExistingColumn.isPresent()) {
      throw new MolgenisException(
          "Alter column "
              + getTableName()
              + "."
              + columnName
              + " failed: column is part of inherited table "
              + ownerOfExistingColumn.get().getTableName());
    }
    Optional<TableMetadata> ownerOfNewColumnName =
        findConflictingParentByName(this, column.getName());
    if (ownerOfNewColumnName.isPresent() && !column.getName().equals(columnName)) {
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
              + ownerOfNewColumnName.get().getTableName());
    }
    getDatabase()
        .tx(
            db ->
                sync(
                    alterColumnTransaction(
                        getSchemaName(), getTableName(), columnName, column, db)));
    // reload the state
    ((SqlSchemaMetadata) getSchema()).sync(getDatabase().getSchema(getSchemaName()).getMetadata());
    return this;
  }

  // static to ensure we don't touch 'this' until transaction succesfull
  private static SqlTableMetadata alterColumnTransaction(
      String schemaName, String tableName, String columnName, Column column, Database db) {
    SqlTableMetadata tm =
        (SqlTableMetadata) db.getSchema(schemaName).getMetadata().getTableMetadata(tableName);
    Column newColumn = new Column(tm, column);
    Column oldColumn = tm.getColumn(columnName);

    // primary keys by definition are required
    if (newColumn.getKey() == 1) {
      newColumn.setRequired(true);
    }

    validateColumn(newColumn);
    if (!columnName.equals(column.getName())) {
      checkNoColumnWithSameNameExistsInSubclass(column.getName(), tm, tm.getJooq());
    }

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

    // drop old key, if touched
    if (oldColumn.getKey() > 0 && newColumn.getKey() != oldColumn.getKey()) {
      executeDropKey(tm.getJooq(), oldColumn.getTable(), oldColumn.getKey());
    }

    // drop referential constraints around this column
    executeRemoveRefConstraints(tm.getJooq(), oldColumn);

    // add ontology table if needed
    if (newColumn.isOntology()) {
      createOntologyTable(newColumn);
    }

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
      newColumn.setPosition(tm.columns.get(columnName).getPosition());
    }
    // remove the old
    tm.columns.remove(columnName);
    // add the new
    tm.columns.put(column.getName(), newColumn);

    // if changing 'ref' then check if not refBack exists
    Column referenceRefBack = oldColumn.getReferenceRefback();
    if (referenceRefBack != null) {
      // delete if changed to non ref
      if (!newColumn.isReference()) {
        referenceRefBack.getTable().dropColumn(referenceRefBack.getName());
      }
      // else update refback if renamed
      else if (!oldColumn.getName().equals(newColumn.getName())) {
        referenceRefBack
            .getTable()
            .alterColumn(
                referenceRefBack.getName(), referenceRefBack.setRefBack(newColumn.getName()));
      }
    }

    // reapply ref constrainst
    executeCreateRefConstraints(tm.getJooq(), newColumn);

    // create/update key, if touched
    if (newColumn.getKey() != oldColumn.getKey()) {
      createOrReplaceKey(tm.getJooq(), tm, newColumn.getKey(), tm.getKeyFields(newColumn.getKey()));
    }

    // delete old column if name changed, then save any other metadata changes
    if (!oldColumn.getName().equals(newColumn.getName())) deleteColumn(tm.getJooq(), oldColumn);
    saveColumnMetadata(tm.getJooq(), newColumn);

    return tm;
  }

  @Override
  public void dropColumn(String name) {
    Column column = getColumn(name);
    if (column == null) {
      throw new MolgenisException("Drop column " + name + " failed: column does not exist");
    }

    long start = System.currentTimeMillis();
    if (getColumn(name) == null) return; // return silently, idempotent
    getDatabase().tx(db -> sync(dropColumnTransaction(db, getSchemaName(), getTableName(), name)));
    log(start, "removed column '" + name + "' from ");
  }

  private static SqlTableMetadata dropColumnTransaction(
      Database db, String schemaName, String tableName, String columnName) {
    SqlTableMetadata tm =
        (SqlTableMetadata) db.getSchema(schemaName).getTable(tableName).getMetadata();
    DSLContext jooq = ((SqlDatabase) db).getJooq();
    SqlColumnExecutor.executeRemoveColumn(jooq, tm.getColumn(columnName));
    tm.columns.remove(columnName);
    SqlTableMetadataExecutor.updateSearchIndexTriggerFunction(jooq, tm, tableName);
    return tm;
  }

  @Override
  public TableMetadata setInheritNames(List<String> names) {
    long start = System.currentTimeMillis();
    if (names == null || names.isEmpty()) {
      return this;
    }
    List<String> currentParents = getInheritNames();
    List<String> newParents =
        names.stream().filter(name -> !currentParents.contains(name)).toList();
    if (newParents.isEmpty()) {
      return this;
    }
    List<String> allRequestedParents = new ArrayList<>(currentParents);
    allRequestedParents.addAll(newParents);

    String inheritSchema = getImportSchema() != null ? getImportSchema() : getSchemaName();
    for (String parentName : newParents) {
      TableMetadata parent;
      if (getImportSchema() != null) {
        Schema otherSchema = getSchema().getDatabase().getSchema(getImportSchema());
        if (otherSchema == null || otherSchema.getMetadata().getTableMetadata(parentName) == null) {
          throw new MolgenisException(
              "Inheritance failed. Other schema.table '"
                  + getImportSchema()
                  + "."
                  + parentName
                  + "' does not exist in this database");
        }
        parent = otherSchema.getMetadata().getTableMetadata(parentName);
      } else {
        parent = getSchema().getTableMetadata(parentName);
        if (parent == null)
          throw new MolgenisException(
              "Inheritance failed. Other table '" + parentName + "' does not exist in this schema");
      }
      if (parent.getPrimaryKeys().isEmpty()) {
        throw new MolgenisException(
            "Set inheritance failed: To extend table '"
                + parentName
                + "' it must have primary key set");
      }
    }
    TableMetadata candidateChild = new org.molgenis.emx2.TableMetadata(getSchema(), this);
    candidateChild.setInheritNames(allRequestedParents);
    candidateChild.validateInheritance();

    getDatabase()
        .tx(
            tdb ->
                sync(
                    bulkAddInheritTransaction(
                        tdb,
                        getSchemaName(),
                        getTableName(),
                        inheritSchema,
                        currentParents,
                        newParents)));
    log(start, "set inherit on ");
    super.setInheritNames(allRequestedParents);
    return this;
  }

  private static SqlTableMetadata bulkAddInheritTransaction(
      Database db,
      String schemaName,
      String tableName,
      String inheritSchema,
      List<String> alreadyWiredParents,
      List<String> parentsToAdd) {
    DSLContext jooq = ((SqlDatabase) db).getJooq();
    SqlTableMetadata tm =
        (SqlTableMetadata) db.getSchema(schemaName).getTable(tableName).getMetadata();

    for (String parentName : parentsToAdd) {
      TableMetadata om = db.getSchema(inheritSchema).getTable(parentName).getMetadata();
      executeSetInherit(jooq, tm, om);
      // Update in-memory list directly — avoids re-entering SqlTableMetadata.setInheritNames
      if (!tm.inheritNames.contains(parentName)) {
        tm.inheritNames.add(parentName);
      }
    }
    MetadataUtils.saveTableMetadata(jooq, tm);
    return tm;
  }

  @Override
  public TableMetadata removeInherit() {
    throw new MolgenisException("remove tableExtends not yet implemented");
  }

  @Override
  public TableMetadata setSettings(Map<String, String> settings) {
    if (PermissionEvaluator.canUpdate(getDatabase().getSchema(getSchemaName()), this)) {
      getDatabase()
          .tx(
              db ->
                  sync(
                      setSettingTransaction(
                          (SqlDatabase) db, getSchemaName(), getTableName(), settings)));
      getDatabase().getListener().schemaChanged(getSchemaName());
      return this;
    } else {
      throw new MolgenisException(
          "Permission denied for user "
              + getDatabase().getActiveUser()
              + " to change setting on table "
              + getTableName()
              + ". You need at least EDITOR permission for table settings.");
    }
  }

  private static SqlTableMetadata setSettingTransaction(
      SqlDatabase db, String schemaName, String tableName, Map<String, String> settings) {
    SqlSchemaMetadata schema = db.getSchema(schemaName).getMetadata();
    SqlTableMetadata tm = schema.getTableMetadata(tableName);
    tm.setSettingsWithoutReload(settings);
    MetadataUtils.saveTableMetadata(db.getJooq(), tm);
    db.getListener().schemaChanged(schemaName);
    return tm;
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
    getDatabase().tx(db -> dropTransaction(db, getSchemaName(), getTableName()));
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
