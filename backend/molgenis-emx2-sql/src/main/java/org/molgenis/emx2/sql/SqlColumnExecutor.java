package org.molgenis.emx2.sql;

import org.jooq.*;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
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

public class SqlColumnExecutor {
  private SqlColumnExecutor() {
    // hide
  }

  public static void executeSetNullable(DSLContext jooq, Column column) {
    boolean isNullable = column.isNullable();
    switch (column.getColumnType()) {
      case REFBACK:
        if (!column.isNullable()) {
          throw new MolgenisException(
              "Set NOT NULL failed on column "
                  + column.getName()
                  + ": refback column must always be nullable (not null not supported)");
        }
        break;
      case REF:
      case REF_ARRAY:
        isNullable = column.getRefColumn().isNullable() || isNullable;
        break;
      case MREF:
        // nullability checked on the jointable
        isNullable = true;
        break;
      default:
        // if has default, we will first update all 'null' to default
        if (!column.isNullable() && column.getDefaultValue() != null) {
          jooq.update(column.getJooqTable())
              .set(column.getJooqField(), column.getDefaultValue())
              .where(column.getJooqField().isNull())
              .execute();
        }
        break;
    }
    // in case of FILE we have to add all parts
    if (FILE.equals(column.getColumnType())) {
      for (Field f : column.getJooqFileFields()) {
        executeSetNullable(jooq, column.getJooqTable(), f, column.isNullable());
      }

    }
    // simply add set nullability
    else {
      executeSetNullable(jooq, column.getJooqTable(), column.getJooqField(), isNullable);
    }
  }

  private static void executeSetNullable(
      DSLContext jooq, Table table, Field field, boolean nullable) {
    if (nullable) {
      jooq.alterTable(table).alterColumn(field).dropNotNull().execute();
    } else {
      jooq.alterTable(table).alterColumn(field).setNotNull().execute();
    }
  }

  public static String getJoinTableName(Column column) {
    return column.getTable().getTableName() + "-" + column.getName();
  }

  static void executeAlterName(DSLContext jooq, Column oldColumn, Column newColumn) {
    // asumes validated before
    if (!oldColumn.getName().equals(newColumn.getName())) {
      if (FILE.equals(newColumn.getColumnType())) {
        for (String suffix :
            new String[] {"_id", "_extension", "_size", "_contents", "_mimetype"}) {
          jooq.execute(
              "ALTER TABLE {0} RENAME COLUMN {1} TO {2}",
              newColumn.getJooqTable(),
              field(name(oldColumn.getName() + suffix)),
              field(name(newColumn.getName() + suffix)));
        }
      } else {
        jooq.execute(
            "ALTER TABLE {0} RENAME COLUMN {1} TO {2}",
            newColumn.getJooqTable(),
            field(name(oldColumn.getName())),
            field(name(newColumn.getName())));
      }
    }
  }

  static void executeAlterType(DSLContext jooq, Column oldColumn, Column newColumn) {
    Table table = newColumn.getTable().getJooqTable();

    if (oldColumn.getColumnType().equals(newColumn.getColumnType())) {
      return; // nothing to do
    }

    // catch cases we do not support
    if (FILE.equals(oldColumn.getColumnType()) && !FILE.equals(newColumn.getColumnType())
        || !FILE.equals(oldColumn.getColumnType()) && FILE.equals(newColumn.getColumnType())) {
      throw new MolgenisException(
          "Alter type for column '" + newColumn.getName() + "' failed",
          "Cannot convert from or to binary");
    }

    // pre changes
    if (REF_ARRAY.equals(oldColumn.getColumnType())) {
      // if ref_array drop the index
      jooq.execute(
          "DROP INDEX {0}",
          name(oldColumn.getSchemaName(), table.getName() + "/" + oldColumn.getName()));
    }

    // change the type
    alterField(
        jooq,
        table,
        oldColumn.getName(),
        oldColumn.getJooqField().getDataType(),
        newColumn.getJooqField().getDataType(),
        getPsqlType(newColumn));

    // post changes
    if (REF_ARRAY.equals(newColumn.getColumnType())) {
      executeCreateRefArrayIndex(jooq, table, newColumn);
    }
  }

  static void alterField(
      DSLContext jooq,
      Table table,
      String columnName,
      DataType oldType,
      DataType newType,
      String postgresType) {

    // change the raw type
    if (!newType.equals(oldType)) {
      if (newType.isArray() && !oldType.isArray()) {
        jooq.execute(
            "ALTER TABLE {0} ALTER COLUMN {1} TYPE {2} USING array[{1}::{3}]",
            table,
            name(columnName),
            keyword(postgresType),
            keyword(postgresType.replace("[]", ""))); // non-array type needed
      } else {
        jooq.execute(
            "ALTER TABLE {0} ALTER COLUMN {1} TYPE {2} USING {1}::{2}",
            table, name(columnName), keyword(postgresType));
      }
    }
  }

  static void reapplyRefbackContraints(Column oldColumn, Column newColumn) {
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

  static void executeRemoveRefback(Column oldColumn, Column newColumn) {
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
    if (column.isReference()) {
      jooq.alterTable(column.getJooqTable()).addColumn(column.getJooqField()).execute();
      if (REF_ARRAY.equals(column.getColumnType())) {
        executeCreateRefArrayIndex(jooq, column.getJooqTable(), column);
      }
    } else if (FILE.equals(column.getColumnType())) {
      for (Field f : column.getJooqFileFields()) {
        jooq.alterTable(column.getJooqTable()).addColumn(f).execute();
      }
    } else {
      jooq.alterTable(column.getJooqTable()).addColumn(column.getJooqField()).execute();
      executeSetDefaultValue(jooq, column);
      executeSetNullable(jooq, column);
    }
    // central constraints
    SqlTableMetadataExecutor.updateSearchIndexTriggerFunction(jooq, column.getTable());
    saveColumnMetadata(jooq, column);
  }

  static void validateColumn(Column c) {
    if (c.getName() == null) {
      throw new MolgenisException("Add column failed", "Column name cannot be null");
    }
    if (c.getKey() > 0) {
      if (c.getTable().getKeyNames(c.getKey()).size() > 1 && c.isNullable()) {
        throw new MolgenisException(
            "unique on column '" + c.getName() + "' failed",
            "When key spans multiple columns, none of the columns can be nullable");
      }
    }
    if (c.isReference() && c.getRefTable() == null) {
      throw new MolgenisException(
          "Add column '"
              + c.getName()
              + "' failed: for columns of type ref, ref_array, refback and mref 'refTable' ");
    }
    if (c.isReference() && c.getRefTable().getPrimaryKeyColumns().size() > 1) {
      if (c.getRefColumnName() == null) {
        throw new MolgenisException(
            "Add column '"
                + c.getName()
                + "' failed: when reference to a table with primary key consisting of multiple columns then 'refColumn' must be provided");
      }
      if (c.getRefName() == null) {
        throw new MolgenisException(
            "Add column '"
                + c.getName()
                + "' failed: when reference to a table with primary key consisting of multiple columns then 'refName' must be provided");
      }
    }
  }

  private static void executeCreateRefArrayIndex(DSLContext jooq, Table table, Column ref) {
    jooq.execute(
        "CREATE INDEX {0} ON {1} USING GIN( {2} )",
        name(table.getName() + "/" + ref.getName()), table, name(ref.getName()));
  }

  static void executeCreateRefConstraints(DSLContext jooq, Column... columns) {
    // set constraints
    switch (columns[0].getColumnType()) {
      case REF:
        createRefConstraints(jooq, columns);
        break;
      case REF_ARRAY:
        createRefArrayConstraints(jooq, columns);
        break;
      case MREF:
        createMrefConstraints(jooq, columns);
        break;
      case REFBACK:
        createRefBackColumnConstraints(jooq, columns);
        break;
      default:
        break;
    }
  }

  static void executeRemoveColumn(DSLContext jooq, Column column) {
    executeRemoveRefConstraints(jooq, column);
    if (FILE.equals(column.getColumnType())) {
      for (Field f : column.getJooqFileFields()) {
        jooq.alterTable(SqlTableMetadataExecutor.getJooqTable(column.getTable()))
            .dropColumn(f)
            .execute();
      }
    } else {
      jooq.alterTable(SqlTableMetadataExecutor.getJooqTable(column.getTable()))
          .dropColumn(field(name(column.getName())))
          .execute();
    }
    MetadataUtils.deleteColumn(jooq, column);
  }

  static void executeRemoveRefConstraints(DSLContext jooq, Column column) {
    // remove triggers
    switch (column.getColumnType()) {
      case REF:
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
        // nothing to do
    }
  }

  static void updatePositions(Column column, TableMetadata existingTable) {
    if (column.getPosition() == null || column.getPosition() > existingTable.getColumns().size()) {
      column.setPosition(existingTable.getLocalColumns().size());
    } else {
      // if needed move other columns positions
      for (Column c : existingTable.getLocalColumns()) {
        // check for position, don't update columns from parent
        if (c.getPosition() >= column.getPosition()
            && c.getTableName().equals(existingTable.getTableName())) {
          existingTable.alterColumn(c.getName(), c.setPosition(c.getPosition() + 1));
        }
      }
    }
  }

  public static void executeSetDefaultValue(DSLContext jooq, Column newColumn) {
    if (newColumn.getDefaultValue() != null) {
      jooq.alterTable(newColumn.getJooqTable())
          .alterColumn(newColumn.getJooqField())
          .defaultValue(newColumn.getDefaultValue())
          .execute();
    } else {
      // remove default
      jooq.alterTable(newColumn.getJooqTable())
          .alterColumn(newColumn.getJooqField())
          .dropDefault()
          .execute();
    }
  }
}
