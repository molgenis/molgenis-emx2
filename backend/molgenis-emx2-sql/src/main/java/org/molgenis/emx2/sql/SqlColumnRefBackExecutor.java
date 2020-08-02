package org.molgenis.emx2.sql;

import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.molgenis.emx2.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.sql.SqlColumnRefExecutor.validateRef;

class SqlColumnRefBackExecutor {
  private SqlColumnRefBackExecutor() {
    // hide
  }

  // will create a dummy array column matching the toColumn we will link to
  // will create a before insert trigger to update all REF instances in the other table that needs
  // updating
  static void createRefBackColumnConstraints(DSLContext jooq, Column column) {

    try {
      // get ref table
      validateRef(column);

      // get the other column
      TableMetadata toTable = column.getRefTable();

      // get the via column which is also in the 'toTable'
      String mappedByColumnName = column.getMappedBy();
      if (mappedByColumnName == null) {
        throw new MolgenisException(
            "Create column failed",
            "Create of REFBACK column '"
                + column.getName()
                + "' failed because mappedBy was not set.");
      }

      Column mappedByColumn = column.getMappedByColumn();
      if (mappedByColumn == null) {
        throw new MolgenisException(
            "Create column failed",
            "Could not create column '"
                + column.getName()
                + "' because could not find mappedBy column '"
                + mappedByColumnName
                + "' in refTable '"
                + column.getRefTableName()
                + "'");
      }

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

      // create the trigger so that insert/update/delete on REFBACK column updates the
      // relationship
      ColumnType mappedByType = mappedByColumn.getColumnType();
      switch (mappedByType) {
        case REF:
          createTriggerForRef(jooq, column);
          break;
        case REF_ARRAY:
          createTriggerForRefArray(jooq, column);
          break;
          // todo case MREF:
        default:
          throw new MolgenisException(
              "Create column failed",
              "Create of REFBACK column '"
                  + column.getName()
                  + "' failed because mappedBy was not of type REF, REF_ARRAY");
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
    String updateTriggerName = refbackUpdateTriggerName(column);

    // ref
    String ref =
        column.getRefColumns().stream()
            .map(r -> name(r.getName()).toString())
            .collect(Collectors.joining(","));
    String newRef =
        column.getRefColumns().stream()
            .map(r -> "NEW." + name(r.getName()))
            .collect(Collectors.joining(","));
    String refTo =
        column.getRefColumns().stream()
            .map(r -> name(r.getTo()).toString())
            .collect(Collectors.joining(","));
    String oldRefTo =
        column.getRefColumns().stream()
            .map(r -> "OLD." + name(r.getTo()))
            .collect(Collectors.joining(","));
    String errorColumns =
        column.getRefColumns().stream()
            .map(r -> "error_row." + name(r.getTo()).toString())
            .collect(Collectors.joining("||','||"));

    Column mappedBy = column.getMappedByColumn();

    // mappedBy.to
    String mappedByTo =
        mappedBy.getRefColumns().stream()
            .map(r -> name(r.getTo()).toString())
            .collect(Collectors.joining(","));
    String oldMappedByTo =
        mappedBy.getRefColumns().stream()
            .map(r -> "OLD." + name(r.getTo()))
            .collect(Collectors.joining(","));
    String newMappedByTo =
        mappedBy.getRefColumns().stream()
            .map(r -> "NEW." + name(r.getTo())) // + " AS " + name(r.getTo()))
            .collect(Collectors.joining(","));
    String mappedByToEqualsNewKey =
        mappedBy.getRefColumns().stream()
            .map(r -> name(r.getTo()) + "=NEW." + name(r.getTo()))
            .collect(Collectors.joining(" AND "));
    String mappedByToEqualsOldKey =
        mappedBy.getRefColumns().stream()
            .map(r -> name(r.getTo()) + "=OLD." + name(r.getTo()))
            .collect(Collectors.joining(" AND "));

    String updateFilter =
        column.getRefColumns().stream()
            .map(r -> "t." + name(r.getTo()) + "=error_row." + name(r.getTo()))
            .collect(Collectors.joining(" AND "));

    String mappedByFrom =
        mappedBy.getRefColumns().stream()
            .map(r -> name(r.getName()).toString())
            .collect(Collectors.joining(","));

    String mappedByFromErrorRow =
        mappedBy.getRefColumns().stream()
            .map(r -> "error_row." + name(r.getName()).toString())
            .collect(Collectors.joining(","));
    String setterUpdate =
        mappedBy.getRefColumns().stream()
            .map(r -> name(r.getName()) + "=t3." + name(r.getName()))
            .collect(Collectors.joining(","));
    String arrayAgg =
        mappedBy.getRefColumns().stream()
            .map(r -> "array_agg(" + name(r.getTo()) + ") as " + name(r.getName()))
            .collect(Collectors.joining(","));
    String setRefToNull =
        column.getRefColumns().stream()
            .map(r -> "NEW." + name(r.getName()) + "=NULL")
            .collect(Collectors.joining(";"));

    jooq.execute(
        "CREATE FUNCTION {0}() RETURNS trigger AS"
            + "\n$BODY$"
            + "\nDECLARE error_row RECORD;"
            + "\nBEGIN"
            // for first refColumn value that does not in refTable key values raise error
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

        // {0} function name
        name(schemaName, updateTriggerName),
        // {1} to replace old mappedBy array(s) with new array(s)
        keyword(setterUpdate),
        // {2} refTable
        table(name(schemaName, column.getRefTableName())),
        // {3} mappedBy
        keyword(mappedByFrom),
        // {4} key that mappedBy uses (might not be pkey)
        keyword(oldMappedByTo),
        // {5} the column, in case of composite decomposed
        keyword(newRef),
        // {6} toColumn where fake foreign key dummy points to
        keyword(refTo),
        // {7} this table
        table(name(schemaName, column.getTable().getTableName())),
        // {8} check if pkey in new.pkey (i.e. if insert or update)
        keyword(mappedByToEqualsNewKey),
        // {9} inline table name
        inline(column.getTable().getTableName()),
        // {10} name
        inline(column.getName()),
        // {11} inline refTable name
        inline(column.getRefTableName()),
        // {12} inline refTable key column names
        inline(refTo),
        // {13} columns concat for errors
        keyword(errorColumns),
        // {14} new key
        keyword(newMappedByTo),
        // {15} set new.ref to null
        keyword(setRefToNull),
        // {16} keyEqualsOldKey
        keyword(mappedByToEqualsOldKey),
        // {17}
        keyword(arrayAgg),
        // {18}
        keyword(updateFilter),
        // {19}
        keyword(refTo),
        // {20}
        keyword(mappedByFromErrorRow),
        // {21}
        keyword(mappedByTo),
        // {22}
        keyword(oldRefTo));

    // attach the trigger

    jooq.execute(
        "CREATE TRIGGER {0} "
            + "\n\tBEFORE INSERT OR UPDATE OF {1} ON {2}"
            + "\n\tFOR EACH ROW EXECUTE PROCEDURE {3}()",
        name(column.getName() + "_UPDATE"),
        keyword(ref),
        name(schemaName, column.getTable().getTableName()),
        name(schemaName, updateTriggerName));

    // delete and truncate trigger

    String deleteTriggerName = refbackDeleteTriggerName(column);

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
                column
                    .getMappedByColumn()
                    .getRefTable()
                    .getPrimaryKeys()
                    .get(0)))); // {3} key that mappedBy uses (might not be pkey)

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

    // create parameters
    String insertOrUpdateTrigger = refbackUpdateTriggerName(column);
    // String deleteTrigger = refbackDeleteTriggerName(column);

    // create composite key statements
    List<String> mappedBySetNullList = new ArrayList<>();
    List<String> mappedByIsOldKeyList = new ArrayList<>();
    List<String> mappedBySetNewKeyList = new ArrayList<>();
    List<String> keyIsNewKeyList = new ArrayList<>();
    List<String> mappedByToList = new ArrayList<>();
    List<String> refBackColumnList = new ArrayList<>();
    List<String> setRefBackValuesToNullList = new ArrayList<>();
    for (Reference ref : column.getRefColumns()) {
      mappedByToList.add(name(ref.getTo()).toString());
      refBackColumnList.add("NEW." + name(ref.getName()));
      setRefBackValuesToNullList.add("NEW." + name(ref.getName()) + "=NULL");
    }
    for (Reference ref : column.getMappedByColumn().getRefColumns()) {
      keyIsNewKeyList.add(name(ref.getTo()) + "=NEW." + name(ref.getTo()));
      mappedBySetNullList.add(name(ref.getName()) + "=NULL");
      mappedByIsOldKeyList.add(name(ref.getName()) + "=OLD." + name(ref.getTo()));
      mappedBySetNewKeyList.add(name(ref.getName()) + "=NEW." + name(ref.getTo()));
    }
    String mappedBySetNull = String.join(",", mappedBySetNullList);
    String mappedByIsOldKey = String.join(" AND ", mappedByIsOldKeyList);
    String keyIsNewKey = String.join(" AND ", keyIsNewKeyList);
    String mappedBySetNewKey = String.join(",", mappedBySetNewKeyList);
    String mappedByTo = String.join(",", mappedByToList);
    String refBackColumn = String.join(",", refBackColumnList);
    String setRefBackValuesToNull = String.join(";", setRefBackValuesToNullList);

    // insert and update trigger
    jooq.execute(
        "CREATE FUNCTION {0}() RETURNS trigger AS $BODY$ BEGIN"
            + "\n\tIF TG_OP='UPDATE' THEN"
            // remove reference to old key
            + "\n\t\tUPDATE {1} set {3} WHERE {4};"
            + "\n\tEND IF;"
            + "\n\tIF TG_OP='UPDATE' OR NOT EXISTS (SELECT 1 FROM {2} WHERE {5}) THEN"
            // update to new key, if in refbackvalues list
            + "\n\t\tUPDATE {1} set {6} WHERE ({7}) IN (SELECT UNNEST({8}));"
            + "\n\t\t{9};"
            + "\n\tEND IF;"
            + "\n\tRETURN NEW;"
            + "\nEND; $BODY$ LANGUAGE plpgsql;",
        name(schemaName, insertOrUpdateTrigger), // {0} function name
        table(name(schemaName, column.getRefTableName())), // {1} toTable table
        table(name(schemaName, column.getTable().getTableName())), // {2} this table
        keyword(mappedBySetNull), // {3}
        keyword(mappedByIsOldKey), // 4
        keyword(keyIsNewKey), // 5
        keyword(mappedBySetNewKey), // 6
        keyword(mappedByTo), // 7
        keyword(refBackColumn), // 8
        keyword(setRefBackValuesToNull)); // 9

    jooq.execute(
        "CREATE TRIGGER {0} "
            + "\n\tBEFORE INSERT OR UPDATE OF {1} ON {2}"
            + "\n\tFOR EACH ROW EXECUTE PROCEDURE {3}()",
        name(insertOrUpdateTrigger),
        name(column.getName()),
        name(schemaName, column.getTable().getTableName()),
        name(schemaName, insertOrUpdateTrigger));

    // delete trigger
    //    jooq.execute(
    //        "CREATE FUNCTION {0}() RETURNS trigger AS $BODY$ BEGIN"
    //            + "\nBEGIN"
    //            + "\n\tUPDATE {2} set {3} = NULL WHERE {3} = OLD.{4};"
    //            + "\n\tRETURN NEW;"
    //            + "\nEND; $BODY$ LANGUAGE plpgsql;",
    //        name(schemaName, deleteTrigger), // {0} function name
    //        keyword(SqlTypeUtils.getPsqlType(column.getRefColumn())), // {1} type of item
    //        table(name(schemaName, column.getRefTableName())), // {2} toTable table
    //        field(name(column.getMappedBy())), // {3} mappedBy field
    //        field(
    //            name(
    //                getMappedByColumn(column)
    //                    .getRefColumn()
    //                    .getName()))); // {4} mappedBy reference to 'me' (might not be pkey)

    //    jooq.execute(
    //        "CREATE TRIGGER {0} "
    //            + "\n\tAFTER DELETE ON {1} "
    //            + "\n\tFOR EACH ROW EXECUTE PROCEDURE {2}()",
    //        name(deleteTrigger),
    //        name(schemaName, column.getTable().getTableName()),
    //        name(schemaName, deleteTrigger));
  }

  static void removeRefBackConstraints(DSLContext jooq, Column column) {
    jooq.execute(
        "DROP FUNCTION IF EXISTS {0} CASCADE",
        name(column.getSchemaName(), refbackDeleteTriggerName(column)));
    jooq.execute(
        "DROP FUNCTION IF EXISTS {0} CASCADE",
        name(column.getSchemaName(), refbackUpdateTriggerName(column)));
  }

  private static String refbackDeleteTriggerName(Column column) {
    return "1" + column.getTable().getTableName() + "-" + column.getName() + "_DELETE";
  }

  private static String refbackUpdateTriggerName(Column column) {
    return "1" + column.getTable().getTableName() + "-" + column.getName() + "_UPDATE";
  }
}
