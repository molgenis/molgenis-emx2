package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.ColumnType.TEXT;
import static org.molgenis.emx2.Order.ASC;

import java.util.Map;
import org.jooq.*;
import org.jooq.Record;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Order;
import org.molgenis.emx2.Reference;
import org.molgenis.emx2.SelectColumn;
import org.molgenis.emx2.TableMetadata;

class SqlQueryBuilderHelpers {

  private SqlQueryBuilderHelpers() {
    // hide constructor
  }

  static SelectConnectByStep<Record> orderBy(
      TableMetadata table,
      SelectColumn select,
      SelectConnectByStep<org.jooq.Record> query,
      String tableAlias) {
    for (Map.Entry<String, Order> orderEntry : select.getOrderBy().entrySet()) {
      Column column = table.getQueryColumn(orderEntry.getKey());
      query = setOrderByForColumn(column, orderEntry.getValue(), query, tableAlias);
    }
    return query;
  }

  private static SelectJoinStep<Record> setOrderByForColumn(
      Column column, Order order, SelectConnectByStep<org.jooq.Record> query, String tableAlias) {
    if (column.isRefback()) {
      // Not supported. Feature request: https://github.com/molgenis/molgenis-emx2/issues/4021
    } else if (column.isReference()) {
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
    Field<?> collatedField =
        column.getColumnType().isStringyType()
            ? field.collate(unquotedName("\"MOLGENIS\".numeric"))
            : field;
    final SortField<?> sortField = ASC.equals(order) ? collatedField.asc() : collatedField.desc();
    return (SelectJoinStep<org.jooq.Record>) query.orderBy(sortField);
  }

  static boolean isCaseSensitiveField(Column column) {
    final ColumnType baseType = column.getColumnType().getBaseType();
    return baseType.equals(STRING) || baseType.equals(TEXT);
  }
}
