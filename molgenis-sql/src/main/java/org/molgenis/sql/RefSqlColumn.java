package org.molgenis.sql;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Name;
import org.molgenis.MolgenisException;
import org.molgenis.Table;

import static org.jooq.impl.DSL.*;
import static org.molgenis.Type.REF;

public class RefSqlColumn extends SimpleSqlColumn {
  private DSLContext sql;

  public RefSqlColumn(
      DSLContext sql, Table table, String name, String otherTable, String otherColumn)
      throws MolgenisException {
    super(sql, table, name, REF, otherTable, otherColumn);
    this.sql = sql;
  }

  @Override
  public RefSqlColumn createColumn() throws MolgenisException {
    // jooq parameters
    Field thisColumn = field(name(getName()), SqlTypeUtils.jooqTypeOf(this));
    org.jooq.Table thisTable = table(name(getTable().getSchema().getName(), getTable().getName()));
    Name fkeyConstraintName =
        name(
            getTable().getName()
                + "."
                + getName()
                + " REFERENCES "
                + getRefTable()
                + "."
                + getRefColumn());
    Name fkeyTable = name(getTable().getSchema().getName(), getRefTable());
    Name fkeyField = name(getRefColumn());

    // execute sql
    sql.alterTable(thisTable).addColumn(thisColumn).execute();
    sql.alterTable(thisTable)
        .add(
            constraint(fkeyConstraintName)
                .foreignKey(thisColumn)
                .references(fkeyTable, fkeyField)
                .onUpdateCascade())
        .execute();
    sql.execute(
        "ALTER TABLE {0} ALTER CONSTRAINT {1} DEFERRABLE INITIALLY IMMEDIATE",
        thisTable, fkeyConstraintName);
    ;
    sql.createIndex(name(getTable().getName()) + "_" + name(getName()) + "_FKINDEX")
        .on(thisTable, thisColumn)
        .execute();

    return this;
  }
}
