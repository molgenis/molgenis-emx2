package org.molgenis.emx2.beaconv2;

import java.util.ArrayList;
import java.util.List;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.TableMetadata;

public class QueryBuilder {

  private Table table;
  private List<String> filters;
  private StringBuilder columnSb;
  private StringBuilder query;
  private Integer limit;
  private Integer offset;

  public QueryBuilder(Table table) {
    this.table = table;
    this.filters = new ArrayList<>();
    this.columnSb = new StringBuilder();
  }

  public QueryBuilder addAllColumns(int maxDepth) {
    this.addColumns(this.table.getMetadata().getColumnsWithoutHeadings(), maxDepth);
    return this;
  }

  public QueryBuilder addColumns(List<Column> columns, int maxDepth) {
    int currentDepth = 0;
    queryColumnsRecursively(columns, maxDepth, currentDepth);

    return this;
  }

  public QueryBuilder addFilters(List<String> filters) {
    this.filters = filters;
    return this;
  }

  public QueryBuilder setLimit(int limit) {
    this.limit = limit;
    return this;
  }

  public QueryBuilder setOffset(int offset) {
    this.offset = offset;
    return this;
  }

  public String getQuery() {
    query = new StringBuilder("{");
    query.append(table.getName());
    if (hasFilterArguments()) {
      query.append("(");
      if (limit != null) query.append("limit: ").append(limit).append(",");
      if (offset != null) query.append("offset: ").append(offset).append(",");
      if (filters != null && !filters.isEmpty()) addFilters();
      query.append(")");
    }
    query.append("{");
    query.append(columnSb);
    query.append("}}");

    return query.toString();
  }

  public String getCountQuery() {
    return getAggregateQuery("count");
  }

  public String getExistsQuery() {
    return getAggregateQuery("exists");
  }

  private String getAggregateQuery(String variable) {
    query = new StringBuilder("{");
    query.append(table.getName()).append("_agg");

    if (filters != null && !filters.isEmpty()) {
      query.append("(");
      addFilters();
      query.append(")");
    }
    query.append("{ ").append(variable).append(" }}");
    return query.toString();
  }

  private boolean hasFilterArguments() {
    return limit != null || offset != null || (filters != null && !filters.isEmpty());
  }

  private void addFilters() {
    addFilters("and");
  }

  private void addFilters(String operator) {
    query.append("filter: { _%s: [".formatted(operator));
    for (String filter : filters) {
      query.append(filter).append(",");
    }
    if (!filters.isEmpty()) {
      query.deleteCharAt(query.length() - 1);
    }
    query.append("] }");
  }

  private int queryColumnsRecursively(List<Column> columns, int maxDepth, int currentDepth) {
    for (Column column : columns) {
      if (column.isReference()) {
        if (currentDepth < maxDepth) {
          TableMetadata refTable = column.getRefTable();

          currentDepth++;
          columnSb.append(column.getIdentifier()).append("{");
          currentDepth =
              queryColumnsRecursively(refTable.getColumnsWithoutHeadings(), maxDepth, currentDepth);
          columnSb.append("}");
        }
      } else if (column.isFile()) {
        columnSb.append("%s { url },".formatted(column.getIdentifier()));
      } else {
        columnSb.append(column.getIdentifier()).append(",");
      }
    }
    currentDepth--;
    return currentDepth;
  }
}
