package org.molgenis.emx2.sql;

import org.jooq.ConstraintForeignKeyOnStep;
import org.jooq.DSLContext;
import org.jooq.Name;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Reference;

import java.util.ArrayList;
import java.util.List;

import static org.jooq.impl.DSL.constraint;
import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.sql.SqlColumnExecutor.getSchemaName;
import static org.molgenis.emx2.sql.SqlTableMetadataExecutor.getJooqTable;

public class SqlColumnRefExecutor {
  private SqlColumnRefExecutor() {
    // hide
  }

  static void removeRefConstraints(DSLContext jooq, Column column) {
    jooq.alterTable(getJooqTable(column.getTable()))
        .dropConstraint(getRefConstraintName(column))
        .execute();
    jooq.execute("DROP INDEX {0}", name(getSchemaName(column), getIndexName(column)));
  }

  static void createRefConstraints(DSLContext jooq, Column column) {
    String refTableName = column.getRefTableName();
    validateRef(column, refTableName);

    Name fkeyConstraintName = name(getRefConstraintName(column));
    Name thisTable = getJooqTable(column.getTable()).getQualifiedName();
    List<Name> thisColumns = new ArrayList<>();
    List<Name> otherColumns = new ArrayList<>();

    for (Reference ref : column.getRefColumns()) {
      thisColumns.add(name(ref.getName()));
      otherColumns.add(name(ref.getTo()));
    }

    Name fkeyTable = name(column.getTable().getSchema().getName(), refTableName);

    ConstraintForeignKeyOnStep constraint =
        constraint(fkeyConstraintName)
            .foreignKey(thisColumns.toArray(new Name[thisColumns.size()]))
            .references(fkeyTable, otherColumns.toArray(new Name[otherColumns.size()]))
            .onUpdateCascade();
    if (column.isCascadeDelete()) {
      constraint = constraint.onDeleteCascade();
    }

    jooq.alterTable(getJooqTable(column.getTable())).add(constraint).execute();

    jooq.execute(
        "ALTER TABLE {0} ALTER CONSTRAINT {1} DEFERRABLE INITIALLY IMMEDIATE",
        thisTable, fkeyConstraintName);

    jooq.createIndex(getIndexName(column))
        .on(thisTable, thisColumns.toArray(new Name[thisColumns.size()]))
        .execute();
  }

  public static void validateRef(Column column, String refTableName) {
    // check if refTable exists
    if (refTableName == null) {
      throw new MolgenisException(
          "Create column failed",
          "Create of column '" + column.getName() + "' failed because RefTableName was not set");
    }

    // check if other end has primary key
    if (column.getRefTable().getPrimaryKeys() == null) {
      throw new MolgenisException(
          "Create column failed",
          "Create of column '"
              + column.getName()
              + "' failed because other table has no primary key set");
    }
  }

  private static String getIndexName(Column column) {
    return column.getTable().getTableName() + "_" + column.getName() + "_FKINDEX";
  }

  private static String getRefConstraintName(Column column) {
    return column.getTable().getTableName()
        + "."
        + column.getName()
        + " REFERENCES "
        + column.getRefTableName();
  }
}
