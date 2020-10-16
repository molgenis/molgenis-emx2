package org.molgenis.emx2.sql;

import org.jooq.DSLContext;
import org.jooq.Name;
import org.jooq.Table;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Reference;

import java.util.stream.Collectors;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.sql.SqlColumnRefExecutor.validateRef;

class SqlColumnRefArrayExecutor {
  private SqlColumnRefArrayExecutor() {
    // hide
  }

  static void createRefArrayConstraints(DSLContext jooq, Column column) {
    validateRef(column);
    createReferenceExistsCheck(jooq, column);
    createDeleteOrUpdateReferedCheck(jooq, column);
    // createUpdateReferedCheck(jooq, column);
  }

  static void removeRefArrayConstraints(DSLContext jooq, Column column) {
    jooq.execute(
        "DROP TRIGGER {0} ON {1}",
        name(getDeleteTriggerName(column)), column.getRefTable().getJooqTable());
    //    jooq.execute(
    //        "DROP TRIGGER {0} ON {1}", name(getUpdateTriggerName(column)),
    // column.getRefTable().asJooqTable());
    jooq.execute(
        "DROP TRIGGER {0} ON {1}", name(getUpdateCheckName(column)), column.getJooqTable());
    jooq.execute("DROP FUNCTION {0}", name(column.getSchemaName(), getDeleteTriggerName(column)));
    //    jooq.execute(
    //        "DROP FUNCTION {0}",
    //        name(SqlColumnExecutor.getSchemaName(column), getUpdateTriggerName(column)));
    jooq.execute("DROP FUNCTION {0} ", name(column.getSchemaName(), getUpdateCheckName(column)));
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
        "CREATE OR REPLACE FUNCTION {0}() RETURNS trigger AS"
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

    String unnestRefs =
        column.getReferences().stream()
            .map(r -> "UNNEST(" + name(r.getName()) + ") AS " + name(r.getTo()))
            .collect(Collectors.joining(","));

    String oldEqualsAnyRef =
        column.getReferences().stream()
            .map(r -> "OLD." + name(r.getTo()) + "=ANY(" + name(r.getName()) + ")")
            .collect(Collectors.joining(" AND "));

    String keyColumns =
        column.getReferences().stream()
            .map(r -> name(r.getTo()).toString())
            .collect(Collectors.joining(","));

    String oldEqualsTo =
        column.getReferences().stream()
            .map(r -> "OLD." + name(r.getTo()) + "= " + name(r.getTo()))
            .collect(Collectors.joining(" AND "));

    String oldValuesAsString =
        column.getReferences().stream()
            .map(r -> "OLD." + name(r.getTo()))
            .collect(Collectors.joining("||','||"));

    String toColumns =
        column.getReferences().stream()
            .map(r -> name(r.getName()).toString())
            .collect(Collectors.joining(","));

    String newNotEqualsOld =
        column.getReferences().stream()
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
        column.getJooqTable(),
        // 4 anyFilter
        keyword(oldEqualsAnyRef),
        // 5 toTable
        inline(column.getRefTableName()),
        // 6 toColumns
        inline(keyColumns),
        // 7 old.toColumnValues
        keyword(oldValuesAsString),
        // 8 inline fromTable
        inline(column.getTableName()),
        // 9
        keyword(oldEqualsTo),
        // 10
        keyword(newNotEqualsOld));

    jooq.execute(
        "CREATE CONSTRAINT TRIGGER {0} "
            + "\n\tAFTER DELETE OR UPDATE OF {3} ON {1} "
            + "\n\tDEFERRABLE INITIALLY IMMEDIATE "
            + "\n\tFOR EACH ROW EXECUTE PROCEDURE {2}()",
        name(deleteTrigger),
        column.getRefTable().getJooqTable(),
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

    String functionName = getUpdateCheckName(column);

    String newFromColumns =
        column.getReferences().stream()
            .map(r -> "NEW." + name(r.getName()).toString())
            .collect(Collectors.joining(","));

    String fromColumns =
        column.getReferences().stream()
            .map(r -> name(r.getName()).toString())
            .collect(Collectors.joining(","));

    String toColumns =
        column.getReferences().stream()
            .map(r -> name(r.getTo()).toString())
            .collect(Collectors.joining(","));

    Name toTable = name(column.getTable().getSchema().getName(), column.getRefTableName());

    String errorColumns =
        column.getReferences().stream()
            .map(r -> "COALESCE(error_row." + name(r.getTo()).toString() + ",'NULL')")
            .collect(Collectors.joining("||','||"));

    String exceptFilter =
        column.getReferences().stream()
            .map(r -> name(r.getTo()) + " = ANY (NEW." + name(r.getName()) + ")")
            .collect(Collectors.joining(" AND "));

    jooq.execute(
        "CREATE OR REPLACE FUNCTION {0}() RETURNS trigger AS $BODY$ "
            + "\nDECLARE error_row RECORD;"
            + "\nBEGIN"
            + "\n\tFOR error_row IN SELECT * FROM UNNEST({1}) AS t({2}) EXCEPT SELECT {2} FROM {3} WHERE {10} LOOP"
            + "\n\t\tRAISE EXCEPTION USING ERRCODE='23503', MESSAGE = 'insert or update on table \"'||{9}||'\" violates foreign key constraint'"
            + " , DETAIL = 'Key ('||{6}||')=('|| {5} ||') is not present in table \"'||{7}||'\", column(s)('||{8}||')';"
            + "\n\tEND LOOP;"
            + "\n\tRETURN NEW;"
            + "\nEND; $BODY$ LANGUAGE plpgsql;",
        // 0
        name(column.getSchemaName(), functionName),
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
        inline(column.getTableName()),
        // 10
        keyword(exceptFilter));

    // add the trigger
    jooq.execute(
        "CREATE CONSTRAINT TRIGGER {0} "
            + "\n\tAFTER INSERT OR UPDATE OF {1} ON {2} FROM {3}"
            + "\n\tDEFERRABLE INITIALLY IMMEDIATE "
            + "\n\tFOR EACH ROW EXECUTE PROCEDURE {4}()",
        name(functionName),
        keyword(fromColumns),
        thisTable,
        toTable,
        name(column.getTable().getSchema().getName(), functionName));
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
