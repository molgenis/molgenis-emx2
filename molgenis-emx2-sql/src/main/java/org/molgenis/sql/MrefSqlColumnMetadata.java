package org.molgenis.sql;

import org.jooq.DSLContext;
import org.molgenis.MolgenisException;
import org.molgenis.metadata.ColumnMetadata;
import org.molgenis.metadata.TableMetadata;

import static org.jooq.impl.DSL.*;
import static org.molgenis.data.Row.MOLGENISID;
import static org.molgenis.metadata.Type.MREF;
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
public class MrefSqlColumnMetadata extends SqlColumnMetadata {

  protected MrefSqlColumnMetadata(
      SqlTableMetadata sqlTable,
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
  }

  @Override
  public MrefSqlColumnMetadata createColumn() throws MolgenisException {
    String schemaName = getTable().getSchema().getName();

    // create setNullable array columns compatible with the refs
    SqlTableMetadata otherTable =
        (SqlTableMetadata) getTable().getSchema().getTableMetadata(getRefTableName());
    ColumnMetadata otherColumn = otherTable.getColumn(getRefColumnName());

    getJooq()
        .alterTable(name(schemaName, getTable().getTableName()))
        .add(field(name(getColumnName()), SqlTypeUtils.jooqTypeOf(otherColumn).getArrayDataType()))
        .execute();
    getJooq()
        .alterTable(name(schemaName, otherTable.getTableName()))
        .add(
            field(
                name(getReverseColumnName()),
                SqlTypeUtils.jooqTypeOf(otherColumn).getArrayDataType()))
        .execute();

    // create the joinTable
    TableMetadata table = getTable().getSchema().createTableIfNotExists(getMrefJoinTableName());
    table.addRef(getRefColumnName(), getRefTableName(), getRefColumnName());
    table.addRef(getReverseRefColumn(), getTable().getTableName(), getReverseRefColumn());

    // add the reverse column to the other table
    MrefSqlColumnMetadata reverseColumn =
        new MrefSqlColumnMetadata(
            otherTable,
            getReverseColumnName(),
            getTable().getTableName(),
            getReverseRefColumn(),
            getColumnName(),
            getRefColumnName(),
            getMrefJoinTableName());
    otherTable.addMrefReverse(reverseColumn);

    // create triggers both ways
    createTriggers(getJooq(), table, this, reverseColumn);
    createTriggers(getJooq(), table, reverseColumn, this);

    // save metadata
    saveColumnMetadata(this);
    saveColumnMetadata(reverseColumn);

    return this;
  }

  private static void createTriggers(
      DSLContext jooq, TableMetadata joinTable, ColumnMetadata column, ColumnMetadata reverseColumn)
      throws MolgenisException {

    //  parameters
    String schemaName = column.getTable().getSchema().getName();
    String insertOrUpdateTrigger =
        column.getTable().getTableName() + "_" + column.getColumnName() + "_UPSERT_TRIGGER";
    ColumnMetadata targetColumn = reverseColumn.getTable().getColumn(column.getRefColumnName());

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
        name(column.getTable().getSchema().getName(), insertOrUpdateTrigger),
        keyword(SqlTypeUtils.getPsqlType(targetColumn)),
        table(name(joinTable.getSchema().getName(), joinTable.getTableName())),
        field(name(reverseColumn.getRefColumnName())),
        field(name(column.getColumnName())),
        name(MOLGENISID),
        field(name(column.getRefColumnName())));

    jooq.execute(
        "CREATE TRIGGER {0} "
            + "\n\tBEFORE INSERT ON {1}"
            + "\n\tFOR EACH ROW EXECUTE PROCEDURE {2}()",
        name(insertOrUpdateTrigger),
        name(schemaName, column.getTable().getTableName()),
        name(schemaName, insertOrUpdateTrigger));

    // delete trigger: will delete all mrefs that involve 'self' before deleting 'self'
    String deleteTrigger =
        column.getTable().getTableName() + "_" + column.getColumnName() + "_DELETE_TRIGGER";
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
        name(schemaName, column.getTable().getTableName()),
        name(schemaName, deleteTrigger));
  }
}
