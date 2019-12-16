package org.molgenis.emx2.sql;

import org.jooq.Field;
import org.jooq.Name;
import org.jooq.exception.DataAccessException;
import org.molgenis.emx2.MolgenisException;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.sql.MetadataUtils.saveColumnMetadata;

public class SqlRefColumn extends SqlColumn {

  public SqlRefColumn(SqlTableMetadata table, String columnName, String toTable, String toColumn) {
    super(table, columnName, REF);
    this.setReference(toTable, toColumn);
  }

  @Override
  public SqlRefColumn createColumn() {
    try {
      String refTableName = getRefTableName();
      if (refTableName == null) {
        throw new MolgenisException(
            "Create column failed",
            "Create of column '" + getName() + "' failed because RefTableName was not set");
      }

      String refColumnName = getRefColumnName();
      if (refColumnName == null) {
        refColumnName = getTable().getSchema().getTableMetadata(getRefTableName()).getPrimaryKey();
        if (refColumnName == null) {
          throw new MolgenisException(
              "Create column failed",
              "Create of column '"
                  + getName()
                  + "' failed because RefColumnName was not set nor the other table has primary key set");
        }
      }
      // define jooq parameters
      Field thisColumn = field(name(getName()), SqlTypeUtils.jooqTypeOf(this));
      org.jooq.Table thisTable =
          table(name(getTable().getSchema().getName(), getTable().getTableName()));

      // execute alter table add column
      getJooq().alterTable(thisTable).addColumn(thisColumn).execute();

      Name fkeyConstraintName =
          name(
              getTable().getTableName()
                  + "."
                  + getName()
                  + " REFERENCES "
                  + refTableName
                  + "."
                  + refColumnName);
      Name fkeyTable = name(getTable().getSchema().getName(), refTableName);
      Name fkeyField = name(refColumnName);

      getJooq()
          .alterTable(thisTable)
          .add(
              constraint(fkeyConstraintName)
                  .foreignKey(thisColumn)
                  .references(fkeyTable, fkeyField)
                  .onUpdateCascade())
          .execute();

      getJooq()
          .execute(
              "ALTER TABLE {0} ALTER CONSTRAINT {1} DEFERRABLE INITIALLY IMMEDIATE",
              thisTable, fkeyConstraintName);

      getJooq()
          .createIndex(name(getTable().getTableName()) + "_" + name(getName()) + "_FKINDEX")
          .on(thisTable, thisColumn)
          .execute();

      saveColumnMetadata(this);
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException(
          "Foreign key '"
              + getName()
              + "' could not be created in table '"
              + getTable().getTableName()
              + "'",
          dae);
    }
    return this;
  }
}
