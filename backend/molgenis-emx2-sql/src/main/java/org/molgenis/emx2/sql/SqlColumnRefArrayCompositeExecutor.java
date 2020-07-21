package org.molgenis.emx2.sql;

import org.jooq.DSLContext;
import org.jooq.Name;
import org.molgenis.emx2.Column;

import static org.jooq.impl.DSL.*;

class SqlColumnRefArrayCompositeExecutor {
  private SqlColumnRefArrayCompositeExecutor() {
    // hide
  }

  static void createRefArrayConstraints(DSLContext jooq, Column column) {
    createReferenceExistsTrigger(jooq, column);
    createIsReferencedByTrigger(jooq, column);
  }

  static void removeRefArrayConstraints(DSLContext jooq, Column column) {
    jooq.execute(
        "DROP FUNCTION {0} CASCADE",
        name(SqlColumnExecutor.getSchemaName(column), getDeleteTriggerName(column)));
    jooq.execute(
        "DROP FUNCTION {0} CASCADE",
        name(SqlColumnExecutor.getSchemaName(column), getUpdateTriggerName(column)));
    jooq.execute(
        "DROP FUNCTION {0} CASCADE ",
        name(SqlColumnExecutor.getSchemaName(column), getUpdateCheckName(column)));
  }

  // this trigger is to check for foreign violations: to prevent that referenced records cannot be
  // changed/deleted in such a way that we get dangling foreign key references.
  private static void createIsReferencedByTrigger(DSLContext jooq, Column column) {
    // only support singular keys
    Name toTable = name(column.getTable().getSchema().getName(), column.getRefTableName());
    Name thisTable =
        name(column.getTable().getSchema().getName(), column.getTable().getTableName());
    Name thisColumn = name(column.getName());
    Name toColumn = name(column.getRefTable().getPrimaryKeys().get(0));

    String updateTrigger = getUpdateTriggerName(column);
    String deleteTrigger = getDeleteTriggerName(column);

    // cascade impossible? at least expensive!

    // any array that has matching identifiers should be unnested in intermediate result
    // that result is then checked for the key tuple
    // if exists, then replace the tuple, and write the query back as arrays

    // in case of cascade, simply update the jsonb records in those arrays, i.e.
    // {array}=array_replace({array},jsonb({old}),jsonb({new}))

    // in case of update of other end cascade
    jooq.execute(
        "CREATE FUNCTION {0}() RETURNS trigger AS"
            + "\n$BODY$"
            + "\n\tBEGIN"
            + "\n\tUPDATE {1} SET {3}=ARRAY_REPLACE({3}, OLD.{2}, NEW.{2}) WHERE OLD.{2} != NEW.{2} AND OLD.{2} = ANY ({3});"
            + "\n\tRETURN NEW;"
            + "\nEND;"
            + "\n$BODY$ LANGUAGE plpgsql;",
        name(column.getTable().getSchema().getName(), updateTrigger),
        thisTable,
        toColumn,
        thisColumn);

    // create the cascade trigger
    jooq.execute(
        "CREATE CONSTRAINT TRIGGER {0} "
            + "\n\tAFTER UPDATE OF {1} ON {2} "
            + "\n\tDEFERRABLE INITIALLY IMMEDIATE "
            + "\n\tFOR EACH ROW EXECUTE PROCEDURE {3}()",
        name(updateTrigger),
        toColumn,
        toTable,
        name(column.getTable().getSchema().getName(), updateTrigger));

    // in case of delete should fail if still pointed to

    // fail if exist select * from (select unnest(refa,refb) from table_from where old.keya =
    // any(refa) and old.keyb = any(refb)) where keya=refa and keyb=refb

    jooq.execute(
        "CREATE FUNCTION {0}() RETURNS trigger AS"
            + "\n$BODY$"
            + "\n\tBEGIN"
            + "\n\tIF(EXISTS(SELECT * from {1} WHERE OLD.{2} = ANY({3}) ) ) THEN "
            + "RAISE EXCEPTION USING ERRCODE='23503', MESSAGE = 'update or delete on table {4} violates foreign key constraint on table {5}'"
            + " , DETAIL = 'Key ({6})=('|| OLD.{2} ||') is still referenced from table {5}';"
            + "\n\tEND IF;"
            + "\n\tRETURN NEW;"
            + "\nEND;"
            + "\n$BODY$ LANGUAGE plpgsql;",
        name(column.getTable().getSchema().getName(), deleteTrigger),
        thisTable,
        toColumn,
        thisColumn,
        inline(column.getRefTableName()),
        inline(column.getTable().getTableName()),
        inline(toColumn.getName()));

    jooq.execute(
        "CREATE CONSTRAINT TRIGGER {0} "
            + "\n\tAFTER DELETE ON {1} "
            + "\n\tDEFERRABLE INITIALLY IMMEDIATE "
            + "\n\tFOR EACH ROW EXECUTE PROCEDURE {2}()",
        name(deleteTrigger), toTable, name(column.getTable().getSchema().getName(), deleteTrigger));
  }

  private static String getDeleteTriggerName(Column column) {
    return "DEL_"
        + column.getRefTableName()
        + "_CHECK_"
        + column.getTable().getTableName()
        + "_"
        + column.getName();
  }

  private static String getUpdateTriggerName(Column column) {
    return "UPD_"
        + column.getRefTableName()
        + "_CASCADE_"
        + column.getTable().getTableName()
        + "_"
        + column.getName();
  }

  /** trigger on this column to check if foreign key exists */
  private static void createReferenceExistsTrigger(DSLContext jooq, Column column) {
    Name thisTable =
        name(column.getTable().getSchema().getName(), column.getTable().getTableName());
    Name thisColumn = name(column.getName());
    Name toTable = name(column.getTable().getSchema().getName(), column.getRefTableName());
    Name toColumn = name(column.getRefTable().getPrimaryKeys().get(0));

    Name functionName = name(SqlColumnExecutor.getSchemaName(column), getUpdateCheckName(column));

    // the function

    // foreach select unnest(new.refa, new.refb) as(keya,keyb) exclude select (keya,keyb) from
    // othertable
    // raise exception

    jooq.execute(
        "CREATE FUNCTION {0}() RETURNS trigger AS $BODY$ BEGIN"
            + "\n\tFOR error_row IN SELECT UNNEST({fromColumns}) AS {toColumn} EXCEPT SELECT {toColumn} FROM {toTable}"
            + "\n\tLOOP"
            + "RAISE EXCEPTION USING ERRCODE='23503', MESSAGE = 'insert or update on table {fromTable} violates foreign key constraint'"
            + " , DETAIL = 'Key ({fromKey})=('|| {error_row.fromkey} ||') is not present in table {toTable}, columns {toColumns}';"
            + "\n\tEND LOOP;"
            + "\nEND; $BODY$ LANGUAGE plpgsql;",
        functionName);

    jooq.execute(
        "CREATE FUNCTION {0}() RETURNS trigger AS"
            + "\n$BODY$"
            + "\nDECLARE"
            + "\n\t test {1};"
            + "\nBEGIN"
            + "\n\ttest =  ARRAY (SELECT from_column FROM (SELECT UNNEST(NEW.{2}) as from_column) as from_table "
            + "LEFT JOIN (SELECT {3} as to_column FROM {4}) as to_table "
            + "ON from_table.from_column=to_table.to_column WHERE to_table.to_column IS NULL);"
            + "\n\tIF(array_length(test,1) > 0) THEN "
            + "RAISE EXCEPTION USING ERRCODE='23503', MESSAGE = 'update or delete on table {5} violates foreign key constraint'"
            + " , DETAIL = 'Key ({6})=('|| array_to_string(test,',') ||') is not present in table {7}, column {8}';"
            + "\n\tEND IF;"
            + "\n\tRETURN NEW;"
            + "\nEND;"
            + "\n$BODY$ LANGUAGE plpgsql;",
        functionName, // {0}
        keyword(
            SqlTypeUtils.getPsqlType(column.getRefTable().getPrimaryKeyColumns().get(0))
                + "[]"), // {1}
        thisColumn, // {2}
        toColumn, // {3}
        toTable, // {4}
        inline(column.getTable().getTableName()), // {5}
        inline(column.getName()), // {6}
        inline(column.getRefTableName()), // {7}
        inline(column.getRefTable().getPrimaryKeys().get(0))); // {8}

    // add the trigger
    jooq.execute(
        "CREATE CONSTRAINT TRIGGER {0} "
            + "\n\tAFTER INSERT OR UPDATE OF {1} ON {2} FROM {3}"
            + "\n\tDEFERRABLE INITIALLY IMMEDIATE "
            + "\n\tFOR EACH ROW EXECUTE PROCEDURE {4}()",
        name(column.getTable().getTableName() + "_" + column.getName()),
        thisColumn,
        thisTable,
        toTable,
        functionName);
  }

  private static String getUpdateCheckName(Column column) {
    return "UPD_"
        + column.getTable().getTableName()
        + "_CHECK_"
        + column.getTable().getTableName()
        + "_"
        + column.getName();
  }
}
