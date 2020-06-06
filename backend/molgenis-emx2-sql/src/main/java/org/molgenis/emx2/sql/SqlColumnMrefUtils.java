package org.molgenis.emx2.sql;

import org.jooq.DSLContext;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.TableMetadata;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.sql.SqlColumnUtils.getJoinTableName;
import static org.molgenis.emx2.sql.SqlTypeUtils.*;

class SqlColumnMrefUtils {
  private SqlColumnMrefUtils() {
    // hide
  }

  static void createMrefConstraints(DSLContext jooq, Column column) {
    // create joinTable the joinTable
    column
        .getTable()
        .getSchema()
        .create(new TableMetadata(getJoinTableName(column)))
        .add(
            new Column(column.getName())
                .type(REF)
                .refTable(column.getRefTableName())
                .refColumn(column.getRefColumnName()))
        .add(
            new Column(column.getTable().getPrimaryKey()[0])
                .type(REF)
                .refTable(column.getTable().getTableName())
                .refColumn(column.getTable().getPrimaryKey()[0]));

    // create trigger on insert, update and delete of rows in this table will update mref table
    createTriggers(jooq, column, getJoinTableName(column));
  }

  private static void createTriggers(DSLContext jooq, Column thisColumn, String joinTableName) {
    //  parameters
    String schemaName = thisColumn.getTable().getSchema().getName();
    String insertOrUpdateTrigger =
        thisColumn.getTable().getTableName() + "_" + thisColumn.getName() + "_UPSERT_TRIGGER";
    TableMetadata thisTable = thisColumn.getTable();
    String[] primaryKey = thisTable.getPrimaryKey();
    String sqlType = getPsqlType(thisColumn.getRefColumn());

    // insert and update trigger: does the following
    // first delete mrefs to previous instance of 'self'
    // and then update with current set of refColumn, reverseRefColumn pairs
    jooq.execute(
        "CREATE FUNCTION {0}() RETURNS trigger AS"
            + "\n$BODY$"
            + "\nDECLARE"
            + "\n\t item {1};"
            + "\nBEGIN"
            // DELETE FROM jointable WHERE reverseRefColumn = reverseRefColumn (i.e. primary key)
            + "\n\tIF TG_OP='UPDATE' THEN DELETE FROM {2} WHERE {3} = OLD.{3}; END IF;"
            // foreach new.refColumn
            // check if we can expect 'update following on conflict insert'
            + "\n\tFOREACH item IN ARRAY NEW.{4} LOOP"
            // INSERT INTO jointable(refColumn,getReverseRefColumn) VALUES (item, NEW.refColumn)
            + "\n\t\tINSERT INTO {2} ({3},{4}) VALUES (NEW.{3},item);"
            + "\n\tEND LOOP;"
            // NEW.column = NULL, unless this is INSERT and we can expect ON CONFLICT
            + "\n\tIF TG_OP='UPDATE' OR NOT EXISTS (SELECT 1 FROM {5} WHERE {3} = NEW.{3}) THEN NEW.{4} = NULL; END IF;"
            + "\n\tRETURN NEW;"
            + "\nEND;"
            + "\n$BODY$ LANGUAGE plpgsql;",
        name(schemaName, insertOrUpdateTrigger), // {0} trigger name
        keyword(sqlType), // {1} type of the target of the mref
        table(name(schemaName, joinTableName)), // {2} the join table
        field(name(primaryKey)), // {3} column from other table that points to 'me'
        field(name(thisColumn.getName())), // {4} the new mref column we are creating
        table(name(schemaName, thisTable.getTableName()))); // {5} the table the trigger is on

    jooq.execute(
        "CREATE TRIGGER {0} "
            + "\n\tBEFORE INSERT OR UPDATE OF {1} ON {2}"
            + "\n\tFOR EACH ROW EXECUTE PROCEDURE {3}()",
        name(insertOrUpdateTrigger),
        name(thisColumn.getName()),
        name(schemaName, thisColumn.getTable().getTableName()),
        name(schemaName, insertOrUpdateTrigger));

    // delete trigger: will delete all mrefs that involve 'self' before deleting 'self'
    String deleteTrigger =
        thisColumn.getTable().getTableName() + "_" + thisColumn.getName() + "_DELETE_TRIGGER";
    jooq.execute(
        "CREATE FUNCTION {0}() RETURNS trigger AS"
            + "\n$BODY$"
            + "\nBEGIN"
            // DELETE FROM jointable WHERE getReverseRefColumn = reverseRefColumn
            + "\n\tDELETE FROM {1} WHERE {2} = OLD.{2};"
            + "\n\tRETURN OLD;"
            + "\nEND;"
            + "\n$BODY$ LANGUAGE plpgsql;",
        name(schemaName, deleteTrigger),
        table(name(schemaName, joinTableName)),
        field(name(primaryKey)));
    jooq.execute(
        "CREATE TRIGGER {0} "
            + "\n\tAFTER DELETE ON {1}"
            + "\n\tFOR EACH ROW EXECUTE PROCEDURE {2}()",
        name(deleteTrigger),
        name(schemaName, thisColumn.getTable().getTableName()),
        name(schemaName, deleteTrigger));
  }
}
