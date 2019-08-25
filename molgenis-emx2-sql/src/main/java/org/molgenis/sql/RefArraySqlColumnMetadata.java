package org.molgenis.sql;

import org.jooq.DSLContext;
import org.jooq.Name;
import org.molgenis.MolgenisException;

import static org.jooq.impl.DSL.keyword;
import static org.jooq.impl.DSL.name;
import static org.molgenis.metadata.Type.REF_ARRAY;
import static org.molgenis.sql.MetadataUtils.saveColumnMetadata;

public class RefArraySqlColumnMetadata extends SqlColumnMetadata {

  public RefArraySqlColumnMetadata(
      SqlTableMetadata table, String columnName, String toTable, String toColumn) {
    super(table, columnName, REF_ARRAY);
    this.setReference(toTable, toColumn);
  }

  @Override
  public RefArraySqlColumnMetadata createColumn() throws MolgenisException {
    super.createColumn();
    this.createReferenceExistsTrigger();
    this.createIsReferencedByTrigger();
    saveColumnMetadata(this);
    return this;
  }

  // this trigger is to check for foreign violations: to prevent that referenced records cannot be
  // changed/deleted in such a way that we get dangling foreign key references.
  // todo: enable cascading updates
  private void createIsReferencedByTrigger() throws MolgenisException {
    Name triggerName = getTriggerName();
    Name toTable = name(getTable().getSchema().getName(), getRefTableName());
    Name thisTable = name(getTable().getSchema().getName(), getTable().getName());
    Name thisColumn = name(getName());
    Name toColumn = name(getRefColumnName());

    Name functionName =
        name(
            getTable().getSchema().getName(),
            getTable().getName() + "_" + getName() + "_REF_ARRAY_TRIGGER2");

    // create the function
    getJooq()
        .execute(
            "CREATE FUNCTION {0}() RETURNS trigger AS"
                + "\n$BODY$"
                + "\n\tBEGIN"
                + "\n\tIF(EXISTS(SELECT * from {1} WHERE OLD.{2} = ANY({3}) ) ) THEN "
                + "RAISE EXCEPTION USING ERRCODE='23503', MESSAGE = 'update or delete on table "
                + toTable.unqualifiedName().toString()
                + " violates foreign key constraint "
                + triggerName.unqualifiedName().toString()
                + " on table "
                + thisTable.unqualifiedName().toString()
                + ""
                + "', DETAIL = 'Key ("
                + toColumn.unqualifiedName().toString()
                + ")=('|| OLD.{2} ||') is still referenced from table "
                + thisTable.unqualifiedName().toString()
                + "';"
                + "\n\tEND IF;"
                + "\n\tRETURN NEW;"
                + "\nEND;"
                + "\n$BODY$ LANGUAGE plpgsql;",
            functionName,
            thisTable,
            toColumn,
            thisColumn);

    // create the trigger
    getJooq()
        .execute(
            "CREATE CONSTRAINT TRIGGER {0} "
                + "\n\tAFTER UPDATE OR DELETE ON {1} "
                + "\n\tDEFERRABLE INITIALLY IMMEDIATE "
                + "\n\tFOR EACH ROW EXECUTE PROCEDURE {2}()",
            triggerName, toTable, functionName);
  }

  /** trigger on this column to check if foreign key exists */
  private void createReferenceExistsTrigger() throws MolgenisException {
    Name triggerName = getTriggerName();
    Name thisTable = name(getTable().getSchema().getName(), getTable().getName());
    Name thisColumn = name(getName());
    Name toTable = name(getTable().getSchema().getName(), getRefTableName());
    Name toColumn = name(getRefColumnName());

    Name functionName =
        name(
            getTable().getSchema().getName(),
            getTable().getName() + "_" + getName() + "_REF_ARRAY_TRIGGER");

    // create the function
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
                + "\n\tIF(cardinality(test) > 0) THEN "
                + "RAISE EXCEPTION 'insert or update on table "
                + thisTable.unqualifiedName().toString() // for odd reasons {5} and {6} didn't work
                + " violates foreign key constraint "
                + triggerName.unqualifiedName().toString()
                + "' USING ERRCODE = '23503', DETAIL = 'Key("
                + thisColumn.unqualifiedName().toString()
                + ")=(' || array_to_string(test,',') || ') is not present in table "
                + toTable.unqualifiedName().toString()
                + "';"
                + "\n\tEND IF;"
                + "\n\tRETURN NEW;"
                + "\nEND;"
                + "\n$BODY$ LANGUAGE plpgsql;",
            functionName,
            keyword(
                SqlTypeUtils.getPsqlType(
                        getTable()
                            .getSchema()
                            .getTableMetadata(getRefTableName())
                            .getColumn(getRefColumnName()))
                    + "[]"),
            thisColumn,
            toColumn,
            toTable);

    // add the trigger
    getJooq()
        .execute(
            "CREATE CONSTRAINT TRIGGER {0} "
                + "\n\tAFTER INSERT OR UPDATE OF {1} ON {2} FROM {3}"
                + "\n\tDEFERRABLE INITIALLY IMMEDIATE "
                + "\n\tFOR EACH ROW EXECUTE PROCEDURE {4}()",
            triggerName, thisColumn, thisTable, toTable, functionName);
  }

  private Name getTriggerName() throws MolgenisException {
    return name(
        getTable().getName()
            + "."
            + getName()
            + " REFERENCES "
            + getRefTableName()
            + "."
            + getRefColumnName());
  }
}
