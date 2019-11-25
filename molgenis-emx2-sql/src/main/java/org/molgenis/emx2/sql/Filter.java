package org.molgenis.emx2.sql;

import org.molgenis.emx2.Operator;

import java.util.LinkedHashMap;
import java.util.Map;

public class Filter {

  private String field;
  private Operator operator;
  private Object[] values;
  private Map<String, Filter> filters = new LinkedHashMap<>();

  private Filter(String field, Filter... filters) {
    this.field = field;
    for (Filter f : filters) {
      if (field != null && this.filters.get(f.getField()) != null) {
        throw new RuntimeException("already created filter for field " + f.getField());
      }
      this.filters.put(f.getField(), f);
    }
  }

  public static Filter f(String field, Filter... filters) {
    return new Filter(field, filters);
  }

  public Filter eq(Object... values) {
    if (filters.size() > 0)
      throw new RuntimeException(
          "cannot eq filter on '" + this.field + "' when you also created sub filters");
    this.operator = Operator.EQUALS;
    this.values = values;
    return this;
  }

  public String getField() {
    return field;
  }

  public Filter getFilter(String name) {
    return this.filters.get(name);
  }

  public Operator getOperator() {
    return this.operator;
  }

  public Iterable<? extends Filter> getFilters() {
    return this.filters.values();
  }

  public Object[] getValues() {
    return this.values;
  }
}
