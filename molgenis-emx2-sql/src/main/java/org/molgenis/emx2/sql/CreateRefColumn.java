package org.molgenis.emx2.sql;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.exception.DataAccessException;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.sql.MetadataUtils.saveColumnMetadata;

public class CreateRefColumn {

  public static void createRefColumn(DSLContext jooq, Column column) {
    try {

      String refTableName = column.getRefTableName();
      if (refTableName == null) {
        throw new MolgenisException(
            "Create column failed",
            "Create of column '" + column.getName() + "' failed because RefTableName was not set");
      }

      String refColumnName = column.getRefColumnName();
      if (refColumnName == null) {
        refColumnName =
            column
                .getTable()
                .getSchema()
                .getTableMetadata(column.getRefTableName())
                .getPrimaryKey();
        if (refColumnName == null) {
          throw new MolgenisException(
              "Create column failed",
              "Create of column '"
                  + column.getName()
                  + "' failed because RefColumnName was not set nor the other table has primary key set");
        }
      }

      // create the column
      Field thisColumn = field(name(column.getName()), SqlTypeUtils.jooqTypeOf(column));
      org.jooq.Table thisTable =
          table(name(column.getTable().getSchema().getName(), column.getTable().getTableName()));

      // execute alter table add column
      jooq.alterTable(thisTable).addColumn(thisColumn).execute();

      Name fkeyConstraintName =
          name(
              column.getTable().getTableName()
                  + "."
                  + column.getName()
                  + " REFERENCES "
                  + refTableName
                  + "."
                  + refColumnName);
      Name fkeyTable = name(column.getTable().getSchema().getName(), refTableName);
      Name fkeyField = name(refColumnName);

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

      jooq.createIndex(
              name(column.getTable().getTableName()) + "_" + name(column.getName()) + "_FKINDEX")
          .on(thisTable, thisColumn)
          .execute();
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException(
          "Foreign key '"
              + column.getName()
              + "' could not be created in table '"
              + column.getTable().getTableName()
              + "'",
          dae);
    }
  }
}
