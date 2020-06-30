package org.molgenis.emx2.sql;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.utils.TypeUtils;

import java.util.ArrayList;
import java.util.List;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.sql.MetadataUtils.saveColumnMetadata;
import static org.molgenis.emx2.sql.SqlColumnMrefExecutor.createMrefConstraints;
import static org.molgenis.emx2.sql.SqlColumnMrefExecutor.dropMrefConstraints;
import static org.molgenis.emx2.sql.SqlColumnRefArrayExecutor.createRefArrayConstraints;
import static org.molgenis.emx2.sql.SqlColumnRefArrayExecutor.removeRefArrayConstraints;
import static org.molgenis.emx2.sql.SqlColumnRefBackExecutor.createRefBackColumnConstraints;
import static org.molgenis.emx2.sql.SqlColumnRefExecutor.createRefConstraints;
import static org.molgenis.emx2.sql.SqlTypeUtils.getPsqlType;
import static org.molgenis.emx2.sql.SqlTypeUtils.jooqTypeOf;
import static org.molgenis.emx2.utils.TypeUtils.getNonArrayType;

public class SqlColumnExecutor {
  private SqlColumnExecutor() {
    // hide
  }

  static void executeSetNullable(DSLContext jooq, Column column) {
    executeSetNullable(jooq, column, column.isNullable());
  }

  static void executeSetNullable(DSLContext jooq, Column column, boolean nullable) {
    for (Field f : asJooqField(column)) {
      if (nullable) {
        jooq.alterTable(asJooqTable(column.getTable()))
            .alterColumn(f)
            .dropNotNull()
            .execute(); // seperate to not interfere with type}

      } else {
        jooq.alterTable(asJooqTable(column.getTable()))
            .alterColumn(f)
            .setNotNull()
            .execute(); // seperate to not int
      }
    }
  }

  // helper methods
  public static org.jooq.Table asJooqTable(TableMetadata table) {
    return table(name(table.getSchema().getName(), table.getTableName()));
  }

  public static List<Field> asJooqField(Column column) {
    List<Field> result = new ArrayList<>();
    switch (column.getColumnType()) {
      case REF:
        for (Column c : column.getRefColumns()) {
          result.add(
              field(
                  name(column.getName() + (column.isCompositeRef() ? "-" + c.getName() : "")),
                  jooqTypeOf(c.getColumnType())));
        }
        break;
      case REFBACK:
      case MREF:
      case REF_ARRAY:
        for (Column c : column.getRefColumns()) {
          result.add(
              field(
                  name(column.getName() + (column.isCompositeRef() ? "-" + c.getName() : "")),
                  jooqTypeOf(c.getColumnType()).getArrayDataType()));
        }
        break;
      default:
        result.add(field(name(column.getName()), jooqTypeOf(column.getColumnType())));
    }
    return result;
  }

  public static TableMetadata getRefTable(Column column) {
    return column.getTable().getSchema().getTableMetadata(column.getRefTableName());
  }

  public static String getJoinTableName(Column column) {
    return "MREF_" + column.getTable().getTableName() + "_" + column.getName();
  }

  protected static Column getMappedByColumn(Column column) {
    return getRefTable(column).getColumn(column.getMappedBy());
  }

  static void executeAlterColumn(DSLContext jooq, Column oldColumn, Column newColumn) {

    // remove old constraints
    executeRemoveConstraints(jooq, oldColumn);

    // rename if needed
    if (!oldColumn.getName().equals(newColumn.getName())) {
      List<Field> oldField = asJooqField(oldColumn);
      List<Field> newField = asJooqField(newColumn);
      for (int i = 0; i < oldField.size(); i++) {
        jooq.execute(
            "ALTER TABLE {0} RENAME COLUMN {1} TO {2}",
            asJooqTable(newColumn.getTable()), oldField.get(i), newField.get(i));
      }
      // delete old metadata
      MetadataUtils.deleteColumn(jooq, oldColumn);
    }

    // change the raw type
    if (newColumn.getColumnType().getType().isArray()
        && !oldColumn.getColumnType().getType().isArray()) {
      for (Field f : asJooqField(newColumn)) {
        jooq.execute(
            "ALTER TABLE {0} ALTER COLUMN {1} TYPE {2} USING array[{1}::{3}]",
            asJooqTable(newColumn.getTable()),
            f,
            keyword(getPsqlType(newColumn)),
            keyword(getPsqlType(getNonArrayType(TypeUtils.getPrimitiveColumnType(newColumn)))));
      }
    } else {
      for (Field f : asJooqField(newColumn)) {
        jooq.execute(
            "ALTER TABLE {0} ALTER COLUMN {1} TYPE {2} USING {1}::{2}",
            asJooqTable(newColumn.getTable()), f, keyword(getPsqlType(newColumn)));
      }
    }

    // add the new constraints
    switch (newColumn.getColumnType()) {
      case REF:
        SqlColumnRefExecutor.createRefConstraints(jooq, newColumn);
        executeSetNullable(jooq, newColumn);
        break;
      case REF_ARRAY:
        createMrefConstraints(jooq, newColumn);
        executeSetNullable(jooq, newColumn);
        break;
      case REFBACK:
        SqlColumnRefBackExecutor.createRefBackColumnConstraints(jooq, newColumn);
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
    for (Field f : asJooqField(column)) {
      jooq.alterTable(asJooqTable(column.getTable())).addColumn(f).execute();
    }
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
        SqlColumnRefBackExecutor.removeRefBackConstraints(jooq, column);
        break;
      case MREF:
        dropMrefConstraints(jooq, column);
      default:
        // nothing else?
    }
    // remove nullable
    for (Field f : asJooqField(column)) {
      jooq.alterTable(asJooqTable(column.getTable()))
          .alterColumn(f)
          .dropNotNull()
          .execute(); // seperate to not interfere with type
    }
  }

  static String getSchemaName(Column column) {
    return column.getTable().getSchema().getName();
  }
}
