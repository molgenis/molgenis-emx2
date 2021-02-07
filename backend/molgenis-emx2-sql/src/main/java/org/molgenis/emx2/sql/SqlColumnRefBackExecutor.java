package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.sql.SqlColumnExecutor.validateColumn;

import java.util.List;
import java.util.stream.Collectors;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Reference;

class SqlColumnRefBackExecutor {
  private SqlColumnRefBackExecutor() {
    // hide
  }

  // will create a dummy array column matching the toColumn we will link to
  // will create a before insert trigger to update all REF instances in the other table that needs
  // updating
  static void createRefBackColumnConstraints(DSLContext jooq, Column ref) {

    try {
      // get ref table
      validateColumn(ref);
      String columNames =
          ref.getReferences().stream().map(Reference::getName).collect(Collectors.joining(","));

      // get the via column which is also in the 'toTable'
      String mappedByColumnName = ref.getMappedBy();
      if (mappedByColumnName == null) {
        throw new MolgenisException(
            "Create column failed: Create of REFBACK column(s) '"
                + columNames
                + "' failed because mappedBy was not set.");
      }

      // create the trigger so that insert/update/delete on REFBACK column updates the
      // relationship
      Column mappedBy = ref.getMappedByColumn();
      if (mappedBy == null) {
        throw new MolgenisException(
            "Set refback on column '"
                + ref.getTableName()
                + "."
                + ref.getName()
                + "'failed: mappedBy column '"
                + ref.getMappedBy()
                + "'not found");
      }
      ColumnType mappedByType = mappedBy.getColumnType();
      switch (mappedByType) {
        case REF:
          createTriggerForRef(jooq, ref);
          break;
        case REF_ARRAY:
          createTriggerForRefArray(jooq, ref);
          break;
          // todo case MREF:
        default:
          throw new MolgenisException(
              "Create column failed: Create of REFBACK column(s) '"
                  + ref.getTableName()
                  + "."
                  + getNames(ref)
                  + "' failed because mappedBy '"
                  + ref.getMappedBy()
                  + "' was not of type REF, REF_ARRAY");
      }
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException(
          "REFBACK column(s) '"
              + getNames(ref)
              + "' could not be created in table '"
              + ref.getTableName()
              + "'",
          dae);
    }
  }

  private static void createTriggerForRefArray(DSLContext jooq, Column ref2) {

    String schemaName = ref2.getTable().getSchema().getName();
    String updateTriggerName = refbackUpdateTriggerName(ref2);
    List<Reference> columns = ref2.getReferences();

    // ref
    String ref =
        columns.stream().map(r -> name(r.getName()).toString()).collect(Collectors.joining(","));
    String newRef =
        columns.stream().map(r -> "NEW." + name(r.getName())).collect(Collectors.joining(","));
    String refTo =
        columns.stream().map(r -> name(r.getRefTo()).toString()).collect(Collectors.joining(","));
    String oldRefTo =
        columns.stream().map(r -> "OLD." + name(r.getRefTo())).collect(Collectors.joining(","));
    String errorColumns =
        columns.stream()
            .map(r -> "error_row." + name(r.getRefTo()).toString())
            .collect(Collectors.joining("||','||"));

    // mappedBy.to
    List<Reference> mappedByColumns = ref2.getMappedByColumn().getReferences();
    String mappedByTo =
        mappedByColumns.stream()
            .map(r -> name(r.getRefTo()).toString())
            .collect(Collectors.joining(","));
    String oldMappedByTo =
        mappedByColumns.stream()
            .map(r -> "OLD." + name(r.getRefTo()))
            .collect(Collectors.joining(","));
    String newMappedByTo =
        mappedByColumns.stream()
            .map(r -> "NEW." + name(r.getRefTo())) // + " AS " + name(r.getTo()))
            .collect(Collectors.joining(","));
    String mappedByToEqualsNewKey =
        mappedByColumns.stream()
            .map(Reference::getRefTo)
            .map(r -> name(r) + "=NEW." + name(r))
            .collect(Collectors.joining(" AND "));
    String mappedByToEqualsOldKey =
        mappedByColumns.stream()
            .map(Reference::getRefTo)
            .map(r -> name(r) + "=OLD." + name(r))
            .collect(Collectors.joining(" AND "));

    String updateFilter =
        columns.stream()
            .map(Reference::getRefTo)
            .map(r -> "t." + name(r) + "=error_row." + name(r))
            .collect(Collectors.joining(" AND "));

    String mappedByFrom =
        mappedByColumns.stream()
            .map(r -> name(r.getName()).toString())
            .collect(Collectors.joining(","));

    String mappedByFromErrorRow =
        mappedByColumns.stream()
            .map(r -> "error_row." + name(r.getName()).toString())
            .collect(Collectors.joining(","));
    String setterUpdate =
        mappedByColumns.stream()
            .map(r -> name(r.getName()) + "=t3." + name(r.getName()))
            .collect(Collectors.joining(","));
    String arrayAgg =
        mappedByColumns.stream()
            .map(r -> "array_agg(" + name(r.getRefTo()) + ") as " + name(r.getName()))
            .collect(Collectors.joining(","));
    String setRefToNull =
        columns.stream()
            .map(r -> "NEW." + name(r.getName()) + "=NULL")
            .collect(Collectors.joining(";"));

    jooq.execute(
        "CREATE FUNCTION {0}() RETURNS trigger AS"
            + "\n$BODY$"
            + "\nDECLARE error_row RECORD;"
            + "\nBEGIN"
            + "\n-- raise error for first refColumn value that does not in refTable key values "
            + "\n\tFOR error_row IN SELECT * FROM UNNEST({5}) as u({6}) EXCEPT (SELECT {6} FROM {2}) LOOP"
            + "\n\t\tRAISE EXCEPTION USING ERRCODE='23503', "
            + "\n\t\tMESSAGE = 'update or delete on table '||{9}||' violates foreign key constraint',"
            + "\n\t\tDETAIL = 'Key ('||{10}||')=('|| {13} ||') is not present in table '||{11}||', column '||{12};"
            + "\n\tEND LOOP;"
            // check for on conflict update via 'not exists ...'
            + "\n\tIF TG_OP='UPDATE' OR NOT EXISTS (SELECT 1 FROM {7} WHERE {8}) THEN "
            // === (1) remove references that are not needed
            + "\n\t\tFOR error_row IN SELECT {19},{3} FROM {2} "
            // where OLD ids in the list
            + "\n\t\t\tWHERE ({4}) IN (SELECT * FROM UNNEST({3}) as u({21}))"
            // but not in the refback
            + "\n\t\t\tAND ({6}) NOT IN (SELECT * FROM UNNEST({5}) as u({6})) "
            + "\n\t\tLOOP UPDATE {2} AS t SET {1} FROM ("
            + "\n\t\t\tSELECT {17} FROM ("
            // set copy of previous ref_array values except the values that need removal
            + "\n\t\t\t\tSELECT * FROM UNNEST({20}) AS t1({21}) EXCEPT (SElECT {4})"
            + "\n\t\t\t) AS t2"
            + "\n\t\t) AS t3 WHERE {18}; END LOOP;"
            // === (2) add missing refs
            + "\n\t\tFOR error_row IN SELECT {19},{3} FROM {2} "
            // in the refback
            + "\n\t\t\tWHERE ({6}) IN (SELECT * FROM UNNEST({5}) as u({6})) "
            // and not yet in the ref_array
            + "\n\t\t\t AND ({14}) NOT IN (SELECT * FROM UNNEST({3}) as u({21})) "
            // update arrays to include the new refback
            + "\n\t\tLOOP UPDATE {2} AS t SET {1} FROM ("
            + "\n\t\t\tSELECT {17} FROM ("
            + "\n\t\t\t\tSELECT * FROM UNNEST({20}) AS t1({21}) UNION (SELECT {14})"
            + "\n\t\t\t) AS t2"
            + "\n\t\t) AS t3 WHERE {18}; END LOOP;"
            + "\n\tEND IF;"
            + "\n\tIF TG_OP='UPDATE' OR NOT EXISTS (SELECT 1 FROM {7} WHERE {8}) THEN {15}; END IF;"
            + "\n\tRETURN NEW;"
            + "\nEND;"
            + "\n$BODY$ LANGUAGE plpgsql;",

        // 0 function name
        name(schemaName, updateTriggerName),
        // 1 to replace old mappedBy array(s) with new array(s)
        keyword(setterUpdate),
        // 2 refTable
        table(name(schemaName, ref2.getRefTableName())),
        // 3 mappedBy
        keyword(mappedByFrom),
        // 4 key that mappedBy uses (might not be pkey)
        keyword(oldMappedByTo),
        // 5 the column, in case of composite decomposed
        keyword(newRef),
        // 6 toColumn where fake foreign key dummy points to
        keyword(refTo),
        // 7 this table
        table(name(schemaName, ref2.getTable().getTableName())),
        // 8 check if pkey in new.pkey (i.e. if insert or update)
        keyword(mappedByToEqualsNewKey),
        // 9 inline table name
        inline(ref2.getTable().getTableName()),
        // 10 name
        inline(getNames(ref2)),
        // 11 inline refTable name
        inline(ref2.getRefTableName()),
        // 12 inline refTable key column names
        inline(refTo),
        // 13 columns concat for errors
        keyword(errorColumns),
        // 14 new key
        keyword(newMappedByTo),
        // 15 set new.ref to null
        keyword(setRefToNull),
        // 16 keyEqualsOldKey
        keyword(mappedByToEqualsOldKey),
        // 17
        keyword(arrayAgg),
        // 18
        keyword(updateFilter),
        // 19
        keyword(refTo),
        // 20
        keyword(mappedByFromErrorRow),
        // 21
        keyword(mappedByTo),
        // 22
        keyword(oldRefTo));

    // attach the trigger

    jooq.execute(
        "CREATE TRIGGER {0} "
            + "\n\tBEFORE INSERT OR UPDATE OF {1} ON {2}"
            + "\n\tFOR EACH ROW EXECUTE PROCEDURE {3}()",
        name(updateTriggerName),
        keyword(ref),
        name(schemaName, ref2.getTableName()),
        name(schemaName, updateTriggerName));

    // delete and truncate trigger

    String deleteTriggerName = refbackDeleteTriggerName(ref2);
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
        table(name(schemaName, ref2.getRefTableName())), // {1} this table
        field(name(ref2.getMappedBy())), // {2} mappedBy
        field(
            name(
                ref2.getMappedByColumn()
                    .getRefTable()
                    .getPrimaryKeys()
                    .get(0)))); // {3} key that mappedBy uses (might not be pkey)

    // attach trigger
    jooq.execute(
        "CREATE TRIGGER {0} "
            + "\n\tAFTER DELETE ON {1}"
            + "\n\tFOR EACH ROW EXECUTE PROCEDURE {2}()",
        name(deleteTriggerName),
        name(schemaName, ref2.getTableName()),
        name(schemaName, deleteTriggerName));
  }

  private static void createTriggerForRef(DSLContext jooq, Column column) {

    // check if any refback array has non-existing pkey
    // remove refs from other table if not any more in refback array
    // update refs from other table to new identifier ( automatic via cascade , nothing to
    // do here)
    // add refs from other table if new in refback array

    String sql =
        "CREATE FUNCTION {0}() RETURNS trigger AS $BODY$ "
            + "\nDECLARE error_row RECORD;"
            + "\nBEGIN"
            + "\n\t-- raise error for first refColumn value that does not in refTable key values "
            + "\n\tFOR error_row IN SELECT * FROM UNNEST({1}) as u({2}) EXCEPT (SELECT {2} FROM {3}) LOOP"
            + "\n\t\tRAISE EXCEPTION USING ERRCODE='23503', "
            + "\n\t\tMESSAGE = 'update or delete on table '||{4}||' violates foreign key constraint',"
            + "\n\t\tDETAIL = 'Key ('||{5}||')=('|| {6} ||') is not present in table '||{7}||', column '||{8};"
            + "\n\tEND LOOP;"
            + "\n\tIF TG_OP='UPDATE' THEN"
            + "\n\t-- remove ref to 'old'.key if not anymore in refarray"
            + "\n\t\tUPDATE {3} set {9} WHERE {10};"
            + "\n\tEND IF;"
            + "\n\tIF TG_OP='UPDATE' OR NOT EXISTS (SELECT * FROM {13} WHERE {14}) THEN"
            + "\n\t-- set to ref to 'new'.key if in refbackvalues list"
            + "\n\t\tUPDATE {3} set {11} WHERE ({2}) IN (SELECT * FROM UNNEST({1}) as u({2}));"
            + "\n\t\t-- set new refback to NULL so it doesn't get stored"
            + "\n\t\t{12};"
            + "\n\tEND IF;"
            + "\n\tRETURN NEW;"
            + "\nEND; $BODY$ LANGUAGE plpgsql;";

    String schemaName = column.getTable().getSchema().getName();
    String insertOrUpdateTrigger = refbackUpdateTriggerName(column);

    List<Reference> columns = column.getReferences();

    jooq.execute(
        sql,
        // 0 function name
        name(schemaName, insertOrUpdateTrigger),
        // 1 NEW.refback column(s) names
        keyword(
            columns.stream().map(r -> "NEW." + name(r.getName())).collect(Collectors.joining(","))),
        // 2 foreign key column names refback refers to
        keyword(
            columns.stream()
                .map(r -> name(r.getRefTo()).toString())
                .collect(Collectors.joining(","))),
        // 3 refTable
        table(name(schemaName, column.getRefTableName())),
        // 4 inline string of table for debug message
        inline(column.getTable().getTableName()),
        // 5 inline columns
        keyword(
            columns.stream()
                .map(r -> inline(r.getName()).toString())
                .collect(Collectors.joining("||','||"))),
        // 6 concat of the error column values
        keyword(
            columns.stream()
                .map(r -> "COALESCE(error_row." + name(r.getRefTo()).toString() + ",'NULL')")
                .collect(Collectors.joining("||','||"))),
        // 7 inline refTable
        inline(column.getRefTable().getTableName()),
        // 8 inline toColumns
        keyword(
            columns.stream()
                .map(r -> inline(r.getRefTo()).toString())
                .collect(Collectors.joining("||','||"))),
        // 9 set mappedBy to null
        keyword(
            column.getMappedByColumn().getReferences().stream()
                .map(r -> name(r.getName()) + "=NULL")
                .collect(Collectors.joining(","))),
        // 10 where references old key and not new key
        keyword(
            column.getMappedByColumn().getReferences().stream()
                .map(r -> name(r.getName()) + "=OLD." + name(r.getRefTo()))
                .collect(Collectors.joining(" AND "))),
        // 11 set to point to this.key(s)
        keyword(
            column.getMappedByColumn().getReferences().stream()
                .map(r -> name(r.getName()) + "=NEW." + name(r.getRefTo()))
                .collect(Collectors.joining(","))),
        // 12 set NEW.refback columns to null so they don't get saved
        keyword(
            columns.stream()
                .map(r -> "NEW." + name(r.getName()) + "=NULL")
                .collect(Collectors.joining(";"))),
        // 13 this table
        table(name(schemaName, column.getTableName())),
        // 14 where this keys
        keyword(
            column.getMappedByColumn().getReferences().stream()
                .map(Reference::getRefTo)
                .map(s -> name(s) + "=NEW." + name(s))
                .collect(Collectors.joining(" AND "))));

    jooq.execute(
        "CREATE TRIGGER {0} "
            + "\n\tBEFORE INSERT OR UPDATE OF {1} ON {2}"
            + "\n\tFOR EACH ROW EXECUTE PROCEDURE {3}()",
        // 0 name of the trigger
        name(insertOrUpdateTrigger),
        // 1 the columns of the refback that should be set to trigger the trigger
        keyword(
            columns.stream()
                .map(r -> name(r.getName()).toString())
                .collect(Collectors.joining(","))),
        // name of the table
        name(schemaName, column.getTable().getTableName()),
        // reference to the trigger function
        name(schemaName, insertOrUpdateTrigger));
  }

  static void removeRefBackConstraints(DSLContext jooq, Column column) {
    jooq.execute(
        "DROP FUNCTION IF EXISTS {0} CASCADE",
        name(column.getSchemaName(), refbackDeleteTriggerName(column)));
    jooq.execute(
        "DROP FUNCTION IF EXISTS {0} CASCADE",
        name(column.getSchemaName(), refbackUpdateTriggerName(column)));
  }

  private static String refbackDeleteTriggerName(Column... column) {
    return "1" + column[0].getTable().getTableName() + "-" + getNames(column) + "_DELETE";
  }

  private static String getNames(Column... column) {
    return List.of(column).stream().map(Column::getName).collect(Collectors.joining(","));
  }

  private static String refbackUpdateTriggerName(Column... column) {
    return "1" + column[0].getTable().getTableName() + "-" + getNames(column) + "_UPDATE";
  }
}
