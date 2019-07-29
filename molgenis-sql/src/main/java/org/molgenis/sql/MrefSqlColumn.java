package org.molgenis.sql;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.molgenis.MolgenisException;
import org.molgenis.Table;
import org.molgenis.Type;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;

public class MrefSqlColumn extends SimpleSqlColumn {

  public MrefSqlColumn(
      DSLContext sql, SqlTable sqlTable, String name, Type mref, String toTable, String toColumn) {
    super(sql, sqlTable, name, mref, toTable, toColumn);
  }

  @Override
  public MrefSqlColumn createColumn() throws MolgenisException {
    // createColumn the array column (we use for trigger)
    org.jooq.Table thisTable = asJooqTable();
    Field thisColumn = field(name(getName()), SqlTypeUtils.jooqTypeOf(this));
    sql.alterTable(thisTable).addColumn(thisColumn).execute();
    sql.alterTable(thisTable).alterColumn(thisColumn).setNotNull().execute();

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
