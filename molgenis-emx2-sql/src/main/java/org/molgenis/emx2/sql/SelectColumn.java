package org.molgenis.emx2.sql;

import org.molgenis.emx2.Order;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class SelectColumn {
  private String column;
  private int limit = 0;
  private int offset = 0;
  private Map<String, SelectColumn> children = new LinkedHashMap<>();
  private Map<String, Order> orderBy = new LinkedHashMap<>();

  public SelectColumn(String column) {
    this.column = column;
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

  public static SelectColumn s(String column) {
    return new SelectColumn(column);
  }

  public static SelectColumn s(String column, SelectColumn... sub) {
    return new SelectColumn(column, sub);
  }

  String getColumn() {
    return column;
  }

  boolean has(String name) {
    return children.containsKey(name);
  }

  SelectColumn get(String name) {
    return children.get(name);
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

  public void orderBy(String column, Order order) {
    this.orderBy.put(column, order);
  }

  public void setOrderBy(Map<String, Order> values) {
    this.orderBy.putAll(values);
  }

  public Map<String, Order> getOrderBy() {
    return orderBy;
  }

  public void select(String columnName) {
    this.children.put(columnName, s(columnName));
  }
}
