package org.molgenis.emx2;

import java.util.*;
import java.util.stream.Stream;

public class FilterBean implements Filter {
  private String column;
  private Map<Operator, Object[]> conditions = new LinkedHashMap<>();
  private Map<String, Filter> childFilters = new LinkedHashMap<>();

  public static Filter f(String columnName, Filter... filters) {
    return new FilterBean(columnName, filters);
  }

  public static Filter f(String columnName, Operator operator, Object... values) {
    return new FilterBean(columnName).addCondition(operator, values);
  }

  public static Filter f(String columnName, Operator operator, List<Object> values) {
    return new FilterBean(columnName).addCondition(operator, values);
  }

  public FilterBean(String columnName, Filter... childFilters) {
    this.column = columnName;
    for (Filter f : childFilters) {
      if (columnName != null && this.childFilters.get(f.getColumn()) != null) {
        throw new MolgenisException(
            "Invalid filter", "already created filter for field " + f.getColumn());
      }
      this.childFilters.put(f.getColumn(), f);
    }
  }

  @Override
  public String getColumn() {
    return column;
  }

  @Override
  public Filter getColumnFilter(String column) {
    return this.childFilters.get(column);
  }

  @Override
  public Map<Operator, Object[]> getConditions() {
    return Collections.unmodifiableMap(conditions);
  }

  @Override
  public Filter filter(Filter... filters) {
    for (Filter f : filters) {
      this.childFilters.put(f.getColumn(), f);
    }
    return this;
  }

  @Override
  public Filter filter(String columnName, Operator operator, Object... values) {
    if (this.childFilters.get(columnName) == null) {
      this.filter(f(columnName, operator, values));
    } else {
      Map<Operator, Object[]> con = this.childFilters.get(columnName).getConditions();
      if (con.containsKey(operator)) {
        con.put(operator, Stream.of(con.get(operator), values).flatMap(Stream::of).toArray());
      } else {
        this.childFilters.get(columnName).addCondition(operator, values);
      }
    }
    return this;
  }

  @Override
  public Filter filter(String columnName, Filter... subcolumnFilters) {
    return this.filter(f(columnName, subcolumnFilters));
  }

  @Override
  public boolean has(String columnName) {
    return this.childFilters.containsKey(columnName);
  }

  @Override
  public Filter addCondition(Operator operator, Object... values) {
    this.conditions.put(operator, values);
    return this;
  }

  @Override
  public Filter addCondition(Operator operator, List<?> values) {
    this.conditions.put(operator, values.toArray());
    return this;
  }

  @Override
  public Collection<Filter> getColumnFilters() {
    return childFilters.values();
  }
}
