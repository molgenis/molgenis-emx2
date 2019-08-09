package org.molgenis.sql;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Name;
import org.molgenis.MolgenisException;

import static org.jooq.impl.DSL.*;
import static org.molgenis.Type.REF;
import static org.molgenis.sql.MetadataUtils.saveColumnMetadata;

public class RefSqlColumn extends SqlColumn {
  private DSLContext jooq;

  public RefSqlColumn(SqlTable table, String columnName, String toTable, String toColumn) {
    super(table, columnName, REF);
    this.setReference(toTable, toColumn);
    this.jooq = table.getJooq();
  }

  @Override
  public RefSqlColumn createColumn() throws MolgenisException {

    // define jooq parameters
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

    // execute alter table add column
    jooq.alterTable(thisTable).addColumn(thisColumn).execute();

    jooq.alterTable(thisTable)
        .add(
            constraint(fkeyConstraintName)
                .foreignKey(thisColumn)
                .references(fkeyTable, fkeyField)
                .onUpdateCascade())
        .execute();

    jooq.execute(
        "ALTER TABLE {0} ALTER CONSTRAINT {1} DEFERRABLE INITIALLY IMMEDIATE",
        thisTable, fkeyConstraintName);

    jooq.createIndex(name(getTable().getName()) + "_" + name(getName()) + "_FKINDEX")
        .on(thisTable, thisColumn)
        .execute();

    saveColumnMetadata(this);

    return this;
  }
}
