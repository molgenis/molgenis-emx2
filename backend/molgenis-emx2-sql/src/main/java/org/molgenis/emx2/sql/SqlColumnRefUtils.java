package org.molgenis.emx2.sql;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.Table;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.sql.SqlColumnUtils.getSchemaName;
import static org.molgenis.emx2.sql.SqlTable.getJooqField;
import static org.molgenis.emx2.sql.SqlTableMetadataExecutor.getJooqTable;

public class SqlColumnRefUtils {
  private SqlColumnRefUtils() {
    // hide
  }

  static void removeRefConstraints(DSLContext jooq, Column column) {
    jooq.alterTable(getJooqTable(column.getTable()))
        .dropConstraint(getRefConstraintName(column))
        .execute();
    jooq.dropIndex(name(getSchemaName(column), getIndexName(column))).execute();
  }

  static void createRefConstraints(DSLContext jooq, Column column) {

    String refTableName = column.getRefTableName();
    if (refTableName == null) {
      throw new MolgenisException(
          "Create column failed",
          "Create of column '" + column.getName() + "' failed because RefTableName was not set");
    }

    String refColumnName = column.getRefColumnName();
    if (refColumnName == null) {
      if (column.getTable().getSchema().getTableMetadata(column.getRefTableName()).getPrimaryKey()
              == null
          || column
                  .getTable()
                  .getSchema()
                  .getTableMetadata(column.getRefTableName())
                  .getPrimaryKey()
                  .length
              != 1) {
        throw new MolgenisException(
            "Create column failed",
            "Create of column '"
                + column.getName()
                + "' failed because RefColumnName was not set nor the other table has singular primary key set");
      } else {
        refColumnName =
            column
                .getTable()
                .getSchema()
                .getTableMetadata(column.getRefTableName())
                .getPrimaryKey()[0];
      }
    }

    Name fkeyConstraintName = name(getRefConstraintName(column));

    Table thisTable = getJooqTable(column.getTable());
    Field thisField = getJooqField(column);
    Name fkeyTable = name(column.getTable().getSchema().getName(), refTableName);
    Name fkeyField = name(refColumnName);

    jooq.alterTable(getJooqTable(column.getTable()))
        .add(
            constraint(fkeyConstraintName)
                .foreignKey(thisField)
                .references(fkeyTable, fkeyField)
                .onUpdateCascade())
        .execute();

    jooq.execute(
        "ALTER TABLE {0} ALTER CONSTRAINT {1} DEFERRABLE INITIALLY IMMEDIATE",
        thisTable, fkeyConstraintName);

    jooq.createIndex(getIndexName(column)).on(thisTable, thisField).execute();
  }

  private static String getIndexName(Column column) {
    return column.getTable().getTableName() + "_" + column.getName() + "_FKINDEX";
  }

  private static String getRefConstraintName(Column column) {
    return column.getTable().getTableName()
        + "."
        + column.getName()
        + " REFERENCES "
        + column.getRefTableName()
        + "."
        + column.getRefColumnName();
  }
}
