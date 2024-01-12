package org.molgenis.emx2;

import static org.molgenis.emx2.Operator.AND;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class SelectColumn {
  private String column;
  private Map<String, SelectColumn> children = new LinkedHashMap<>();
  private int limit = 0;
  private int offset = 0;
  private Filter filter;
  private Map<String, Order> orderBy = new LinkedHashMap<>();

  public SelectColumn(String column) {
    this.column = column;
    this.filter = new FilterBean(AND);
  }

  public SelectColumn(String column, String... subselects) {
    this(column);
    for (String subName : subselects) {
      children.put(subName, new SelectColumn(subName));
    }
  }

  public SelectColumn(String column, SelectColumn... subselects) {
    this(column);
    for (SelectColumn s : subselects) {
      this.children.put(s.getColumn(), s);
    }
  }

  public SelectColumn(String column, Collection<String> subselects) {
    this(column);
    this.select(subselects);
  }

  public static SelectColumn s(String column) {
    return new SelectColumn(column);
  }

  public static SelectColumn s(String column, SelectColumn... sub) {
    return new SelectColumn(column, sub);
  }

  public String getColumn() {
    return column;
  }

  public boolean has(String name) {
    return children.containsKey(name);
  }

  public SelectColumn getSubselect(String name) {
    return children.get(name);
  }

  public Collection<SelectColumn> getSubselect() {
    return children.values();
  }

  public Collection<String> getColumNames() {
    return children.keySet();
  }

  public void setLimit(int limit) {
    this.limit = limit;
  }

  public void setOffset(int offset) {
    this.offset = offset;
  }

  public int getLimit() {
    return limit;
  }

  public int getOffset() {
    return offset;
  }

  public void setOrderBy(Map<String, Order> values) {
    this.orderBy.putAll(values);
  }

  public Map<String, Order> getOrderBy() {
    return orderBy;
  }

  public SelectColumn subselect(SelectColumn... selects) {
    for (SelectColumn select : selects) {
      this.children.put(select.getColumn(), select);
    }
    return this;
  }

  public SelectColumn select(String... columnName) {
    for (String name : columnName) {
      this.children.put(name, s(name));
    }
    return this;
  }

  public SelectColumn select(Collection<String> columnName) {
    return this.select(columnName.toArray(new String[columnName.size()]));
  }

  public SelectColumn where(Filter... filters) {
    this.filter.addSubfilters(filters);
    return this;
  }

  public Filter getFilter() {
    return filter;
  }
}
