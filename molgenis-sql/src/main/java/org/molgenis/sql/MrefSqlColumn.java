package org.molgenis.sql;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.molgenis.MolgenisException;
import org.molgenis.Table;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.molgenis.Type.MREF;

public class MrefSqlColumn extends SqlColumn {
  private DSLContext jooq;

  public MrefSqlColumn(
      SqlTable sqlTable, String name, String toTable, String toColumn, Boolean isNullable) {
    super(sqlTable, name, MREF, toTable, toColumn, isNullable);
    this.jooq = sqlTable.getJooq();
  }

  @Override
  public MrefSqlColumn createColumn() throws MolgenisException {
    // createColumn the array column (we use for trigger)
    org.jooq.Table thisTable = asJooqTable();
    Field thisColumn = field(name(getName()), SqlTypeUtils.jooqTypeOf(this));
    jooq.alterTable(thisTable).addColumn(thisColumn).execute();
    jooq.alterTable(thisTable).alterColumn(thisColumn).setNotNull().execute();

    // createColumn the mref table
    try {
      getTable()
          .getSchema()
          .getTable(String.format("MREF|%s|%s|%s", this.getName(), getRefTable(), getRefColumn()));
      throw new UnsupportedOperationException("TODO implement MREF back");
    } catch (Exception e) {
      Table mrefTable =
          getTable()
              .getSchema()
              .createTable(
                  String.format("MREF|%s|%s|%s", getRefTable(), getRefColumn(), this.getName()));
      mrefTable.addRef(getRefColumn(), this.getTable().getName(), this.getName());
      mrefTable.addRef(getName(), getRefTable(), getRefColumn());
    }
    return this;
  }
}
