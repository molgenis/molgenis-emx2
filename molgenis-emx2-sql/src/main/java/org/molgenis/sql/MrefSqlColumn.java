package org.molgenis.sql;

import org.jooq.DSLContext;
import org.molgenis.Column;
import org.molgenis.MolgenisException;
import org.molgenis.Table;

import static org.jooq.impl.DSL.*;
import static org.molgenis.Row.MOLGENISID;
import static org.molgenis.Type.MREF;
import static org.molgenis.sql.MetadataUtils.saveColumnMetadata;

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
public class MrefSqlColumn extends SqlColumn {
  private DSLContext jooq;

  protected MrefSqlColumn(
      SqlTable sqlTable,
      String name,
      String refTable,
      String refColumn,
      String reverseName,
      String reverseRefColumn,
      String joinTableName) {
    super(sqlTable, name, MREF);
    this.setReference(refTable, refColumn);
    this.setReverseReference(reverseName, reverseRefColumn);
    this.setJoinTable(joinTableName);
    this.jooq = sqlTable.getJooq();
  }

  @Override
  public MrefSqlColumn createColumn() throws MolgenisException {

    // create setNullable array columns compatible with the refs
    SqlTable otherTable = (SqlTable) getTable().getSchema().getTable(getRefTableName());
    Column otherColumn = otherTable.getColumn(getRefColumnName());

    jooq.alterTable(name(getTable().getSchemaName(), getTable().getName()))
        .add(field(name(getName()), SqlTypeUtils.jooqTypeOf(otherColumn).getArrayDataType()))
        .execute();
    jooq.alterTable(name(getTable().getSchemaName(), otherTable.getName()))
        .add(
            field(
                name(getReverseColumnName()),
                SqlTypeUtils.jooqTypeOf(otherColumn).getArrayDataType()))
        .execute();

    // create the joinTable
    Table table = getTable().getSchema().createTableIfNotExists(getMrefJoinTableName());
    table.addRef(getRefColumnName(), getRefTableName(), getRefColumnName());
    table.addRef(getReverseRefColumn(), getTable().getName(), getReverseRefColumn());

    // add the reverse column to the other table
    MrefSqlColumn reverseColumn =
        new MrefSqlColumn(
            otherTable,
            getReverseColumnName(),
            getTable().getName(),
            getReverseRefColumn(),
            getName(),
            getRefColumnName(),
            getMrefJoinTableName());
    otherTable.addMrefReverse(reverseColumn);

    // create triggers both ways
    createTriggers(jooq, table, this, reverseColumn);
    createTriggers(jooq, table, reverseColumn, this);

    // save metadata
    saveColumnMetadata(this);
    saveColumnMetadata(reverseColumn);

    return this;
  }

  private static void createTriggers(
      DSLContext jooq, Table joinTable, Column column, Column reverseColumn)
      throws MolgenisException {

    //  parameters
    String insertOrUpdateTrigger =
        column.getTable().getName() + "_" + column.getName() + "_UPSERT_TRIGGER";
    Column targetColumn = reverseColumn.getTable().getColumn(column.getRefColumnName());

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
            + "\n\tDELETE FROM {2} WHERE {3} = OLD.{3};"
            // foreach new.refColumn
            + "\n\tFOREACH item IN ARRAY NEW.{4}"
            + "\n\tLOOP"
            // INSERT INTO jointable(refColumn,getReverseRefColumn) VALUES (item, NEW.refColumn)
            + "\n\t\tINSERT INTO {2} ({5},{3},{6}) VALUES (md5(random()::text || clock_timestamp()::text)::uuid, NEW.{3},item);"
            + "\n\tEND LOOP;"
            // NEW.column = NULL
            + "\n\tNEW.{4} = NULL;"
            + "\n\tRETURN NEW;"
            + "\nEND;"
            + "\n$BODY$ LANGUAGE plpgsql;",
        name(column.getTable().getSchemaName(), insertOrUpdateTrigger),
        keyword(SqlTypeUtils.getPsqlType(targetColumn)),
        table(name(joinTable.getSchema().getName(), joinTable.getName())),
        field(name(reverseColumn.getRefColumnName())),
        field(name(column.getName())),
        name(MOLGENISID),
        field(name(column.getRefColumnName())));

    jooq.execute(
        "CREATE TRIGGER {0} "
            + "\n\tBEFORE INSERT ON {1}"
            + "\n\tFOR EACH ROW EXECUTE PROCEDURE {2}()",
        name(insertOrUpdateTrigger),
        name(column.getTable().getSchemaName(), column.getTable().getName()),
        name(column.getTable().getSchemaName(), insertOrUpdateTrigger));

    // delete trigger: will delete all mrefs that involve 'self' before deleting 'self'
    String deleteTrigger = column.getTable().getName() + "_" + column.getName() + "_DELETE_TRIGGER";
    jooq.execute(
        "CREATE FUNCTION {0}() RETURNS trigger AS"
            + "\n$BODY$"
            + "\nBEGIN"
            // DELETE FROM jointable WHERE getReverseRefColumn = reverseRefColumn
            + "\n\tDELETE FROM {1} WHERE {2} = OLD.{2};"
            + "\n\tRETURN OLD;"
            + "\nEND;"
            + "\n$BODY$ LANGUAGE plpgsql;",
        name(column.getTable().getSchemaName(), deleteTrigger),
        table(name(joinTable.getSchema().getName(), joinTable.getName())),
        field(name(reverseColumn.getRefColumnName())));
    jooq.execute(
        "CREATE TRIGGER {0} "
            + "\n\tAFTER DELETE ON {1}"
            + "\n\tFOR EACH ROW EXECUTE PROCEDURE {2}()",
        name(deleteTrigger),
        name(column.getTable().getSchemaName(), column.getTable().getName()),
        name(column.getTable().getSchemaName(), deleteTrigger));
  }
}
