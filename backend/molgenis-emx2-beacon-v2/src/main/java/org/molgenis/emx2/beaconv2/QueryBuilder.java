package org.molgenis.emx2.beaconv2;

import java.util.*;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Table;

public class QueryBuilder {

  private static final Set<String> EXCLUDED_COLUMNS = Set.of("parent", "children");

  private final Table table;
  private List<String> filters;
  private final StringBuilder columnSb;
  private StringBuilder query;
  private Integer limit;
  private Integer offset;

  public QueryBuilder(Table table) {
    this.table = table;
    this.filters = new ArrayList<>();
    this.columnSb = new StringBuilder();
  }

  public QueryBuilder addAllColumns(int maxDepth) {
    return this.addColumns(this.table.getMetadata().getColumnsWithoutHeadings(), maxDepth);
  }

  public QueryBuilder addColumns(List<Column> columns, int maxDepth) {
    Map<String, Integer> tableMinDepths = new HashMap<>();
    Set<String> visited = new HashSet<>();
    String rootTable = table.getName().toLowerCase();
    tableMinDepths.put(rootTable, 0);
    visited.add(rootTable);

    collectMinDepths(columns, 0, tableMinDepths, visited);
    buildQuery(columns, maxDepth, 0, tableMinDepths, new HashSet<>());

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
    query.append(table.getIdentifier());
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
    query.append(table.getIdentifier()).append("_agg");

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

  private void collectMinDepths(
      List<Column> columns,
      int currentDepth,
      Map<String, Integer> tableMinDepths,
      Set<String> visitedTables) {

    for (Column column : columns) {
      if (!column.isReference()
          || column.isOntology()
          || EXCLUDED_COLUMNS.contains(column.getName())) {
        continue;
      }

      String refTableName = column.getRefTableName().toLowerCase();
      Integer recordedDepth = tableMinDepths.get(refTableName);

      if (recordedDepth == null || currentDepth < recordedDepth) {
        tableMinDepths.put(refTableName, currentDepth);
        if (visitedTables.add(refTableName)) {
          collectMinDepths(
              column.getRefTable().getColumnsWithoutHeadings(),
              currentDepth + 1,
              tableMinDepths,
              visitedTables);
          visitedTables.remove(refTableName);
        }
      }
    }
  }

  private void buildQuery(
      List<Column> columns,
      int maxDepth,
      int currentDepth,
      Map<String, Integer> tableMinDepths,
      Set<String> includedTables) {

    for (Column column : columns) {
      String columnName = column.getName();

      if (EXCLUDED_COLUMNS.contains(columnName)) continue;

      if (column.isReference()) {
        String refTableName = column.getRefTableName().toLowerCase();

        if (column.isOntology()) {
          columnSb.append(column.getIdentifier()).append("{");
          for (Column subCol : column.getRefTable().getColumnsWithoutHeadings()) {
            if (!EXCLUDED_COLUMNS.contains(subCol.getName())) {
              columnSb.append(subCol.getIdentifier()).append(",");
            }
          }
          columnSb.append("},");
          continue;
        }

        Integer requiredDepth = tableMinDepths.get(refTableName);
        if (requiredDepth != null && requiredDepth == currentDepth && currentDepth < maxDepth) {
          if (includedTables.add(refTableName)) {
            columnSb.append(column.getIdentifier()).append("{");
            buildQuery(
                column.getRefTable().getColumnsWithoutHeadings(),
                maxDepth,
                currentDepth + 1,
                tableMinDepths,
                includedTables);
            columnSb.append("},");
          }
        }

      } else if (column.isFile()) {
        columnSb.append("%s { url },".formatted(column.getIdentifier()));
      } else {
        columnSb.append(column.getIdentifier()).append(",");
      }
    }
  }
}
