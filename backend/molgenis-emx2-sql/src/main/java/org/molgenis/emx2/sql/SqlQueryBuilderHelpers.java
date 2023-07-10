package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
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
    for (Map.Entry<String, Order> col : select.getOrderBy().entrySet()) {
      Column column = getColumnByName(table, col.getKey());
      query = setOderByForColumn(column, col, query);
    }
    return query;
  }

  private static SelectJoinStep<Record> setOderByForColumn(
      Column column, Map.Entry<String, Order> col, SelectConnectByStep<org.jooq.Record> query) {

    if (column.isReference()) {
      for (Reference ref : column.getReferences()) {
        final Field<Object> refField = field(name(ref.getName()));
        final SortField<Object> sortField =
            ASC.equals(col.getValue()) ? refField.asc() : refField.desc();
        query = (SelectJoinStep<Record>) query.orderBy(sortField);
      }
      return (SelectJoinStep<org.jooq.Record>) query;
    } else {
      final Field<Object> field = field(name(col.getKey()));
      final SortField<Object> sortField = ASC.equals(col.getValue()) ? field.asc() : field.desc();
      return (SelectJoinStep<org.jooq.Record>) query.orderBy(sortField);
    }
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
}
