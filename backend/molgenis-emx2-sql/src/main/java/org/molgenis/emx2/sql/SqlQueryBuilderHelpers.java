package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.lower;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.ColumnType.TEXT;
import static org.molgenis.emx2.Constants.TEXT_SEARCH_COLUMN_NAME;
import static org.molgenis.emx2.Order.ASC;
import static org.molgenis.emx2.sql.SqlTableMetadataExecutor.searchColumnName;

import java.util.List;
import java.util.Map;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectConnectByStep;
import org.jooq.SelectJoinStep;
import org.jooq.SortField;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Order;
import org.molgenis.emx2.Reference;
import org.molgenis.emx2.SelectColumn;
import org.molgenis.emx2.TableMetadata;

class SqlQueryBuilderHelpers {

  private SqlQueryBuilderHelpers() {
    // hide constructor
  }

  static SelectConnectByStep<Record> orderBy(
      TableMetadata table, SelectColumn select, SelectConnectByStep<org.jooq.Record> query) {
    for (Map.Entry<String, Order> orderEntry : select.getOrderBy().entrySet()) {
      Column column = getColumnByName(table, orderEntry.getKey());
      query = setOderByForColumn(column, orderEntry.getValue(), query);
    }
    return query;
  }

  private static SelectJoinStep<Record> setOderByForColumn(
      Column column, Order order, SelectConnectByStep<org.jooq.Record> query) {

    if (column.isReference()) {
      for (Reference ref : column.getReferences()) {
        final Column refColumn = ref.toPrimitiveColumn();
        query = setOrderByForColumn(refColumn, order, query);
      }
    } else {
      query = setOrderByForColumn(column, order, query);
    }
    return (SelectJoinStep<org.jooq.Record>) query;
  }

  private static SelectJoinStep<Record> setOrderByForColumn(
      Column column, Order order, SelectConnectByStep<org.jooq.Record> query) {
    final Field<?> field =
        isCaseSensitiveField(column) ? lower(column.getJooqField()) : column.getJooqField();
    final SortField<?> sortField = ASC.equals(order) ? field.asc() : field.desc();
    return (SelectJoinStep<org.jooq.Record>) query.orderBy(sortField);
  }

  static Column getColumnByName(TableMetadata table, String columnName) {
    // is search?
    if (TEXT_SEARCH_COLUMN_NAME.equals(columnName)) {
      return new Column(table, searchColumnName(table.getTableName()));
    }
    // is scalar column
    Column column = table.getColumn(columnName);
    if (column == null) {
      // is reference?
      final List<Column> columns = table.getColumns();
      for (Column c : columns) {
        for (Reference ref : c.getReferences()) {
          // can also request composite reference columns, can only be used on row level queries
          if (ref.getName().equals(columnName)) {
            return new Column(table, columnName, true).setType(ref.getPrimitiveType());
          }
        }
      }
      // is file?
      for (Column c : columns) {
        if (c.isFile()
            && columnName.startsWith(c.getName())
            && (columnName.equals(c.getName())
                || columnName.endsWith("_mimetype")
                || columnName.endsWith("_filename")
                || columnName.endsWith("_extension")
                || columnName.endsWith("_size")
                || columnName.endsWith("_contents"))) {
          return new Column(table, columnName);
        }
      }
      throw new MolgenisException(
          "Query failed: Column '" + columnName + "' is unknown in table " + table.getTableName());
    }
    return column;
  }

  static boolean isCaseSensitiveField(Column column) {
    final ColumnType baseType = column.getColumnType().getBaseType();
    return baseType.equals(STRING) || baseType.equals(TEXT);
  }
}
