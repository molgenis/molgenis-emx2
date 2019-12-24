package org.molgenis.emx2.sql;

import org.jooq.DSLContext;
import org.jooq.DataType;
import org.jooq.Field;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.TableMetadata;

import static org.jooq.impl.DSL.*;
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
}
