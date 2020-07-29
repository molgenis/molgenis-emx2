package org.molgenis.emx2;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class QueryBean implements Query {
  private SelectColumn select;
  private Filter filter;
  private String[] searchTerms = new String[0];

  public QueryBean() {
    this.select = new SelectColumn(null);
    this.filter = new FilterBean(null);
  }

  @Override
  public Query select(SelectColumn... columns) {
    this.select.select(columns);
    return this;
  }

  @Override
  public Query select(String... columns) {
    this.select.select(columns);
    return this;
  }

  @Override
  public String getColumn() {
    return null;
  }

  @Override
  public Map<Operator, Object[]> getConditions() {
    return filter.getConditions();
  }

  @Override
  public Filter getColumnFilter(String column) {
    return filter.getColumnFilter(column);
  }

  @Override
  public Query filter(Filter... filters) {
    this.filter.filter(filters);
    return this;
  }

  @Override
  public Query filter(String columnName, Operator operator, Object... values) {
    this.filter.filter(columnName, operator, values);
    return this;
  }

  @Override
  public Query filter(String name, Filter... filters) {
    this.filter.filter(name, filters);
    return this;
  }

  @Override
  public List<Row> getRows() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String retrieveJSON() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean has(String columnName) {
    return this.filter.has(columnName);
  }

  @Override
  public Filter addCondition(Operator operator, Object... values) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Filter addCondition(Operator operator, List<?> values) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<Filter> getColumnFilters() {
    return this.filter.getColumnFilters();
  }

  @Override
  public Query search(String... terms) {
    if (this.searchTerms == null || this.searchTerms.length == 0) {
      this.searchTerms = terms;
    } else {
      this.searchTerms =
          Stream.of(this.searchTerms, terms).flatMap(Stream::of).toArray(String[]::new);
    }
    return this;
  }

  @Override
  public Query select(Collection<String> columns) {
    return this.select(columns.toArray(new String[0]));
  }

  @Override
  public Query setLimit(int limit) {
    this.select.setLimit(limit);
    return this;
  }

  @Override
  public Query setOffset(int offset) {
    this.select.setOffset(offset);
    return this;
  }

  @Override
  public Filter getFilter() {
    return filter;
  }

  @Override
  public SelectColumn getSelect() {
    return select;
  }

  @Override
  public String[] getSearchTerms() {
    return searchTerms;
  }

  @Override
  public void setOrderBy(Map<String, Order> values) {
    this.select.setOrderBy(values);
  }

  @Override
  public Map<String, Order> getOrderBy() {
    return this.select.getOrderBy();
  }
}
