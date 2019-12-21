package org.molgenis.emx2.sql;

import org.jooq.Name;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.ColumnType.REF_ARRAY;
import static org.molgenis.emx2.sql.MetadataUtils.saveColumnMetadata;

public class SqlRefArrayColumn extends SqlColumn {

  public SqlRefArrayColumn(
      SqlTableMetadata table, String columnName, String toTable, String toColumn) {
    super(table, columnName, REF_ARRAY);
    this.setReference(toTable, toColumn);
  }

  @Override
  public SqlRefArrayColumn createColumn() {
    super.createColumn();
    this.createReferenceExistsTrigger();
    this.createIsReferencedByTrigger();
    saveColumnMetadata(this);
    return this;
  }

  // this trigger is to check for foreign violations: to prevent that referenced records cannot be
  // changed/deleted in such a way that we get dangling foreign key references.
  private void createIsReferencedByTrigger() {
    Name toTable = name(getTable().getSchema().getName(), getRefTableName());
    Name thisTable = name(getTable().getSchema().getName(), getTable().getTableName());
    Name thisColumn = name(getName());
    Name toColumn = name(getRefColumnName());

    Name updateTrigger =
        name(
            getTable().getSchema().getName(),
            getRefTableName() + "_" + getRefColumnName() + "_UPDTRIGGER");

    Name deleteTrigger =
        name(
            getTable().getSchema().getName(),
            getRefTableName() + "_" + getRefColumnName() + "_DELTRIGGER");

    // in case of update of other end cascade
    getJooq()
        .execute(
            "CREATE FUNCTION {0}() RETURNS trigger AS"
                + "\n$BODY$"
                + "\n\tBEGIN"
                + "\n\tUPDATE {1} SET {3}=ARRAY_REPLACE({3}, OLD.{2}, NEW.{2}) WHERE OLD.{2} != NEW.{2} AND OLD.{2} = ANY ({3});"
                + "\n\tRETURN NEW;"
                + "\nEND;"
                + "\n$BODY$ LANGUAGE plpgsql;",
            updateTrigger, thisTable, toColumn, thisColumn);

    // createTableIfNotExists the trigger
    getJooq()
        .execute(
            "CREATE CONSTRAINT TRIGGER {0} "
                + "\n\tAFTER UPDATE OF {1} ON {2} "
                + "\n\tDEFERRABLE INITIALLY IMMEDIATE "
                + "\n\tFOR EACH ROW EXECUTE PROCEDURE {3}()",
            name(getRefColumnName() + "_UPDATE"), toColumn, toTable, updateTrigger);

    // in case of delete should fail if still pointed to
    getJooq()
        .execute(
            "CREATE FUNCTION {0}() RETURNS trigger AS"
                + "\n$BODY$"
                + "\n\tBEGIN"
                + "\n\tIF(EXISTS(SELECT * from {1} WHERE OLD.{2} = ANY({3}) ) ) THEN "
                + "RAISE EXCEPTION USING ERRCODE='23503', MESSAGE = 'update or delete on table {4} violates foreign key constraint on table {5}'"
                + " , DETAIL = 'Key ({6})=('|| OLD.{2} ||') is still referenced from table {5}';"
                + "\n\tEND IF;"
                + "\n\tRETURN NEW;"
                + "\nEND;"
                + "\n$BODY$ LANGUAGE plpgsql;",
            deleteTrigger,
            thisTable,
            toColumn,
            thisColumn,
            inline(getRefTableName()),
            inline(getTableName()),
            inline(getRefColumnName()));

    getJooq()
        .execute(
            "CREATE CONSTRAINT TRIGGER {0} "
                + "\n\tAFTER DELETE ON {1} "
                + "\n\tDEFERRABLE INITIALLY IMMEDIATE "
                + "\n\tFOR EACH ROW EXECUTE PROCEDURE {2}()",
            name(getRefColumnName() + "_DELETE"), toTable, updateTrigger);
  }

  /** trigger on this column to check if foreign key exists */
  private void createReferenceExistsTrigger() {
    Name thisTable = name(getTable().getSchema().getName(), getTable().getTableName());
    Name thisColumn = name(getName());
    Name toTable = name(getTable().getSchema().getName(), getRefTableName());
    Name toColumn = name(getRefColumnName());

    Name functionName =
        name(
            getTable().getSchema().getName(),
            getTable().getTableName() + "_" + getName() + "_UPDATETRIGGER");

    // createTableIfNotExists the function
    getJooq()
        .execute(
            "CREATE FUNCTION {0}() RETURNS trigger AS"
                + "\n$BODY$"
                + "\nDECLARE"
                + "\n\t test {1};"
                + "\nBEGIN"
                + "\n\ttest =  ARRAY (SELECT from_column FROM (SELECT UNNEST(NEW.{2}) as from_column) as from_table "
                + "LEFT JOIN (SELECT {3} as to_column FROM {4}) as to_table "
                + "ON from_table.from_column=to_table.to_column WHERE to_table.to_column IS NULL);"
                + "\n\tIF(array_length(test,1) > 0) THEN "
                + "RAISE EXCEPTION USING ERRCODE='23503', MESSAGE = 'update or delete on table {5} violates foreign key constraint'"
                + " , DETAIL = 'Key ({6})=('|| array_to_string(test,',') ||') is not present in table {7}, column {8}';"
                + "\n\tEND IF;"
                + "\n\tRETURN NEW;"
                + "\nEND;"
                + "\n$BODY$ LANGUAGE plpgsql;",
            functionName, // {0}
            keyword(
                SqlTypeUtils.getPsqlType(
                        getTable()
                            .getSchema()
                            .getTableMetadata(getRefTableName())
                            .getColumn(getRefColumnName()))
                    + "[]"), // {1}
            thisColumn, // {2}
            toColumn, // {3}
            toTable, // {4}
            inline(getTableName()), // {5}
            inline(getName()), // {6}
            inline(getRefTableName()), // {7}
            inline(getRefColumnName())); // {8}

    // add the trigger
    getJooq()
        .execute(
            "CREATE CONSTRAINT TRIGGER {0} "
                + "\n\tAFTER INSERT OR UPDATE OF {1} ON {2} FROM {3}"
                + "\n\tDEFERRABLE INITIALLY IMMEDIATE "
                + "\n\tFOR EACH ROW EXECUTE PROCEDURE {4}()",
            name(getTableName() + "_" + getName()), thisColumn, thisTable, toTable, functionName);
  }
}
