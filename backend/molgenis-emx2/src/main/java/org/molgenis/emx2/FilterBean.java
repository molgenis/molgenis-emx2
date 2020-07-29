package org.molgenis.emx2;

import java.util.*;

public class FilterBean implements Filter {
  private String column;
  private Operator operator;
  private Object[] values;
  private Map<String, Filter> subFilters = new LinkedHashMap<>();

  public static Filter f(String columnName, Filter... filters) {
    return new FilterBean(columnName, filters);
  }

  public static Filter f(String columnName, Operator operator, Object... values) {
    return new FilterBean(columnName, operator, values);
  }

  public static Filter f(String columnName, Operator operator, List<Object> values) {
    return new FilterBean(columnName, operator, values.toArray());
  }

  public FilterBean(String columnName, Operator operator, Object[] values) {
    this.column = columnName;
    this.operator = operator;
    this.values = values;
  }

  public FilterBean(String columnName, Filter... childFilters) {
    this.column = columnName;
    for (Filter f : childFilters) {
      if (columnName != null && this.subFilters.get(f.getColumn()) != null) {
        throw new MolgenisException(
            "Invalid filter", "already created filter for field " + f.getColumn());
      }
      this.subFilters.put(f.getColumn(), f);
    }
  }

  @Override
  public String getColumn() {
    return column;
  }

  @Override
  public Filter getSubfilter(String column) {
    return this.subFilters.get(column);
  }

  @Override
  public Filter subfilter(Filter... subfilters) {
    for (Filter f : subfilters) {
      this.subFilters.put(f.getColumn(), f);
    }
    return this;
  }

  @Override
  public boolean has(String columnName) {
    return this.subFilters.containsKey(columnName);
  }

  @Override
  public Collection<Filter> getSubfilter() {
    return subFilters.values();
  }

  @Override
  public Operator getOperator() {
    return operator;
  }

  @Override
  public Object[] getValues() {
    return values;
  }
}
