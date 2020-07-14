package org.molgenis.emx2.sql;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Reference;
import org.molgenis.emx2.TableMetadata;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.sql.MetadataUtils.saveColumnMetadata;
import static org.molgenis.emx2.sql.SqlColumnMrefExecutor.createMrefConstraints;
import static org.molgenis.emx2.sql.SqlColumnMrefExecutor.dropMrefConstraints;
import static org.molgenis.emx2.sql.SqlColumnRefArrayExecutor.createRefArrayConstraints;
import static org.molgenis.emx2.sql.SqlColumnRefArrayExecutor.removeRefArrayConstraints;
import static org.molgenis.emx2.sql.SqlColumnRefBackExecutor.createRefBackColumnConstraints;
import static org.molgenis.emx2.sql.SqlColumnRefBackExecutor.removeRefBackConstraints;
import static org.molgenis.emx2.sql.SqlColumnRefExecutor.createRefConstraints;
import static org.molgenis.emx2.sql.SqlTypeUtils.getPsqlType;
import static org.molgenis.emx2.utils.TypeUtils.getNonArrayType;

public class SqlColumnExecutor {
  private SqlColumnExecutor() {
    // hide
  }

  static void executeSetNullable(DSLContext jooq, Table table, Field field, boolean nullable) {
    if (nullable) {
      jooq.alterTable(table)
          .alterColumn(field)
          .dropNotNull()
          .execute(); // seperate to not interfere with type
    } else {
      jooq.alterTable(table).alterColumn(field).setNotNull().execute(); // seperate to not int
    }
  }

  static void executeSetNullable(DSLContext jooq, Column column) {
    executeSetNullable(jooq, column, column.isNullable());
  }

  static void executeSetNullable(DSLContext jooq, Column column, boolean isNullable) {
    switch (column.getColumnType()) {
      case REF:
      case REF_ARRAY:
      case MREF:
        for (Reference ref : column.getRefColumns()) {
          executeSetNullable(jooq, column.getJooqTable(), ref.asJooqField(), isNullable);
        }
        break;
      default:
        executeSetNullable(jooq, column.getJooqTable(), column.asJooqField(), isNullable);
    }
  }

  public static TableMetadata getRefTable(Column column) {
    return column.getTable().getSchema().getTableMetadata(column.getRefTableName());
  }

  public static String getJoinTableName(Column column) {
    return column.getTable().getTableName() + "-" + column.getName();
  }

  protected static Column getMappedByColumn(Column column) {
    return getRefTable(column).getColumn(column.getMappedBy());
  }

  static void executeAlterColumn(DSLContext jooq, Column oldColumn, Column newColumn) {

    // check if reference and of different size
    if (REF_ARRAY.equals(newColumn.getColumnType())
        && newColumn.getRefTable().getPrimaryKeyFields().size() > 1) {
      throw new MolgenisException(
          "Alter column of '" + oldColumn.getName() + " failed",
          "REF_ARRAY is not supported for composite keys of table " + newColumn.getRefTableName());
    }
    if ((oldColumn.getRefColumns().size() > 1 || oldColumn.getRefColumns().size() > 1)
        && oldColumn.getRefColumns().size() != newColumn.getRefColumns().size()) {
      throw new MolgenisException(
          "Cannot alter column '" + oldColumn.getName(),
          "New column '"
              + newColumn.getName()
              + "' has different number of reference multiplicity then '"
              + oldColumn.getName()
              + "'");
    }

    // remove old constraints
    executeRemoveConstraints(jooq, oldColumn);

    Table table = newColumn.getTable().asJooqTable();
    Field oldField =
        oldColumn.isReference()
            ? oldColumn.getRefColumns().get(0).asJooqField()
            : oldColumn.asJooqField();
    Field newField =
        newColumn.isReference()
            ? newColumn.getRefColumns().get(0).asJooqField()
            : newColumn.asJooqField();
    String postgresType =
        newColumn.isReference()
            ? getPsqlType(newColumn.getRefColumns().get(0).getColumnType())
            : getPsqlType(newColumn);

    // rename if needed
    if (!oldColumn.getName().equals(newColumn.getName())) {
      jooq.execute("ALTER TABLE {0} RENAME COLUMN {1} TO {2}", table, oldField, newField);
      // delete old metadata
      MetadataUtils.deleteColumn(jooq, oldColumn);
    }

    // change the raw type
    if (newColumn.getColumnType().getType().isArray()
        && !oldColumn.getColumnType().getType().isArray()) {
      jooq.execute(
          "ALTER TABLE {0} ALTER COLUMN {1} TYPE {2} USING array[{1}::{3}]",
          table,
          newField,
          keyword(postgresType),
          keyword(postgresType.replace("[]", ""))); // non-array type needed
    } else {
      jooq.execute(
          "ALTER TABLE {0} ALTER COLUMN {1} TYPE {2} USING {1}::{2}",
          table, newField, keyword(postgresType));
    }

    // add the new constraints
    switch (newColumn.getColumnType()) {
      case REF:
        createRefConstraints(jooq, newColumn);
        executeSetNullable(jooq, newColumn);
        break;
      case REF_ARRAY:
        if (newColumn.getRefColumns().size() > 1)
          throw new MolgenisException(
              "Cannot create column '" + newColumn.getName() + "'",
              "REF_ARRAY cannot refer to composite key of " + newColumn.getRefTableName());
        createRefArrayConstraints(jooq, newColumn);
        executeSetNullable(jooq, newColumn);
        break;
      case REFBACK:
        createRefBackColumnConstraints(jooq, newColumn);
        executeSetNullable(jooq, newColumn);
        break;
      case MREF:
        createMrefConstraints(jooq, newColumn);
        // always nullable, constraint is on mref table
        executeSetNullable(jooq, newColumn, true);
        break;
      default:
        executeSetNullable(jooq, newColumn);
        executeRemoveRefback(oldColumn, newColumn);
    }
    saveColumnMetadata(jooq, newColumn);
  }

  public static void reapplyRefbackContraints(Column oldColumn, Column newColumn) {
    if ((REF.equals(oldColumn.getColumnType()) || REF_ARRAY.equals(oldColumn.getColumnType()))
        && (REF.equals(newColumn.getColumnType()) || REF_ARRAY.equals(newColumn.getColumnType()))) {
      for (Column check : oldColumn.getRefTable().getColumns()) {
        if (REFBACK.equals(check.getColumnType())
            && oldColumn.getName().equals(check.getMappedBy())) {
          check.getTable().dropColumn(check.getName());
          check.getTable().add(check);
        }
      }
    }
  }

  private static void executeRemoveRefback(Column oldColumn, Column newColumn) {
    if ((REF.equals(oldColumn.getColumnType()) || REF_ARRAY.equals(oldColumn.getColumnType()))
        && !(REF.equals(newColumn.getColumnType())
            || REF_ARRAY.equals(newColumn.getColumnType()))) {
      for (Column check : oldColumn.getRefTable().getColumns()) {
        if (REFBACK.equals(check.getColumnType())
            && oldColumn.getName().equals(check.getMappedBy())) {
          check.getTable().dropColumn(check.getName());
        }
      }
    }
  }

  static void executeCreateColumn(DSLContext jooq, Column column) {

    if (column.isReference()) {
      for (Reference ref : column.getRefColumns()) {
        jooq.alterTable(column.getJooqTable()).addColumn(ref.asJooqField()).execute();
      }
    } else {
      jooq.alterTable(column.getJooqTable()).addColumn(column.asJooqField()).execute();
    }
    // create the column

    // central constraints
    SqlTableMetadataExecutor.updateSearchIndexTriggerFunction(jooq, column.getTable());
    saveColumnMetadata(jooq, column);
  }

  static void executeSetForeignkeys(DSLContext jooq, Column column) {
    // set constraints
    switch (column.getColumnType()) {
      case REF:
        createRefConstraints(jooq, column);
        executeSetNullable(jooq, column);
        break;
      case REF_ARRAY:
        createRefArrayConstraints(jooq, column);
        executeSetNullable(jooq, column);
        break;
      case MREF:
        createMrefConstraints(jooq, column);
        // always null, nullable constraint is on jointable
        executeSetNullable(jooq, column, true);
        break;
      case REFBACK:
        createRefBackColumnConstraints(jooq, column);
        break;
      default:
        executeSetNullable(jooq, column);
    }
  }

  static void executeRemoveColumn(DSLContext jooq, Column column) {
    executeRemoveConstraints(jooq, column);
    jooq.alterTable(SqlTableMetadataExecutor.getJooqTable(column.getTable()))
        .dropColumn(field(name(column.getName())))
        .execute();
    MetadataUtils.deleteColumn(jooq, column);
  }

  static void executeRemoveConstraints(DSLContext jooq, Column column) {
    // remove triggers
    switch (column.getColumnType()) {
      case REF:
        // nothing to do?
        SqlColumnRefExecutor.removeRefConstraints(jooq, column);
        break;
      case REF_ARRAY:
        removeRefArrayConstraints(jooq, column);
        break;
      case REFBACK:
        removeRefBackConstraints(jooq, column);
        break;
      case MREF:
        dropMrefConstraints(jooq, column);
      default:
        // nothing else?
    }
    // remove nullable
    if (column.isReference()) {
      for (Reference ref : column.getRefColumns()) {
        jooq.alterTable(column.getJooqTable())
            .alterColumn(ref.asJooqField())
            .dropNotNull()
            .execute(); // seperate to not interfere with type
      }
    } else {
      jooq.alterTable(column.getJooqTable())
          .alterColumn(column.asJooqField())
          .dropNotNull()
          .execute(); // seperate to not interfere with type
    }
  }

  static String getSchemaName(Column column) {
    return column.getTable().getSchema().getName();
  }
}
