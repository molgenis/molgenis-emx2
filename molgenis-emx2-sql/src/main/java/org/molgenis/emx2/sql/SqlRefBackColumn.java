package org.molgenis.emx2.sql;

import org.jooq.Field;
import org.jooq.exception.DataAccessException;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.TypeUtils;

import static org.jooq.impl.DSL.*;
import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.sql.MetadataUtils.saveColumnMetadata;

class SqlRefBackColumn extends SqlColumn {
  public SqlRefBackColumn(
      SqlTableMetadata table, String columnName, String toTable, String toColumn, String mappedBy) {
    super(table, columnName, REFBACK);
    this.setReference(toTable, toColumn);
    this.setMappedBy(mappedBy);
  }

  // will create a dummy array column matching the toColumn we will link to
  // will create a before insert trigger to update all REF instances in the other table that needs
  // updating
  public SqlColumn createColumn() {
    try {
      // get ref table
      String refTableName = getRefTableName();
      if (refTableName == null) {
        throw new MolgenisException(
            "Create column failed",
            "Create of column '" + getName() + "' failed because RefTableName was not set");
      }

      // get the target column
      String refColumnName = getRefColumnName();
      if (refColumnName == null) {
        refColumnName = getTable().getSchema().getTableMetadata(getRefTableName()).getPrimaryKey();
        if (refColumnName == null) {
          throw new MolgenisException(
              "Create column failed",
              "Create of column '"
                  + getName()
                  + "' failed because RefColumnName was not set nor the other table has primary key set");
        }
      }

      // get the other column
      TableMetadata toTable = getTable().getSchema().getTableMetadata(refTableName);
      Column toColumn = toTable.getColumn(refColumnName);

      // toColumn should be a key, not null
      if ((!toColumn.isPrimaryKey() && !toTable.isUnique(toColumn.getName()))
          || toColumn.isNullable()) {
        throw new MolgenisException(
            "Create column failed",
            "Create of column '"
                + getName()
                + "' failed because RefColumnName '"
                + toColumn.getName()
                + "'is not primary key and not unique or nullable");
      }

      // get the via column which is also in the 'toTable'
      String mappedByColumnName = this.getMappedBy();
      if (mappedByColumnName == null) {
        throw new MolgenisException(
            "Create column failed",
            "Create of REFBACK column '" + getName() + "' failed because mappedBy was not set.");
      }

      // check mappedBy column
      Column mappedByColumn = getMappedByColumn();
      if (mappedByColumn == null) {
        throw new MolgenisException(
            "Create column failed",
            "Create of REFBACK column '" + getName() + "' failed because mappedBy did not exist");
      }

      // get array type for that target column
      ColumnType arrayType = TypeUtils.getArrayType(toColumn.getColumnType());

      // execute alter current table add that column
      Field thisColumn = field(name(getName()), SqlTypeUtils.jooqTypeOf(arrayType));
      org.jooq.Table thisTable =
          table(name(getTable().getSchema().getName(), getTable().getTableName()));
      getJooq().alterTable(thisTable).addColumn(thisColumn).execute();

      // create the trigger so that insert/update/delete on REFBACK column updates the relationship
      ColumnType mappedByType = mappedByColumn.getColumnType();
      switch (mappedByType) {
        case REF:
          this.createTriggerForRef();
          break;
        case REF_ARRAY:
          this.createTriggerForRefArray();
          break;
        case MREF:
          this.createTriggerForMref();
          break;
        default:
          throw new MolgenisException(
              "Create column failed",
              "Create of REFBACK column '"
                  + getName()
                  + "' failed because mappedBy was not of type REF, REF_ARRAY or MREF");
      }

      saveColumnMetadata(this);
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException(
          "REFBACK '"
              + getName()
              + "' could not be created in table '"
              + getTable().getTableName()
              + "'",
          dae);
    }
    return this;
  }

  private void createTriggerForMref() {}

  private void createTriggerForRefArray() {
    String schemaName = getTable().getSchema().getName();
    String updateTriggerName = "1" + getTable().getTableName() + "_" + getName() + "_UPDATETRIGGER";

    // on insert and update trigger

    getJooq()
        .execute(
            "CREATE FUNCTION {0}() RETURNS trigger AS"
                + "\n$BODY$"
                + "\nDECLARE"
                + "\n\t item {1};"
                + "\nBEGIN"
                // remove all mappedBy references to 'me' that are not valid anymore
                + "\n\tIF TG_OP = 'UPDATE' THEN"
                + "\n\t\tUPDATE {2} set {3} = array_remove({3}, OLD.{4}) WHERE OLD.{4} = ANY {3} AND {6} != ANY NEW.{5}"
                + "\n\tEND IF;"
                // add all new mappedBy references to 'me' for all values in NEW.{this column}
                + "\n\tIF TG_OP = 'INSERT' OR TG_OP='UPDATE' FOREACH item IN ARRAY NEW.{5} LOOP"
                // SET THAT THE OTHER POINTS TO new 'ME'
                + "\n\t\tUPDATE {2} SET {3} = array_append({3},item) WHERE NEW.{4} != ANY {3} AND {6} = ANY NEW.{5}"
                + "\n\tEND LOOP;"
                + "\n\tRETURN NEW;"
                + "\nEND;"
                + "\n$BODY$ LANGUAGE plpgsql;",
            name(schemaName, updateTriggerName), // {0} function name
            keyword(SqlTypeUtils.getPsqlType(getRefColumn())), // {1} type of item
            table(name(schemaName, getRefTableName())), // {2} toTable table
            field(name(getMappedBy())), // {3} mappedBy
            field(
                name(
                    getMappedByColumn()
                        .getRefColumnName())), // {4} key that mappedBy uses (might not be pkey)
            field(name(getName())), // {5} the dummy column that triggers all this
            field(name(getRefColumnName()))); // {6} toColumn where fake foreign key dummy points to

    // attach the trigger

    getJooq()
        .execute(
            "CREATE TRIGGER {0} "
                + "\n\tBEFORE INSERT OR UPDATE OF {1} ON {2}"
                + "\n\tFOR EACH ROW EXECUTE PROCEDURE {3}()",
            name(getMappedBy() + "_UPDATE"),
            name(getName()),
            name(schemaName, getTable().getTableName()),
            name(schemaName, updateTriggerName));

    // delete and truncate trigger

    String deleteTriggerName = "1" + getTable().getTableName() + "_" + getName() + "_DELTRIGGER";

    getJooq()
        .execute(
            "CREATE FUNCTION {0}() RETURNS trigger AS"
                + "\n$BODY$"
                + "\nBEGIN"
                // remove all mappedBy references to 'me' that are not valid anymore
                + "\n\tUPDATE {1} set {2} = array_remove({2}, OLD.{3}) WHERE OLD.{3} = ANY {2}"
                + "\n\tEND IF;"
                + "\nEND;"
                + "\n$BODY$ LANGUAGE plpgsql;",
            name(schemaName, deleteTriggerName), // {0} function name
            table(name(schemaName, getRefTableName())), // {1} toTable table
            field(name(getMappedBy())), // {2} mappedBy
            field(
                name(
                    getMappedByColumn()
                        .getRefColumnName()))); // {3} key that mappedBy uses (might not be pkey)

    getJooq()
        .execute(
            "CREATE TRIGGER {0} "
                + "\n\tBEFORE DELETE OR TRUNCATE ON {1}"
                + "\n\tFOR EACH ROW EXECUTE PROCEDURE {2}()",
            name(getName() + "_DELETE"),
            name(schemaName, getTable().getTableName()),
            name(schemaName, deleteTriggerName));
  }

  private void createTriggerForRef() {
    String schemaName = getTable().getSchema().getName();
    // added number because triggers are fired in alphabethical order, thus ensuring that text
    // search indexer fire last
    String insertOrUpdateTrigger = "1" + getTable().getTableName() + "_" + getName() + "_UPDATE";

    // insert and update trigger
    getJooq()
        .execute(
            "CREATE FUNCTION {0}() RETURNS trigger AS"
                + "\n$BODY$"
                + "\nDECLARE"
                + "\n\t item {1};"
                + "\nBEGIN"
                // set to null all toTable rows that point to old 'me'. Might lead to null issues
                + "\n\tIF TG_OP = 'UPDATE' THEN"
                + "\n\t\tUPDATE {2} set {3} = NULL WHERE {3}=OLD.{4};"
                + "\n\tEND IF;"
                // set all toTable rows to point to new 'me' using mappedBy, and protect on conflict
                // update
                + "\n\tIF TG_OP='UPDATE' OR NOT EXISTS (SELECT 1 FROM {7} WHERE {8} = NEW.{8}) THEN FOREACH item IN ARRAY NEW.{5} LOOP"
                // SET THAT THE OTHER POINTS TO new 'ME'
                + "\n\t\tUPDATE {2} SET {3}=NEW.{4} WHERE {6}=item;"
                + "\n\tEND LOOP; END IF;"
                // set to null unless there is 'on conflict' which is in case of INSERT + EXISTS
                + "\n\tIF TG_OP='UPDATE' OR NOT EXISTS (SELECT 1 FROM {7} WHERE {8} = NEW.{8}) THEN NEW.{5} = NULL; END IF;"
                + "\n\tRETURN NEW;"
                + "\nEND;"
                + "\n$BODY$ LANGUAGE plpgsql;",
            name(schemaName, insertOrUpdateTrigger), // {0} function name
            keyword(SqlTypeUtils.getPsqlType(getRefColumn())), // {1} type of item
            table(name(schemaName, getRefTableName())), // {2} toTable table
            field(name(getMappedBy())), // {3} mappedBy field
            field(
                name(
                    getMappedByColumn()
                        .getRefColumnName())), // {4} mappedBy reference to 'me' (might not be pkey)
            field(name(getName())), // {5} the dummy column that triggers all this
            field(name(getRefColumnName())), // {6} toColumn where fake foreign key dummy points to
            table(name(schemaName, getTable().getTableName())), // {7} this table
            field(name(getTable().getPrimaryKey()))); // {8} primary key of this table

    getJooq()
        .execute(
            "CREATE TRIGGER {0} "
                + "\n\tBEFORE INSERT OR UPDATE OF {1} ON {2}"
                + "\n\tFOR EACH ROW EXECUTE PROCEDURE {3}()",
            name(getName() + "_UPDATE"),
            name(getName()),
            name(schemaName, getTable().getTableName()),
            name(schemaName, insertOrUpdateTrigger));

    getJooq()
        .execute(
            "CREATE TRIGGER {0} "
                + "\n\tAFTER DELETE ON {1} "
                + "\n\tFOR EACH ROW EXECUTE PROCEDURE {2}()",
            name(getName() + "_DELETE"),
            name(schemaName, getTable().getTableName()),
            name(schemaName, insertOrUpdateTrigger));
  }
}
