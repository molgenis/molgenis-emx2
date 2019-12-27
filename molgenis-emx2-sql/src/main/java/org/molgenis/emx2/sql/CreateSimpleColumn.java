package org.molgenis.emx2.sql;

import org.jooq.DSLContext;
import org.jooq.DataType;
import org.jooq.Field;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.TableMetadata;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.sql.CreateMrefColumn.createMrefColumn;
import static org.molgenis.emx2.sql.CreateRefArrayColumn.createRefArrayColumn;
import static org.molgenis.emx2.sql.CreateRefArrayColumn.executeDropRefArrayTriggers;
import static org.molgenis.emx2.sql.CreateRefBackColumn.createRefBackColumn;
import static org.molgenis.emx2.sql.CreateRefBackColumn.executeDropRefbackTriggers;
import static org.molgenis.emx2.sql.CreateRefColumn.createRefColumn;
import static org.molgenis.emx2.sql.MetadataUtils.saveColumnMetadata;
import static org.molgenis.emx2.sql.SqlTypeUtils.jooqTypeOf;

public class CreateSimpleColumn {

  static void createSimpleColumn(DSLContext jooq, Column column) {
    jooq.alterTable(asJooqTable(column.getTable())).addColumn(asJooqField(column)).execute();
    // todo set indexed
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
    // todo might be too long, i.e. 64 chars
    return "MREF_" + column.getTable().getTableName() + "_" + column.getName();
  }

  protected static Column getMappedByColumn(Column column) {
    return getRefTable(column).getColumn(column.getMappedBy());
  }

  static void executeCreateColumn(DSLContext jooq, Column column) {
    switch (column.getColumnType()) {
      case REF:
        createRefColumn(jooq, column);
        executeSetNullable(jooq, column);
        break;
      case REF_ARRAY:
        createRefArrayColumn(jooq, column);
        executeSetNullable(jooq, column);
        break;
      case REFBACK:
        createRefBackColumn(jooq, column);
        break;
      case MREF:
        createMrefColumn(jooq, column);
        break;
      default:
        createSimpleColumn(jooq, column);
        executeSetNullable(jooq, column);
    }
    // central constraints
    if (column.isPrimaryKey())
      SqlTableMetadataUtils.executeSetPrimaryKey(jooq, column.getTable(), column.getName());
    SqlTableMetadataUtils.updateSearchIndexTriggerFunction(jooq, column.getTable());
    saveColumnMetadata(jooq, column);
  }

  static void executeRemoveColumn(DSLContext jooq, Column column) {
    // remove triggers
    switch (column.getColumnType()) {
      case REF:
        // nothing to do
        break;
      case REF_ARRAY:
        executeDropRefArrayTriggers(jooq, column);
        break;
      case REFBACK:
        executeDropRefbackTriggers(jooq, column);
        break;
      case MREF:
        // executeDropMrefTriggers(jooq, column);
        break;
      default:
        // nothing else?
    }

    jooq.alterTable(SqlTableMetadataUtils.getJooqTable(column.getTable()))
        .dropColumn(field(name(column.getName())))
        .execute();
    SqlTableMetadataUtils.updateSearchIndexTriggerFunction(jooq, column.getTable());
    MetadataUtils.deleteColumn(jooq, column);
  }
}
