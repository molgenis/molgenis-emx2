package org.molgenis.emx2.sql;

import org.jooq.DSLContext;
import org.jooq.DataType;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.utils.TypeUtils;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.sql.SqlColumnRefArrayUtils.createRefArrayConstraints;
import static org.molgenis.emx2.sql.SqlColumnRefBackUtils.createRefBackColumnConstraints;
import static org.molgenis.emx2.sql.SqlColumnRefUtils.createRefConstraints;
import static org.molgenis.emx2.sql.MetadataUtils.saveColumnMetadata;
import static org.molgenis.emx2.sql.SqlTypeUtils.*;
import static org.molgenis.emx2.utils.TypeUtils.getNonArrayType;

public class SqlColumnUtils {
  private SqlColumnUtils() {
    // hide
  }

  static void executeSetNullable(DSLContext jooq, Column column) {
    if (column.isNullable()) {
      jooq.alterTable(asJooqTable(column.getTable()))
          .alterColumn(asJooqField(column))
          .dropNotNull()
          .execute(); // seperate to not interfere with type
    } else {
      jooq.alterTable(asJooqTable(column.getTable()))
          .alterColumn(asJooqField(column))
          .setNotNull()
          .execute(); // seperate to not int
    }
  }

  // helper methods
  public static org.jooq.Table asJooqTable(TableMetadata table) {
    return table(name(table.getSchema().getName(), table.getTableName()));
  }

  public static org.jooq.Field asJooqField(Column column) {
    DataType thisType = jooqTypeOf(column);
    return field(name(column.getName()), thisType);
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
      jooq.execute(
          "ALTER TABLE {0} RENAME COLUMN {1} TO {2}",
          asJooqTable(newColumn.getTable()), asJooqField(oldColumn), asJooqField(newColumn));
      // delete old metadata
      MetadataUtils.deleteColumn(jooq, oldColumn);
    }

    // change the raw type
    if (newColumn.getColumnType().getType().isArray()
        && !oldColumn.getColumnType().getType().isArray()) {
      jooq.execute(
          "ALTER TABLE {0} ALTER COLUMN {1} TYPE {2} USING array[{1}::{3}]",
          asJooqTable(newColumn.getTable()),
          asJooqField(newColumn),
          keyword(getPsqlType(newColumn)),
          keyword(getPsqlType(getNonArrayType(TypeUtils.getPrimitiveColumnType(newColumn)))));
    } else {
      jooq.execute(
          "ALTER TABLE {0} ALTER COLUMN {1} TYPE {2} USING {1}::{2}",
          asJooqTable(newColumn.getTable()),
          asJooqField(newColumn),
          keyword(getPsqlType(newColumn)));
    }

    // add the new constraints
    switch (newColumn.getColumnType()) {
      case REF:
        SqlColumnRefUtils.createRefConstraints(jooq, newColumn);
        executeSetNullable(jooq, newColumn);
        break;
      case REF_ARRAY:
        SqlColumnRefArrayUtils.createRefArrayConstraints(jooq, newColumn);
        executeSetNullable(jooq, newColumn);
        break;
      case REFBACK:
        SqlColumnRefBackUtils.createRefBackColumnConstraints(jooq, newColumn);
        executeSetNullable(jooq, newColumn);
        break;
        //      case MREF:
        //        SqlColumnMrefUtils.createMrefConstraints(jooq, newColumn);
        //        break;
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
    // create the column
    jooq.alterTable(asJooqTable(column.getTable())).addColumn(asJooqField(column)).execute();

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
      case REFBACK:
        createRefBackColumnConstraints(jooq, column);
        break;
        //      case MREF:
        //        createMrefConstraints(jooq, column);
        //        break;
      default:
        executeSetNullable(jooq, column);
    }
    // central constraints
    SqlTableMetadataExecutor.updateSearchIndexTriggerFunction(jooq, column.getTable());
    saveColumnMetadata(jooq, column);
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
        SqlColumnRefUtils.removeRefConstraints(jooq, column);
        break;
      case REF_ARRAY:
        SqlColumnRefArrayUtils.removeRefArrayConstraints(jooq, column);
        break;
      case REFBACK:
        SqlColumnRefBackUtils.removeRefBackConstraints(jooq, column);
        break;
        //      case MREF:
        //        throw new MolgenisException("Error", "Alter on MREF field Not yet implemented");
      default:
        // nothing else?
    }
    // remove nullable
    jooq.alterTable(asJooqTable(column.getTable()))
        .alterColumn(asJooqField(column))
        .dropNotNull()
        .execute(); // seperate to not interfere with type
  }

  static String getSchemaName(Column column) {
    return column.getTable().getSchema().getName();
  }
}
