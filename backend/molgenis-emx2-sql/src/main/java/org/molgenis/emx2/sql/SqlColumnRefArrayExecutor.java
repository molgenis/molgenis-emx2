package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.sql.SqlColumnExecutor.validateColumn;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.jooq.DSLContext;
import org.jooq.Name;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Reference;

/**
 * Create refArray constraints. Might be composite key so therefore using Column...column
 * parameters.
 */
class SqlColumnRefArrayExecutor {
  private SqlColumnRefArrayExecutor() {
    // hide
  }

  static void createRefArrayConstraints(DSLContext jooq, Column column) {
    validateColumn(column);
    createReferenceExistsCheck(jooq, column);
    createReferedCheck(jooq, column);
    // createUpdateReferedCheck(jooq, column);
  }

  static void removeRefArrayConstraints(DSLContext jooq, Column ref) {
    jooq.execute(
        "DROP TRIGGER {0} ON {1}", name(getReferenceExistsCheckName(ref)), ref.getJooqTable());
    jooq.execute("DROP FUNCTION {0} ", name(ref.getSchemaName(), getReferenceExistsCheckName(ref)));
    jooq.execute(
        "DROP TRIGGER {0} ON {1}",
        name(getReferedCheckName(ref)), ref.getRefTable().getJooqTable());
    jooq.execute("DROP FUNCTION {0}", name(ref.getSchemaName(), getReferedCheckName(ref)));
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
  private static void createReferedCheck(DSLContext jooq, Column ref) {
    String deleteTrigger = getReferedCheckName(ref);
    Collection<Reference> references = ref.getReferences();

    String unnestRefs =
        references.stream()
            .map(
                r -> {
                  // can be overlapping with non_array reference
                  if (r.isOverlappingRef()) {
                    return name(r.getName()) + " AS " + name(r.getRefTo());
                  } else {
                    return "UNNEST(" + name(r.getName()) + ") AS " + name(r.getRefTo());
                  }
                })
            .collect(Collectors.joining(","));

    String oldEqualsAnyRef =
        references.stream()
            .map(
                r -> {
                  // can be overlapping with non_array reference
                  if (r.isOverlappingRef()) {
                    return "OLD." + name(r.getRefTo()) + "=" + name(r.getName());
                  } else {
                    return "OLD." + name(r.getRefTo()) + "=ANY(" + name(r.getName()) + ")";
                  }
                })
            .collect(Collectors.joining(" AND "));

    String keyColumns =
        references.stream()
            .map(r -> name(r.getRefTo()).toString())
            .collect(Collectors.joining(","));

    String oldEqualsTo =
        references.stream()
            .map(r -> "OLD." + name(r.getRefTo()) + "= " + name(r.getRefTo()))
            .collect(Collectors.joining(" AND "));

    String oldValuesAsString =
        references.stream()
            .map(r -> "OLD." + name(r.getRefTo()))
            .collect(Collectors.joining("||','||"));

    String toColumns =
        references.stream().map(r -> name(r.getName()).toString()).collect(Collectors.joining(","));

    String newNotEqualsOld =
        references.stream()
            .map(r -> "OLD." + name(r.getRefTo()) + " <> NEW." + name(r.getRefTo()))
            .collect(Collectors.joining(" OR "));

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
        name(ref.getSchemaName(), deleteTrigger),
        // 1
        inline(toColumns),
        // 2
        keyword(unnestRefs),
        // 3
        ref.getJooqTable(),
        // 4 anyFilter
        keyword(oldEqualsAnyRef),
        // 5 toTable
        inline(ref.getRefTableName()),
        // 6 toColumns
        inline(keyColumns),
        // 7 old.toColumnValues
        keyword(oldValuesAsString),
        // 8 inline fromTable
        inline(ref.getTableName()),
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
        ref.getRefTable().getJooqTable(),
        name(ref.getTable().getSchema().getName(), deleteTrigger),
        keyword(keyColumns));
  }

  private static String getReferedCheckName(Column column) {
    // todo, might be longer then 63 bytes!
    return "DEL_" + column.getSchemaName() + "_" + column.getTableName() + "_" + column.getName();
  }

  private static String getUpdateTriggerName(Column... column) {
    Column column1 = column[0];
    return "UPD_"
        + column1.getRefTableName()
        + "_CASCADE_"
        + column1.getTable().getTableName()
        + "_"
        + List.of(column).stream().map(Column::getName).collect(Collectors.joining(","));
  }

  /**
   * trigger on this column to check if foreign key exists. Might be composite key, i.e., list of
   * columns
   */
  private static void createReferenceExistsCheck(DSLContext jooq, Column column) {
    String schemaName = column.getSchema().getName();
    Name thisTable = name(schemaName, column.getTable().getTableName());
    Name toTable = name(column.getRefSchemaName(), column.getRefTableName());
    String functionName = getReferenceExistsCheckName(column);
    List<Reference> references = column.getReferences();

    String fromColumns =
        references.stream().map(r -> name(r.getName()).toString()).collect(Collectors.joining(","));

    String toColumns =
        references.stream()
            .map(r -> name(r.getRefTo()).toString())
            .collect(Collectors.joining(","));

    String errorColumns =
        references.stream()
            .map(r -> "COALESCE(error_row." + name(r.getRefTo()).toString() + ",'NULL')")
            .collect(Collectors.joining("||','||"));

    String exceptFilter =
        references.stream()
            .map(
                r -> {
                  if (r.isOverlappingRef()) {
                    return name(r.getRefTo()) + " = NEW." + name(r.getName());
                  } else {
                    return name(r.getRefTo()) + " = ANY (NEW." + name(r.getName()) + ")";
                  }
                })
            .collect(Collectors.joining(" AND "));

    String unnestRefs =
        references.stream()
            .map(
                r -> {
                  // can be overlapping with non_array reference
                  if (r.isOverlappingRef()) {
                    return "NEW." + name(r.getName()) + " AS " + name(r.getRefTo());
                  } else {
                    return "UNNEST(NEW." + name(r.getName()) + ") AS " + name(r.getRefTo());
                  }
                })
            .collect(Collectors.joining(","));

    String nonRefLinkFieldsAreNotNull =
        references.stream()
            .filter(r -> !r.isOverlapping())
            .map(r2 -> "error_row." + name(r2.getRefTo()) + " IS NOT NULL ")
            .collect(Collectors.joining(" OR "));

    jooq.execute(
        "CREATE OR REPLACE FUNCTION {0}() RETURNS trigger AS $BODY$ "
            + "\nDECLARE error_row RECORD;"
            + "\nBEGIN"
            + "\n\tFOR error_row IN SELECT {1} EXCEPT SELECT {2} FROM {3} WHERE {10} LOOP"
            // exclude if only refLink fields are set
            + "\n\t\tIF {11} THEN"
            + "\n\t\t\tRAISE EXCEPTION USING ERRCODE='23503', MESSAGE = 'insert or update on table \"'||{9}||'\" violates foreign key (ref_array) constraint'"
            + " , DETAIL = 'Key ('||{6}||')=('|| {5} ||') is not present in table \"'||{7}||'\", column(s)('||{8}||')';"
            + "\n\t\tEND IF;"
            + "\n\tEND LOOP;"
            + "\n\tRETURN NEW;"
            + "\nEND; $BODY$ LANGUAGE plpgsql;",
        // 0
        name(schemaName, functionName),
        // 1
        keyword(unnestRefs),
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
        keyword(exceptFilter),
        // 11
        keyword(nonRefLinkFieldsAreNotNull));

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

  private static String getReferenceExistsCheckName(Column column) {
    return "C_" + column.getSchemaName() + "_" + column.getTableName() + "_" + column.getName();
  }
}
