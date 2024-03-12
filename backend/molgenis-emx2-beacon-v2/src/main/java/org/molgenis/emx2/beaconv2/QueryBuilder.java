package org.molgenis.emx2.beaconv2;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.beaconv2.requests.Filter;

public class QueryBuilder {

  private Table table;
  private List<Filter> filters;
  private List<Column> columns;
  private StringBuilder columnSb;
  private StringBuilder query;

  public QueryBuilder(Table table) {
    this.table = table;
    this.columnSb = new StringBuilder();
  }

  public QueryBuilder addColumns(int maxDepth) {
    this.addColumns(this.table.getMetadata().getColumnsWithoutHeadings(), maxDepth);
    return this;
  }

  public QueryBuilder addColumns(List<Column> columns, int maxDepth) {
    Set<String> seenTables = new HashSet<>();
    seenTables.add(columns.get(0).getTable().getIdentifier());
    int currentDepth = 0;
    queryColumnsRecursively(columns, seenTables, maxDepth, currentDepth);

    return this;
  }

  public QueryBuilder addFilters(List<Filter> filters) {
    return this;
  }

  public QueryBuilder addFilters(String[] filters) {
    return this;
  }

  public String getQuery() {
    query = new StringBuilder("{");
    query.append(table.getName());

    if (filters != null) addFilters();
    query.append("{");
    query.append(columnSb);
    query.append("}}");

    return query.toString();
  }

  private void addFilters() {
    query.append("(filter: { _and: [ ");
    for (Filter filter : filters) {
      query.append(filter.toString()).append(",");
    }
    if (!filters.isEmpty()) {
      query.deleteCharAt(query.length() - 1);
    }
    query.append(" ] }  )");
  }

  private int queryColumnsRecursively(
      List<Column> columns, Set<String> seenTables, int maxDepth, int currentDepth) {
    for (Column column : columns) {
      if (column.isOntology() || column.isReference()) {
        if (currentDepth < maxDepth) {
          TableMetadata refTable = column.getRefTable();
          // Don't select the same table twice // todo: Doesn't work for ontology tables
          //          if (seenTables.contains(refTable.getIdentifier())) continue;
          seenTables.add(refTable.getIdentifier());

          currentDepth++;
          columnSb.append(column.getIdentifier()).append("{");
          currentDepth =
              queryColumnsRecursively(
                  refTable.getColumnsWithoutHeadings(), seenTables, maxDepth, currentDepth);
          columnSb.append("}");
        }
      } else {
        columnSb.append(column.getIdentifier()).append(",");
      }
    }
    currentDepth--;
    return currentDepth;
  }
}
