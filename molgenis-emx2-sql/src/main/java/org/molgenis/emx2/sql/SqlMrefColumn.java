package org.molgenis.emx2.sql;

import org.jooq.DSLContext;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.TableMetadata;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.ColumnType.MREF;
import static org.molgenis.emx2.sql.MetadataUtils.saveColumnMetadata;

/**
 * Creates a join table.
 *
 * <p>Also refTable.refColumn[] and refBackTable.refBackColumn[] columns are created
 *
 * <p>Finally, triggers take inserts/updates on refColumn[] or refBackColumn[] and put content in
 * joinTable
 *
 * <p>Note: we replace all relevant values in joinTable to allow also for changing of the refered to
 * column. This might get slow on large MREF lists (to be measured).
 */
public class SqlMrefColumn extends SqlColumn {

  protected SqlMrefColumn(
      SqlTableMetadata sqlTable,
      String name,
      String refTable,
      String refColumn,
      String joinTableName) {
    super(sqlTable, name, MREF);
    this.setReference(refTable, refColumn);
    this.setJoinVia(joinTableName);
  }

  @Override
  public SqlMrefColumn createColumn() {
    String schemaName = getTable().getSchema().getName();

    String thisTable = getTable().getTableName();
    String thisColumn = getName();

    String toTable = getRefTableName();
    String toColumn = getRefColumnName();

    String reverseColumn = getTable().getPrimaryKey();

    String joinTable = getJoinViaName();

    // create new refArray column to enable updates to be provided for the trigger
    getJooq()
        .alterTable(name(schemaName, thisTable))
        .add(field(name(thisColumn), SqlTypeUtils.jooqTypeOf(getRefColumn()).getArrayDataType()))
        .execute();

    // create joinTable the joinTable
    TableMetadata table = getTable().getSchema().createTable(joinTable);
    Column to = table.addRef(thisColumn, toTable, toColumn);
    Column reverse = table.addRef(reverseColumn, thisTable, reverseColumn);

    // create trigger on insert, update and delete of rows in this table will update mref table
    createTriggers(getJooq(), this, table, reverse);

    // save metadata
    saveColumnMetadata(this);
    return this;
  }

  private static void createTriggers(
      DSLContext jooq, Column thisColumn, TableMetadata joinTable, Column reverseColumn) {

    //  parameters
    String schemaName = thisColumn.getTable().getSchema().getName();
    String insertOrUpdateTrigger =
        thisColumn.getTable().getTableName() + "_" + thisColumn.getName() + "_UPSERT_TRIGGER";
    TableMetadata thisTable = thisColumn.getTable();
    String primaryKey = thisTable.getPrimaryKey();

    // insert and update trigger: does the following
    // first delete mrefs to previous instance of 'self'
    // and then update with current set of refColumn, reverseRefColumn pairs
    jooq.execute(
        "CREATE FUNCTION {0}() RETURNS trigger AS"
            + "\n$BODY$"
            + "\nDECLARE"
            + "\n\t item {1};"
            + "\nBEGIN"
            // DELETE FROM jointable WHERE reverseRefColumn = reverseRefColumn
            + "\n\tIF TG_OP='UPDATE' THEN DELETE FROM {2} WHERE {3} = OLD.{4}; END IF;"
            // foreach new.refColumn
            // check if we can expect 'update following on conflict insert'
            + "\n\tFOREACH item IN ARRAY NEW.{5} LOOP"
            // INSERT INTO jointable(refColumn,getReverseRefColumn) VALUES (item, NEW.refColumn)
            + "\n\t\tINSERT INTO {2} ({3},{5}) VALUES (NEW.{4},item);"
            + "\n\tEND LOOP;"
            // NEW.column = NULL, unless this is INSERT and we can expect ON CONFLICT
            + "\n\tIF TG_OP='UPDATE' OR NOT EXISTS (SELECT 1 FROM {6} WHERE {7} = NEW.{7}) THEN NEW.{5} = NULL; END IF;"
            + "\n\tRETURN NEW;"
            + "\nEND;"
            + "\n$BODY$ LANGUAGE plpgsql;",
        name(schemaName, insertOrUpdateTrigger), // {0} trigger name
        keyword(
            SqlTypeUtils.getPsqlType(
                thisColumn.getRefColumn())), // {1} type of the primary key of this
        table(
            name(joinTable.getSchema().getName(), joinTable.getTableName())), // {2} the join table
        field(name(reverseColumn.getName())), // {3} column from other table that points to 'me'
        field(name(reverseColumn.getRefColumnName())), // {4} key in 'me' that it pionts to
        field(name(thisColumn.getName())), // {5} the mref column we are creating
        table(name(schemaName, thisTable.getTableName())), // {6} the table the trigger is on
        field(name(primaryKey))); // {7} the primary key of {6}

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
        table(name(joinTable.getSchema().getName(), joinTable.getTableName())),
        field(name(reverseColumn.getRefColumnName())));
    jooq.execute(
        "CREATE TRIGGER {0} "
            + "\n\tAFTER DELETE ON {1}"
            + "\n\tFOR EACH ROW EXECUTE PROCEDURE {2}()",
        name(deleteTrigger),
        name(schemaName, thisColumn.getTable().getTableName()),
        name(schemaName, deleteTrigger));
  }
}
