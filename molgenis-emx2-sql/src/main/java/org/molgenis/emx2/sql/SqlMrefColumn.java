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
      SqlTableMetadata sqlTable, String name, String refTable, String refColumn) {
    super(sqlTable, name, MREF);
    this.setReference(refTable, refColumn);
  }

  @Override
  public SqlMrefColumn createColumn() {
    String schemaName = getTable().getSchema().getName();

    // create the column in this table
    getJooq()
        .alterTable(name(schemaName, getTable().getTableName()))
        .add(field(name(getName()), SqlTypeUtils.jooqTypeOf(getRefColumn()).getArrayDataType()))
        .execute();

    // create joinTable the joinTable
    TableMetadata table = getTable().getSchema().createTable(getJoinTableName());
    // to points to other table
    table.addRef(getName(), getRefTableName(), getRefColumnName());
    // reverse points to primary key of this table
    Column reverseColumn = getTable().getPrimaryKeyColumn();
    table.addRef(reverseColumn.getName(), getTable().getTableName(), reverseColumn.getName());

    // create trigger on insert, update and delete of rows in this table will update mref table
    createTriggers(getJooq(), this, getJoinTableName());

    // save metadata
    saveColumnMetadata(this);
    return this;
  }

  private static void createTriggers(DSLContext jooq, Column thisColumn, String joinTableName) {

    //  parameters
    String schemaName = thisColumn.getTable().getSchema().getName();
    String insertOrUpdateTrigger =
        thisColumn.getTable().getTableName() + "_" + thisColumn.getName() + "_UPSERT_TRIGGER";
    TableMetadata thisTable = thisColumn.getTable();
    String primaryKey = thisTable.getPrimaryKey();
    String sqlType = SqlTypeUtils.getPsqlType(thisColumn.getRefColumn());

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
