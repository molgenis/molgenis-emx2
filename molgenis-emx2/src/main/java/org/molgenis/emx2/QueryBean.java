package org.molgenis.emx2;

import java.io.Serializable;
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

  public Query select(SelectColumn... select) {
    this.select.select(select);
    return this;
  }

  public Query select(String... select) {
    this.select.select(select);
    return this;
  }

  public String getField() {
    return null;
  }

  @Override
  public Map<Operator, Object[]> getConditions() {
    return filter.getConditions();
  }

  public Filter getFilter(String name) {
    return filter.getFilter(name);
  }

  public Query filter(Filter... filters) {
    this.filter.filter(filters);
    return this;
  }

  public Query filter(String columnName, Operator operator, Serializable... values) {
    this.filter.filter(columnName, operator, values);
    return this;
  }

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

  public boolean has(String columnName) {
    return this.filter.has(columnName);
  }

  public Filter add(Operator operator, Object... values) {
    throw new UnsupportedOperationException();
  }

  public Filter add(Operator operator, List<?> values) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Filter filter(String columnName, Map<Operator, Object[]> conditions) {
    return this.filter.filter(columnName, conditions);
  }

  @Override
  public Collection<Filter> getSubfilters() {
    return this.filter.getSubfilters();
  }

  public Query search(String... terms) {
    if (this.searchTerms == null || this.searchTerms.length == 0) {
      this.searchTerms = terms;
    } else {
      this.searchTerms =
          Stream.of(this.searchTerms, terms).flatMap(Stream::of).toArray(String[]::new);
    }
    return this;
  }

  public Query select(Collection<String> columnNames) {
    return this.select(columnNames.toArray(new String[0]));
  }

  public Query setLimit(int limit) {
    this.select.setLimit(limit);
    return this;
  }

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
