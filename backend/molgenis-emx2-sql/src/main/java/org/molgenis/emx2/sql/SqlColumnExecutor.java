package org.molgenis.emx2.sql;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Reference;
import org.molgenis.emx2.TableMetadata;

import java.util.List;

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
      case REFBACK:
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

  static void executeAlterNameAndType(DSLContext jooq, Column oldColumn, Column newColumn) {
    Table table = newColumn.getTable().getJooqTable();

    if (oldColumn.isReference() || newColumn.isReference()) {
      if (oldColumn.isReference() && newColumn.isReference()) {
        List<Reference> oldRefs = oldColumn.getRefColumns();
        List<Reference> newRefs = newColumn.getRefColumns();
        for (int i = 0; i < oldRefs.size(); i++) {
          Field oldField = oldRefs.get(i).asJooqField();
          Field newField = newRefs.get(i).asJooqField();
          String postgresType = getPsqlType(newRefs.get(i).getColumnType());
          alterField(jooq, table, oldField, newField, postgresType);
        }
      } else if (oldColumn.isReference()) {
        Field oldField = oldColumn.getRefColumns().get(0).asJooqField();
        alterField(jooq, table, oldField, newColumn.asJooqField(), getPsqlType(newColumn));
      } else {
        Field newField = newColumn.getRefColumns().get(0).asJooqField();
        String postgresType = getPsqlType(newColumn.getRefColumns().get(0).getColumnType());
        alterField(jooq, table, oldColumn.asJooqField(), newField, postgresType);
      }
    } else {
      alterField(
          jooq, table, oldColumn.asJooqField(), newColumn.asJooqField(), getPsqlType(newColumn));
    }
  }

  private static void alterField(
      DSLContext jooq, Table table, Field oldField, Field newField, String postgresType) {
    // change name
    if (!oldField.getName().equals(newField.getName())) {
      jooq.execute("ALTER TABLE {0} RENAME COLUMN {1} TO {2}", table, oldField, newField);
    }
    // change the raw type
    if (!newField.getDataType().equals(oldField.getDataType())) {
      if (newField.getDataType().isArray() && !oldField.getDataType().isArray()) {
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
    }
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

  static void executeCreateRefAndNotNullConstraints(DSLContext jooq, Column column) {
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
    executeRemoveRefAndNotNullConstraints(jooq, column);
    jooq.alterTable(SqlTableMetadataExecutor.getJooqTable(column.getTable()))
        .dropColumn(field(name(column.getName())))
        .execute();
    MetadataUtils.deleteColumn(jooq, column);
  }

  static void executeRemoveRefAndNotNullConstraints(DSLContext jooq, Column column) {
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
    }
    executeSetNullable(jooq, column, true);
  }

  static String getSchemaName(Column column) {
    return column.getTable().getSchema().getName();
  }
}
