package org.molgenis.emx2.sql;

import org.jooq.Field;
import org.jooq.exception.DataAccessException;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.TypeUtils;

import static org.jooq.impl.DSL.*;
import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.ColumnType.REFBACK;
import static org.molgenis.emx2.sql.MetadataUtils.saveColumnMetadata;

class SqlRefBackColumn extends SqlColumn {
  public SqlRefBackColumn(
      SqlTableMetadata table,
      String columnName,
      String toTable,
      String toColumn,
      String viaColumn) {
    super(table, columnName, REFBACK);
    this.setReference(toTable, toColumn);
    this.setJoinVia(viaColumn);
  }

  // will create a dummy array column matching the toColumn we will link to
  // will create a before inset trigger to update all REF instances in the other table that needs
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

      // toColumn should be a key
      if (!toColumn.isPrimaryKey() && !toTable.isUnique(toColumn.getName())) {
        throw new MolgenisException(
            "Create column failed",
            "Create of column '"
                + getName()
                + "' failed because RefColumnName '"
                + toColumn.getName()
                + "'is not primary key and not unique");
      }

      // get the via column which is also in the 'toTable'
      String viaColumnName = this.getMrefJoinTableName();
      if (viaColumnName == null) {
        throw new MolgenisException(
            "Create column failed",
            "Create of REFBACK column '"
                + getName()
                + "' failed because viaColumnName was not set.");
      }
      Column viaColumn = toTable.getColumn(viaColumnName);
      if (viaColumn == null || !REF.equals(viaColumn.getColumnType())) {
        throw new MolgenisException(
            "Create column failed",
            "Create of REFBACK column '"
                + getName()
                + "' failed because viaColumnName did not exist or was not of type REF");
      }

      // get array type for that target column
      ColumnType arrayType = TypeUtils.getArrayType(toColumn.getColumnType());

      // execute alter table add the column
      Field thisColumn = field(name(getName()), SqlTypeUtils.jooqTypeOf(arrayType));
      org.jooq.Table thisTable =
          table(name(getTable().getSchema().getName(), getTable().getTableName()));
      getJooq().alterTable(thisTable).addColumn(thisColumn).execute();

      // create the trigger (gruwel)
      this.createBeforeInsertUpdateTrigger(toTable, toColumn, viaColumn);

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

  private void createBeforeInsertUpdateTrigger(
      TableMetadata toTable, Column toColumn, Column viaColumn) {
    String schemaName = getTable().getSchema().getName();
    // added number because triggers are fired in alphabethical order, thus ensuring that text
    // search indexer fire last
    String insertOrUpdateTrigger = "1" + getTable().getTableName() + "_" + getName() + "_TRIGGER";

    // insert and update trigger on 'this': does the following
    // find all current references to 'me'
    // then compares those to the list of values in 'me' insert/update list
    // removes all removed (if nullable constraints allow), and adds the one missing
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
                // set all toTable rows to point to new 'me' using viaColumn
                + "\n\tFOREACH item IN ARRAY NEW.{5}"
                + "\n\tLOOP"
                // SET THAT THE OTHER POINTS TO new 'ME'
                + "\n\t\tUPDATE {2} SET {3}=NEW.{4} WHERE {6}=item;"
                + "\n\tEND LOOP;"
                + "\n\tRETURN NEW;"
                + "\nEND;"
                + "\n$BODY$ LANGUAGE plpgsql;",
            name(schemaName, insertOrUpdateTrigger), // {0} function name
            keyword(SqlTypeUtils.getPsqlType(toColumn)), // {1} type of item
            table(name(schemaName, toTable.getTableName())), // {2} toTable table
            field(name(viaColumn.getName())), // {3} viaColumn field
            field(name(viaColumn.getRefColumnName())), // {4} viaColumn reference to 'me'
            field(name(getName())), // {5} the dummy column that triggers all this
            field(name(toColumn.getName()))); // {6} toColumn where fake foreign key dummy points to

    getJooq()
        .execute(
            "CREATE TRIGGER {0} "
                + "\n\tBEFORE INSERT OR UPDATE OF {1} ON {2}"
                + "\n\tFOR EACH ROW EXECUTE PROCEDURE {3}()",
            name(insertOrUpdateTrigger),
            name(getName()),
            name(schemaName, getTable().getTableName()),
            name(schemaName, insertOrUpdateTrigger));

    // TODO create a insert/update and a delete trigger on the 'ref' table so that it keeps my
    // ref_array up to date;

    // insert and update trigger on 'toTable' to keep REFBACK updated
    insertOrUpdateTrigger = "1" + toTable.getTableName() + "_" + viaColumn.getName() + "_TRIGGER";

    getJooq()
        .execute(
            "CREATE FUNCTION {0}() RETURNS trigger AS"
                + "\n$BODY$"
                + "\nBEGIN"
                + "\n\tIF (TG_OP = 'UPDATE' OR TG_OP = 'DELETE') AND EXISTS (SELECT 1 FROM {1} WHERE OLD.{3} = ANY ({2})) THEN"
                // remove 'old' from the REFBACK array
                + "\n\t\tUPDATE {1} set {2} = array_remove({2}, OLD.{3}) WHERE {4}=OLD.{5};"
                + "\n\tEND IF;"
                // add 'new' to the REFBACK array
                // SET THAT THE OTHER POINTS TO new 'ME'
                + "\n\tIF TG_OP != 'DELETE' AND NOT EXISTS (SELECT 1 FROM {1} WHERE NEW.{3} = ANY ({2})) THEN "
                + "\n\t\tUPDATE {1} SET {2} = array_append({2}, NEW.{3}) WHERE {4}=NEW.{5}; "
                + "\n\tEND IF;"
                + "\n\tRETURN NEW;"
                + "\nEND;"
                + "\n$BODY$ LANGUAGE plpgsql;",
            name(schemaName, insertOrUpdateTrigger), // {0} function name
            table(name(schemaName, getTable().getTableName())), // {1} this table name
            field(name(getName())), // {2} this refback column name
            field(name(toColumn.getName())), // {3} toColumn where refback points to
            field(name(viaColumn.getRefColumnName())), // {4} viaColumn reference to 'me'
            field(name(viaColumn.getName()))); // {5} viaColumn itself

    getJooq()
        .execute(
            "CREATE TRIGGER {0} "
                + "\n\tBEFORE INSERT OR UPDATE OF {1} ON {2} "
                + "\n\tFOR EACH ROW EXECUTE PROCEDURE {3}()",
            name(toColumn.getName() + "_UPDATE"),
            name(toColumn.getName()),
            name(schemaName, toTable.getTableName()),
            name(schemaName, insertOrUpdateTrigger));

    getJooq()
        .execute(
            "CREATE TRIGGER {0} "
                + "\n\tAFTER DELETE ON {2} "
                + "\n\tFOR EACH ROW EXECUTE PROCEDURE {3}()",
            name(toColumn.getName() + "_DELETE"),
            name(toColumn.getName()),
            name(schemaName, toTable.getTableName()),
            name(schemaName, insertOrUpdateTrigger));
  }

  // insert and update trigger on 'toTable' to keep REFBACK updated
  //    insertOrUpdateTrigger =
  //            "1"
  //            + viaColumn.getTable().getTableName()
  //            + "_"
  //                    + viaColumn.getName()
  //                    + "_REFBACK_UPDATETRIGGER";
}
