package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.ColumnType.TEXT;
import static org.molgenis.emx2.Constants.TEXT_SEARCH_COLUMN_NAME;
import static org.molgenis.emx2.Order.ASC;
import static org.molgenis.emx2.sql.SqlTableMetadataExecutor.searchColumnName;

import java.util.List;
import java.util.Map;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
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
      TableMetadata table,
      SelectColumn select,
      SelectConnectByStep<org.jooq.Record> query,
      String tableAlias) {
    for (Map.Entry<String, Order> orderEntry : select.getOrderBy().entrySet()) {
      Column column = getColumnByName(table, orderEntry.getKey());
      query = setOderByForColumn(column, orderEntry.getValue(), query, tableAlias);
    }
    return query;
  }

  private static SelectJoinStep<Record> setOderByForColumn(
      Column column, Order order, SelectConnectByStep<org.jooq.Record> query, String tableAlias) {
    if (column.isRefback()) {
      // TODO, we now ignore sorting on refback, see issue
      // https://github.com/molgenis/molgenis-emx2/issues/4260
      // need to order by join with the refback table
      /*
      List<Condition> conditions =
          column.getRefBackColumn().getReferences().stream()
              .map(ref -> field(name(tableAlias, ref.getRefTo())).eq(field(ref.getName())))
              .toList();
      for (Reference ref : column.getReferences()) {
        Field<?> field =
            isCaseSensitiveField(column) ? lower(field(ref.getRefTo())) : ref.getJooqField();
        var collatedField =
            ref.getColumnType().isStringyType()
                ? field.collate(DSL.unquotedName("\"MOLGENIS\".numeric"))
                : field;
        field =
            DSL.field(
                DSL.select(DSL.max(field(collatedField)))
                    .from(column.getRefTable().getJooqTable())
                    .where(conditions));
        final SortField<?> sortField =
            ASC.equals(order) ? collatedField.asc() : collatedField.desc();
        query = (SelectJoinStep<org.jooq.Record>) query.orderBy(sortField);
      }*/
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
    var collatedField =
        column.getColumnType().isStringyType()
            ? field.collate(DSL.unquotedName("\"MOLGENIS\".numeric"))
            : field;
    final SortField<?> sortField = ASC.equals(order) ? collatedField.asc() : collatedField.desc();
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
