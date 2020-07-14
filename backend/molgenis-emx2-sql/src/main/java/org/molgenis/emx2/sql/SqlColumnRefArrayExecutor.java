package org.molgenis.emx2.sql;

import org.jooq.DSLContext;
import org.jooq.Name;
import org.jooq.Table;
import org.molgenis.emx2.Column;

import java.util.stream.Collectors;

import static org.jooq.impl.DSL.*;

class SqlColumnRefArrayExecutor {
  private SqlColumnRefArrayExecutor() {
    // hide
  }

  static void createRefArrayConstraints(DSLContext jooq, Column column) {
    //    if (column.getRefTable().getPrimaryKeys().size() > 1) {
    //      throw new MolgenisException(
    //          "Create ref_array '" + column.getName() + "' failed",
    //          "refTable "
    //              + column.getRefTableName()
    //              + " must have singular primary key; multiple not supported then use mref");
    //    }

    // create indexes
    //    for (Reference r : column.getRefColumns()) {
    //      jooq.execute(
    //          "CREATE INDEX {0} ON {1} USING GIN({2})",
    //          name(r.getName() + "-idx"), column.getJooqTable(), r.asJooqField());
    //    }

    createReferenceExistsCheck(jooq, column);
    createDeleteOrUpdateReferedCheck(jooq, column);
    // createUpdateReferedCheck(jooq, column);
  }

  static void removeRefArrayConstraints(DSLContext jooq, Column column) {
    jooq.execute(
        "DROP FUNCTION {0} CASCADE",
        name(SqlColumnExecutor.getSchemaName(column), getDeleteTriggerName(column)));
    //    jooq.execute(
    //        "DROP FUNCTION {0} CASCADE",
    //        name(SqlColumnExecutor.getSchemaName(column), getUpdateTriggerName(column)));
    jooq.execute(
        "DROP FUNCTION {0} CASCADE ",
        name(SqlColumnExecutor.getSchemaName(column), getUpdateCheckName(column)));
  }

  // this trigger is to check for foreign violations: to prevent that referenced records cannot be
  // changed/deleted in such a way that we get dangling foreign key references.
  private static void createUpdateReferedCheck(DSLContext jooq, Column column) {
    // only support singular keys
    Name toTable = name(column.getTable().getSchema().getName(), column.getRefTableName());
    Name thisTable =
        name(column.getTable().getSchema().getName(), column.getTable().getTableName());
    Name thisColumn = name(column.getName());
    Name toColumn = name(column.getRefTable().getPrimaryKeys().get(0));

    String updateTrigger = getUpdateTriggerName(column);

    // in case of update of other end cascade
    jooq.execute(
        "CREATE FUNCTION {0}() RETURNS trigger AS"
            + "\n$BODY$"
            + "\n\tBEGIN"
            + "\n\tUPDATE {1} SET {3}=ARRAY_REPLACE({3}, OLD.{2}, NEW.{2}) WHERE OLD.{2} != NEW.{2} AND OLD.{2} = ANY ({3});"
            + "\n\tRETURN NEW;"
            + "\nEND;"
            + "\n$BODY$ LANGUAGE plpgsql;",
        // 0 name of trigger
        name(column.getTable().getSchema().getName(), updateTrigger),
        // 1 name of this table
        thisTable,
        // 2 name of to column
        toColumn,
        // 3 name of this column
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
  }

  private static void createDeleteOrUpdateReferedCheck(DSLContext jooq, Column column) {
    String deleteTrigger = getDeleteTriggerName(column);

    Table toTable = column.getRefTable().asJooqTable();

    String unnestRefs =
        column.getRefColumns().stream()
            .map(r -> "UNNEST(" + name(r.getName()) + ") AS " + name(r.getTo()))
            .collect(Collectors.joining(","));
    Table fromTable = column.getJooqTable();

    String anyFilter =
        column.getRefColumns().stream()
            .map(r -> "OLD." + name(r.getTo()) + "=ANY(" + name(r.getName()) + ")")
            .collect(Collectors.joining(" AND "));

    String keyColumns =
        column.getRefColumns().stream()
            .map(r -> name(r.getTo()).toString())
            .collect(Collectors.joining(","));

    String oldValues =
        column.getRefColumns().stream()
            .map(r -> "OLD." + name(r.getTo()) + "= " + name(r.getTo()))
            .collect(Collectors.joining(" AND "));

    String oldValuesAsString =
        column.getRefColumns().stream()
            .map(r -> "OLD." + name(r.getTo()))
            .collect(Collectors.joining("||','||"));

    String toColumns =
        column.getRefColumns().stream()
            .map(r -> name(r.getName()).toString())
            .collect(Collectors.joining(","));

    String newNotEqualsOld =
        column.getRefColumns().stream()
            .map(r -> "OLD." + name(r.getTo()) + " <> NEW." + name(r.getTo()))
            .collect(Collectors.joining(" AND "));

    jooq.execute(
        "CREATE OR REPLACE FUNCTION {0}() RETURNS trigger AS $BODY$ "
            + "\nBEGIN"
            + "\n\tIF (TG_OP='DELETE' OR {10}) AND EXISTS (SELECT * FROM (SELECT {2} FROM {3} WHERE {4}) AS t WHERE {9}) THEN"
            + "\n\t\tRAISE EXCEPTION USING ERRCODE='23503', MESSAGE = 'delete on table \"'||{5}||'\" violates foreign key constraint'"
            + " , DETAIL = 'Key ('||{6}||')=('|| {7} ||') is still referenced from table \"'||{8}||'\", column(s)('||{1}||')';"
            + "\n\tEND IF;"
            + "\n\tRETURN NEW;"
            + "\nEND; $BODY$ LANGUAGE plpgsql;",
        // 0 trigger name
        name(column.getTable().getSchemaName(), deleteTrigger),
        // 1
        inline(toColumns),
        // 2
        keyword(unnestRefs),
        // 3
        fromTable,
        // 4 anyFilter
        keyword(anyFilter),
        // 5 toTable
        inline(column.getRefTableName()),
        // 6 toColumns
        inline(keyColumns),
        // 7 old.toColumnValues
        keyword(oldValuesAsString),
        // 8 inline fromTable
        inline(column.getTableName()),
        // 9
        keyword(oldValues),
        // 10
        keyword(newNotEqualsOld));

    //    jooq.execute(
    //        "CREATE FUNCTION {0}() RETURNS trigger AS"
    //            + "\n$BODY$"
    //            + "\n\tBEGIN"
    //            + "\n\tIF(EXISTS(SELECT * from {1} WHERE OLD.{2} = ANY({3}) ) ) THEN "
    //            + "RAISE EXCEPTION USING ERRCODE='23503', MESSAGE = 'update or delete on table {4}
    // violates foreign key constraint on table {5}'"
    //            + " , DETAIL = 'Key ({6})=('|| OLD.{2} ||') is still referenced from table {5}';"
    //            + "\n\tEND IF;"
    //            + "\n\tRETURN NEW;"
    //            + "\nEND;"
    //            + "\n$BODY$ LANGUAGE plpgsql;",
    //        // 0
    //        name(column.getTable().getSchema().getName(), deleteTrigger),
    //        // 1
    //        thisTable,
    //        // 2
    //        toColumn,
    //        // 3
    //        thisColumn,
    //        // 4
    //        inline(column.getRefTableName()),
    //        // 5
    //        inline(column.getTable().getTableName()),
    //        // 6
    //        inline(toColumn.getName()));

    jooq.execute(
        "CREATE CONSTRAINT TRIGGER {0} "
            + "\n\tAFTER DELETE OR UPDATE OF {3} ON {1} "
            + "\n\tDEFERRABLE INITIALLY IMMEDIATE "
            + "\n\tFOR EACH ROW EXECUTE PROCEDURE {2}()",
        name(deleteTrigger),
        toTable,
        name(column.getTable().getSchema().getName(), deleteTrigger),
        keyword(keyColumns));
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
  private static void createReferenceExistsCheck(DSLContext jooq, Column column) {

    Name thisTable =
        name(column.getTable().getSchema().getName(), column.getTable().getTableName());

    Name functionName = name(SqlColumnExecutor.getSchemaName(column), getUpdateCheckName(column));

    String newFromColumns =
        column.getRefColumns().stream()
            .map(r -> "NEW." + name(r.getName()).toString())
            .collect(Collectors.joining(","));

    String fromColumns =
        column.getRefColumns().stream()
            .map(r -> name(r.getName()).toString())
            .collect(Collectors.joining(","));

    String toColumns =
        column.getRefColumns().stream()
            .map(r -> name(r.getTo()).toString())
            .collect(Collectors.joining(","));

    Name toTable = name(column.getTable().getSchema().getName(), column.getRefTableName());

    String errorColumns =
        column.getRefColumns().stream()
            .map(r -> "|| error_row." + name(r.getTo()).toString())
            .collect(Collectors.joining("||','||"));

    jooq.execute(
        "CREATE OR REPLACE FUNCTION {0}() RETURNS trigger AS $BODY$ "
            + "\nDECLARE error_row RECORD;"
            + "\nBEGIN"
            + "\n\tFOR error_row IN SELECT * FROM UNNEST({1}) AS t({2}) EXCEPT SELECT {2} FROM {3} LOOP"
            + "\n\t\tRAISE EXCEPTION USING ERRCODE='23503', MESSAGE = 'insert or update on table \"'||{9}||'\" violates foreign key constraint'"
            + " , DETAIL = 'Key ('||{6}||')=('{5} ||') is not present in table \"'||{7}||'\", column(s)('||{8}||')';"
            + "\n\tEND LOOP;"
            + "\n\tRETURN NEW;"
            + "\nEND; $BODY$ LANGUAGE plpgsql;",
        // 0
        functionName,
        // 1
        keyword(newFromColumns),
        // 2
        keyword(toColumns),
        // 3
        toTable,
        // 4
        thisTable,
        // 5
        keyword(errorColumns),
        // 6
        inline(fromColumns),
        // 7
        inline(column.getRefTableName()),
        // 8
        inline(toColumns),
        // 9
        inline(column.getTableName()));

    // check foreign key exists
    //    jooq.execute(
    //        "CREATE FUNCTION {0}() RETURNS trigger AS"
    //            + "\n$BODY$"
    //            + "\nDECLARE"
    //            + "\n\t test {1};"
    //            + "\nBEGIN"
    //            + "\n\ttest =  ARRAY (SELECT from_column FROM (SELECT UNNEST(NEW.{2}) as
    // from_column) as from_table "
    //            + "LEFT JOIN (SELECT {3} as to_column FROM {4}) as to_table "
    //            + "ON from_table.from_column=to_table.to_column WHERE to_table.to_column IS
    // NULL);"
    //            + "\n\tIF(array_length(test,1) > 0) THEN "
    //            + "RAISE EXCEPTION USING ERRCODE='23503', MESSAGE = 'update or delete on table {5}
    // violates foreign key constraint'"
    //            + " , DETAIL = 'Key ({6})=('|| array_to_string(test,',') ||') is not present in
    // table {7}, column {8}';"
    //            + "\n\tEND IF;"
    //            + "\n\tRETURN NEW;"
    //            + "\nEND;"
    //            + "\n$BODY$ LANGUAGE plpgsql;",
    //        functionName, // {0}
    //        keyword(
    //            SqlTypeUtils.getPsqlType(column.getRefTable().getPrimaryKeyColumns().get(0))
    //                + "[]"), // {1}
    //        thisColumn, // {2}
    //        toColumn, // {3}
    //        toTable, // {4}
    //        inline(column.getTable().getTableName()), // {5}
    //        inline(column.getName()), // {6}
    //        inline(column.getRefTableName()), // {7}
    //        inline(column.getRefTable().getPrimaryKeys().get(0))); // {8}

    // add the trigger
    jooq.execute(
        "CREATE CONSTRAINT TRIGGER {0} "
            + "\n\tAFTER INSERT OR UPDATE OF {1} ON {2} FROM {3}"
            + "\n\tDEFERRABLE INITIALLY IMMEDIATE "
            + "\n\tFOR EACH ROW EXECUTE PROCEDURE {4}()",
        name(column.getTable().getTableName() + "_" + column.getName()),
        keyword(fromColumns),
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
