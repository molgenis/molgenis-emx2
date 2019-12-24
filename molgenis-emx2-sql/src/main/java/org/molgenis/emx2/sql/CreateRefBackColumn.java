package org.molgenis.emx2.sql;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.exception.DataAccessException;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.TypeUtils;

import static org.jooq.impl.DSL.*;
import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.sql.CreateSimpleColumn.getMappedByColumn;

class CreateRefBackColumn {

  // will create a dummy array column matching the toColumn we will link to
  // will create a before insert trigger to update all REF instances in the other table that needs
  // updating
  static void createRefBackColumn(DSLContext jooq, Column column) {
    try {
      // get ref table
      String refTableName = column.getRefTableName();
      if (refTableName == null) {
        throw new MolgenisException(
            "Create column failed",
            "Create of column '" + column.getName() + "' failed because RefTableName was not set");
      }

      // get the target column
      String refColumnName = column.getRefColumnName();
      if (refColumnName == null) {
        refColumnName =
            column
                .getTable()
                .getSchema()
                .getTableMetadata(column.getRefTableName())
                .getPrimaryKey();
        if (refColumnName == null) {
          throw new MolgenisException(
              "Create column failed",
              "Create of column '"
                  + column.getName()
                  + "' failed because RefColumnName was not set nor the other table has primary key set");
        }
      }

      // get the other column
      TableMetadata toTable = column.getRefTable();
      Column toColumn = column.getRefColumn();

      // toColumn should be a key, not null
      if ((!toColumn.isPrimaryKey() && !toTable.isUnique(toColumn.getName()))
          || toColumn.isNullable()) {
        throw new MolgenisException(
            "Create column failed",
            "Create of column '"
                + column.getName()
                + "' failed because RefColumnName '"
                + toColumn.getName()
                + "'is not primary key and not unique and not nullable");
      }

      // get the via column which is also in the 'toTable'
      String mappedByColumnName = column.getMappedBy();
      if (mappedByColumnName == null) {
        throw new MolgenisException(
            "Create column failed",
            "Create of REFBACK column '"
                + column.getName()
                + "' failed because mappedBy was not set.");
      }

      Column mappedByColumn = getMappedByColumn(column);

      if (!mappedByColumn.isNullable()) {
        throw new MolgenisException(
            "Create column failed",
            "Create of column refack '"
                + column.getName()
                + "' failed because mappedBy column '"
                + mappedByColumn.getTableName()
                + "."
                + mappedByColumn.getName()
                + "' is not nullable. Bi directional relations both ends must be nullable.");
      }

      // check mappedBy column
      if (mappedByColumn == null) {
        throw new MolgenisException(
            "Create column failed",
            "Create of REFBACK column '"
                + column.getName()
                + "' failed because mappedBy did not exist");
      }

      // get array type for that target column
      ColumnType arrayType = TypeUtils.getArrayType(toColumn.getColumnType());

      // execute alter current table add that column
      Field thisColumn = field(name(column.getName()), SqlTypeUtils.jooqTypeOf(arrayType));
      org.jooq.Table thisTable =
          table(name(column.getTable().getSchema().getName(), column.getTable().getTableName()));
      jooq.alterTable(thisTable).addColumn(thisColumn).execute();

      // create the trigger so that insert/update/delete on REFBACK column updates the relationship
      ColumnType mappedByType = mappedByColumn.getColumnType();
      switch (mappedByType) {
        case REF:
          createTriggerForRef(jooq, column);
          break;
        case REF_ARRAY:
          createTriggerForRefArray(jooq, column);
          break;
        case MREF:
        default:
          throw new MolgenisException(
              "Create column failed",
              "Create of REFBACK column '"
                  + column.getName()
                  + "' failed because mappedBy was not of type REF, REF_ARRAY or MREF");
      }
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException(
          "REFBACK '"
              + column.getName()
              + "' could not be created in table '"
              + column.getTable().getTableName()
              + "'",
          dae);
    }
  }

  private static void createTriggerForRefArray(DSLContext jooq, Column column) {
    String schemaName = column.getTable().getSchema().getName();
    String updateTriggerName =
        "1" + column.getTable().getTableName() + "_" + column.getName() + "_UPDATETRIGGER";

    // on insert and update trigger

    jooq.execute(
        "CREATE FUNCTION {0}() RETURNS trigger AS"
            + "\n$BODY$"
            + "\nDECLARE"
            + "\n\t item {1};"
            + "\nBEGIN"
            // check no dangling foreign keys
            + "\n\tIF EXISTS (SELECT * from unnest(NEW.{5}) as {5} WHERE {5} NOT IN (SELECT {6} FROM {2})) THEN RAISE EXCEPTION USING ERRCODE='23503', "
            + "\n\t\tMESSAGE = 'update or delete on table '||{9}||' violates foreign key constraint'"
            + "\n\t\t, DETAIL = 'Key ('||{10}||')=('|| array_to_string(NEW.{5},',') ||') is not present in table '||{11}||', column '||{12};"
            // remove all mappedBy references to 'me' that are not valid anymore
            + "\n\tEND IF;"
            + "\n\tIF TG_OP = 'UPDATE' THEN"
            + "\n\t\tUPDATE {2} set {3} = array_remove({3}, OLD.{4}) WHERE OLD.{4} = ANY ({3}) AND {6} != ANY (NEW.{5});"
            + "\n\tEND IF;"
            // add all new mappedBy references to 'me' for all values in NEW.{this column},
            // check for on conflict update via not exists
            + "\n\tIF TG_OP='UPDATE' OR NOT EXISTS (SELECT 1 FROM {7} WHERE {8} = NEW.{8}) THEN"
            // SET THAT THE OTHER POINTS TO new 'ME'
            + "\n\t\tUPDATE {2} SET {3} = array_append({3},NEW.{4}) WHERE ({3} = '{}' OR NEW.{4} != ANY ({3})) AND {6} = ANY (NEW.{5});"
            + "\n\tEND IF;"
            + "\n\tIF TG_OP='UPDATE' OR NOT EXISTS (SELECT 1 FROM {7} WHERE {8} = NEW.{8}) THEN NEW.{5} = NULL; END IF;"
            + "\n\tRETURN NEW;"
            + "\nEND;"
            + "\n$BODY$ LANGUAGE plpgsql;",
        name(schemaName, updateTriggerName), // {0} function name
        keyword(SqlTypeUtils.getPsqlType(column.getRefColumn())), // {1} type of item
        table(name(schemaName, column.getRefTableName())), // {2} toTable table
        field(name(column.getMappedBy())), // {3} mappedBy
        field(
            name(
                getMappedByColumn(column)
                    .getRefColumnName())), // {4} key that mappedBy uses (might not be pkey)
        field(name(column.getName())), // {5} the dummy column that triggers all this
        field(
            name(column.getRefColumnName())), // {6} toColumn where fake foreign key dummy points to
        table(name(schemaName, column.getTable().getTableName())), // {7} this table
        field(name(column.getTable().getPrimaryKey())), // {8} primary key of this table
        inline(column.getTable().getTableName()), // {9} inline table name
        inline(column.getName()), // {10}
        inline(column.getRefTableName()), // {11} inline table name
        inline(column.getRefColumnName())); // {12} inline table name

    // attach the trigger

    jooq.execute(
        "CREATE TRIGGER {0} "
            + "\n\tBEFORE INSERT OR UPDATE OF {1} ON {2}"
            + "\n\tFOR EACH ROW EXECUTE PROCEDURE {3}()",
        name(column.getName() + "_UPDATE"),
        name(column.getName()),
        name(schemaName, column.getTable().getTableName()),
        name(schemaName, updateTriggerName));

    // delete and truncate trigger

    String deleteTriggerName =
        "1" + column.getTable().getTableName() + "_" + column.getName() + "_DELTRIGGER";

    jooq.execute(
        "CREATE FUNCTION {0}() RETURNS trigger AS"
            + "\n$BODY$"
            + "\nBEGIN"
            // remove all mappedBy references to 'me' that are not valid anymore
            + "\n\tUPDATE {1} set {2} = array_remove({2}, OLD.{3}) WHERE OLD.{3} = ANY ({2});"
            + "\n\tRETURN OLD;"
            + "\nEND;"
            + "\n$BODY$ LANGUAGE plpgsql;",
        name(schemaName, deleteTriggerName), // {0} function name
        table(name(schemaName, column.getRefTableName())), // {1} this table
        field(name(column.getMappedBy())), // {2} mappedBy
        field(
            name(
                getMappedByColumn(column)
                    .getRefColumnName()))); // {3} key that mappedBy uses (might not be pkey)

    // attach trigger
    jooq.execute(
        "CREATE TRIGGER {0} "
            + "\n\tAFTER DELETE ON {1}"
            + "\n\tFOR EACH ROW EXECUTE PROCEDURE {2}()",
        name(column.getName() + "_DELETE"),
        name(schemaName, column.getTable().getTableName()),
        name(schemaName, deleteTriggerName));
  }

  private static void createTriggerForRef(DSLContext jooq, Column column) {
    String schemaName = column.getTable().getSchema().getName();
    // added number because triggers are fired in alphabethical order, thus ensuring that text
    // search indexer fire last
    String insertOrUpdateTrigger =
        "1" + column.getTable().getTableName() + "_" + column.getName() + "_UPDATE";

    // insert and update trigger
    jooq.execute(
        "CREATE FUNCTION {0}() RETURNS trigger AS"
            + "\n$BODY$"
            + "\nDECLARE"
            + "\n\t item {1};"
            + "\nBEGIN"
            // set to null all toTable rows that point to old 'me'. Might lead to null issues
            + "\n\tIF TG_OP = 'UPDATE' THEN"
            + "\n\t\tUPDATE {2} set {3} = NULL WHERE {3}=OLD.{4} ;"
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
        keyword(SqlTypeUtils.getPsqlType(column.getRefColumn())), // {1} type of item
        table(name(schemaName, column.getRefTableName())), // {2} toTable table
        field(name(column.getMappedBy())), // {3} mappedBy field
        field(
            name(
                getMappedByColumn(column)
                    .getRefColumnName())), // {4} mappedBy reference to 'me' (might not be pkey)
        field(name(column.getName())), // {5} the dummy column that triggers all this
        field(name(column.getRefColumnName())), // {6} toColumn where fake foreign key dummy points
        // to
        table(name(schemaName, column.getTable().getTableName())), // {7} this table
        field(name(column.getTable().getPrimaryKey()))); // {8} primary key of this table

    jooq.execute(
        "CREATE TRIGGER {0} "
            + "\n\tBEFORE INSERT OR UPDATE OF {1} ON {2}"
            + "\n\tFOR EACH ROW EXECUTE PROCEDURE {3}()",
        name(column.getName() + "_UPDATE"),
        name(column.getName()),
        name(schemaName, column.getTable().getTableName()),
        name(schemaName, insertOrUpdateTrigger));

    jooq.execute(
        "CREATE TRIGGER {0} "
            + "\n\tAFTER DELETE ON {1} "
            + "\n\tFOR EACH ROW EXECUTE PROCEDURE {2}()",
        name(column.getName() + "_DELETE"),
        name(schemaName, column.getTable().getTableName()),
        name(schemaName, insertOrUpdateTrigger));
  }
}
