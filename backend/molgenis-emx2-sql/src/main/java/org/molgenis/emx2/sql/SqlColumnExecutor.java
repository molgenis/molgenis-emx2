package org.molgenis.emx2.sql;

import org.jooq.*;
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
        for (Reference ref : column.getReferences()) {
          executeSetNullable(
              jooq, column.getJooqTable(), ref.getJooqField(), ref.isNullable() || isNullable);
        }
        break;
      case FILE:
        for (Field f : column.getJooqFileFields()) {
          executeSetNullable(jooq, column.getJooqTable(), f, column.isNullable());
        }
        break;
      default:
        executeSetNullable(jooq, column.getJooqTable(), column.getJooqField(), isNullable);
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
            new String[] {"-id", "-extension", "-size", "-contents", "-mimetype"}) {
          jooq.execute(
              "ALTER TABLE {0} RENAME COLUMN {1} TO {2}",
              newColumn.getJooqTable(),
              field(name(oldColumn.getName() + suffix)),
              field(name(newColumn.getName() + suffix)));
        }
      } else if (newColumn.isReference()) {
        List<Reference> oldRef = oldColumn.getReferences();
        List<Reference> newRef = newColumn.getReferences();
        for (int i = 0; i < oldRef.size(); i++) {
          jooq.execute(
              "ALTER TABLE {0} RENAME COLUMN {1} TO {2}",
              newColumn.getJooqTable(),
              field(name(oldRef.get(i).getName())),
              field(name(newRef.get(i).getName())));
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

    if (FILE.equals(oldColumn.getColumnType()) && !FILE.equals(newColumn.getColumnType())
        || !FILE.equals(oldColumn.getColumnType()) && FILE.equals(newColumn.getColumnType())) {
      throw new MolgenisException(
          "Alter type for column '" + newColumn.getName() + "' failed",
          "Cannot convert from or to binary");
    }

    if (oldColumn.isReference() || newColumn.isReference()) {
      if (oldColumn.isReference() && newColumn.isReference()) {
        if (oldColumn.getReferences().size() != newColumn.getReferences().size()) {
          throw new MolgenisException(
              "Alter type for column '" + newColumn.getName() + "' failed",
              "Reference has different multiplicity");
        } else {
          List<Reference> oldRefs = oldColumn.getReferences();
          List<Reference> newRefs = newColumn.getReferences();
          for (int i = 0; i < oldRefs.size(); i++) {

            Field oldField = oldRefs.get(i).getJooqField();
            Field newField = newRefs.get(i).getJooqField();
            String postgresType = getPsqlType(newRefs.get(i).getColumnType());

            alterField(
                jooq,
                table,
                oldField.getName(),
                oldField.getDataType(),
                newField.getDataType(),
                postgresType);
          }
        }
      } else if (oldColumn.isReference()) {
        if (oldColumn.getReferences().size() > 1) {
          throw new MolgenisException(
              "Alter type for column '" + newColumn.getName() + "' failed",
              "Reference is composite relation and cannot be changed to "
                  + newColumn.getColumnType());
        }
        // if ref_array drop the index
        if (REF_ARRAY.equals(oldColumn.getColumnType())) {
          for (Reference ref : oldColumn.getReferences()) {
            //            if (!ref.isExisting()) {
            //              jooq.execute(
            //                  "DROP INDEX {0}",
            //                  name(oldColumn.getSchemaName(), table.getName() + "/" +
            // ref.getName()));
            //            }
          }
        }
        Field oldField = oldColumn.getReferences().get(0).getJooqField();
        alterField(
            jooq,
            table,
            oldField.getName(),
            oldField.getDataType(),
            newColumn.getJooqField().getDataType(),
            getPsqlType(newColumn));
      } else {
        if (newColumn.getReferences().size() > 1) {
          throw new MolgenisException(
              "Alter type for column '" + newColumn.getName() + "' failed",
              "Reference is composite relation and cannot be changed to "
                  + newColumn.getColumnType());
        }
        Field newField = newColumn.getReferences().get(0).getJooqField();
        String postgresType = getPsqlType(newColumn.getReferences().get(0).getColumnType());
        alterField(
            jooq,
            table,
            oldColumn.getName(),
            oldColumn.getJooqField().getDataType(),
            newField.getDataType(),
            postgresType);

        // if ref_array create the index
        //        if (REF_ARRAY.equals(newColumn.getColumnType())) {
        //          for (Reference ref : newColumn.getReferences()) {
        //            if (!ref.isExisting()) {
        //              executeCreateRefArrayIndex(jooq, table, ref);
        //            }
        //          }
        //        }
      }
    } else {
      alterField(
          jooq,
          table,
          oldColumn.getName(),
          oldColumn.getJooqField().getDataType(),
          newColumn.getJooqField().getDataType(),
          getPsqlType(newColumn));
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
      for (Reference ref : column.getReferences()) {
        if (!ref.isExisting()) {
          jooq.alterTable(column.getJooqTable()).addColumn(ref.getJooqField()).execute();
          // if ref_array to a string field we should index
          // doesn't seem to speed up
          //          if (REF_ARRAY.equals(column.getColumnType())) {
          //            executeCreateRefArrayIndex(jooq, column.getJooqTable(), ref);
          //          }
        }
      }
    } else if (FILE.equals(column.getColumnType())) {
      for (Field f : column.getJooqFileFields()) {
        jooq.alterTable(column.getJooqTable()).addColumn(f).execute();
      }
    } else {
      jooq.alterTable(column.getJooqTable()).addColumn(column.getJooqField()).execute();
    }

    // central constraints
    SqlTableMetadataExecutor.updateSearchIndexTriggerFunction(jooq, column.getTable());
    saveColumnMetadata(jooq, column);
  }

  private static void executeCreateRefArrayIndex(DSLContext jooq, Table table, Reference ref) {
    jooq.execute(
        "CREATE INDEX {0} ON {1} USING GIN( {2} )",
        name(table.getName() + "/" + ref.getName()), table, name(ref.getName()));
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
        // nothing to do
    }
    executeSetNullable(jooq, column, true);
  }

  static void updatePositions(Column column, TableMetadata existingTable) {
    if (column.getPosition() == null || column.getPosition() > existingTable.getColumns().size()) {
      column.position(existingTable.getLocalColumns().size());
    } else {
      // if needed move other columns positions
      for (Column c : existingTable.getLocalColumns()) {
        // check for position, don't update columns from parent
        if (c.getPosition() >= column.getPosition()
            && c.getTableName().equals(existingTable.getTableName())) {
          existingTable.alterColumn(c.getName(), c.position(c.getPosition() + 1));
        }
      }
    }
  }
}
