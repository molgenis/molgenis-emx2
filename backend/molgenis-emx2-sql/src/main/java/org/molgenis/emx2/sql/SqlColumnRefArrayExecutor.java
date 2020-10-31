package org.molgenis.emx2.sql;

import org.jooq.DSLContext;
import org.jooq.Name;
import org.jooq.Table;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Reference;

import java.util.List;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.sql.SqlColumnRefExecutor.validateRef;

/**
 * Create refArray constraints. Might be composite key so therefore using Column...column
 * parameters.
 */
class SqlColumnRefArrayExecutor {
  private SqlColumnRefArrayExecutor() {
    // hide
  }

  static void createRefArrayConstraints(DSLContext jooq, Column... column) {
    validateRef(column);
    createReferenceExistsCheck(jooq, column);
    createDeleteOrUpdateReferedCheck(jooq, column);
    // createUpdateReferedCheck(jooq, column);
  }

  static void removeRefArrayConstraints(DSLContext jooq, Column... column) {
    Column column1 = column[0];
    jooq.execute(
        "DROP TRIGGER {0} ON {1}",
        name(getDeleteTriggerName(column)), column1.getRefTable().getJooqTable());
    //    jooq.execute(
    //        "DROP TRIGGER {0} ON {1}", name(getUpdateTriggerName(column)),
    // column.getRefTable().asJooqTable());
    jooq.execute(
        "DROP TRIGGER {0} ON {1}", name(getUpdateCheckName(column)), column1.getJooqTable());
    jooq.execute("DROP FUNCTION {0}", name(column1.getSchemaName(), getDeleteTriggerName(column)));
    //    jooq.execute(
    //        "DROP FUNCTION {0}",
    //        name(SqlColumnExecutor.getSchemaName(column), getUpdateTriggerName(column)));
    jooq.execute("DROP FUNCTION {0} ", name(column1.getSchemaName(), getUpdateCheckName(column)));
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

  /** update check; in case of composite key this consists of multiple Column */
  private static void createDeleteOrUpdateReferedCheck(DSLContext jooq, Column... column) {
    List<Column> columns = List.of(column);
    Column column1 = column[0];
    String deleteTrigger = getDeleteTriggerName(column1);

    String unnestRefs =
        columns.stream()
            .map(r -> "UNNEST(" + name(r.getName()) + ") AS " + name(r.getRefColumnName()))
            .collect(Collectors.joining(","));

    String oldEqualsAnyRef =
        columns.stream()
            .map(r -> "OLD." + name(r.getRefColumnName()) + "=ANY(" + name(r.getName()) + ")")
            .collect(Collectors.joining(" AND "));

    String keyColumns =
        columns.stream()
            .map(r -> name(r.getRefColumnName()).toString())
            .collect(Collectors.joining(","));

    String oldEqualsTo =
        columns.stream()
            .map(r -> "OLD." + name(r.getRefColumnName()) + "= " + name(r.getRefColumnName()))
            .collect(Collectors.joining(" AND "));

    String oldValuesAsString =
        columns.stream()
            .map(r -> "OLD." + name(r.getRefColumnName()))
            .collect(Collectors.joining("||','||"));

    String toColumns =
        columns.stream().map(r -> name(r.getName()).toString()).collect(Collectors.joining(","));

    String newNotEqualsOld =
        columns.stream()
            .map(r -> "OLD." + name(r.getRefColumnName()) + " <> NEW." + name(r.getRefColumnName()))
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
        name(column1.getTable().getSchemaName(), deleteTrigger),
        // 1
        inline(toColumns),
        // 2
        keyword(unnestRefs),
        // 3
        column1.getJooqTable(),
        // 4 anyFilter
        keyword(oldEqualsAnyRef),
        // 5 toTable
        inline(column1.getRefTableName()),
        // 6 toColumns
        inline(keyColumns),
        // 7 old.toColumnValues
        keyword(oldValuesAsString),
        // 8 inline fromTable
        inline(column1.getTableName()),
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
        column1.getRefTable().getJooqTable(),
        name(column1.getTable().getSchema().getName(), deleteTrigger),
        keyword(keyColumns));
  }

  private static String getDeleteTriggerName(Column... column) {
    Column column1 = column[0];

    return "DEL_"
        + column1.getRefTableName()
        + "_CHECK_"
        + column1.getTable().getTableName()
        + "_"
        + List.of(column).stream().map(c -> c.getName()).collect(Collectors.joining(","));
  }

  private static String getUpdateTriggerName(Column... column) {
    Column column1 = column[0];
    return "UPD_"
        + column1.getRefTableName()
        + "_CASCADE_"
        + column1.getTable().getTableName()
        + "_"
        + List.of(column).stream().map(c -> c.getName()).collect(Collectors.joining(","));
  }

  /**
   * trigger on this column to check if foreign key exists. Might be composite key, i.e., list of
   * columns
   */
  private static void createReferenceExistsCheck(DSLContext jooq, Column... column) {
    List<Column> columns = List.of(column);
    Column column1 = columns.get(0);
    String schemaName = column1.getSchema().getName();
    Name thisTable = name(schemaName, column1.getTable().getTableName());
    Name toTable = name(schemaName, column1.getRefTableName());
    String functionName = getUpdateCheckName(column1);

    String newFromColumns =
        columns.stream()
            .map(r -> "NEW." + name(r.getName()).toString())
            .collect(Collectors.joining(","));

    String fromColumns =
        columns.stream().map(r -> name(r.getName()).toString()).collect(Collectors.joining(","));

    String toColumns =
        columns.stream()
            .map(r -> name(r.getRefColumnName()).toString())
            .collect(Collectors.joining(","));

    String errorColumns =
        columns.stream()
            .map(r -> "COALESCE(error_row." + name(r.getRefColumnName()).toString() + ",'NULL')")
            .collect(Collectors.joining("||','||"));

    String exceptFilter =
        columns.stream()
            .map(r -> name(r.getRefColumnName()) + " = ANY (NEW." + name(r.getName()) + ")")
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
        name(schemaName, functionName),
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
        inline(column1.getRefTableName()),
        // 8
        inline(toColumns),
        // 9
        inline(column1.getTableName()),
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
        name(column1.getTable().getSchema().getName(), functionName));
  }

  private static String getUpdateCheckName(Column... column) {
    Column column1 = column[0];
    return "UPD_"
        + column1.getTable().getTableName()
        + "_CHECK_"
        + column1.getTable().getTableName()
        + "_"
        + List.of(column).stream().map(c -> c.getName()).collect(Collectors.joining(","));
  }
}
