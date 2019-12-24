package org.molgenis.emx2.sql;

import org.molgenis.emx2.Operator;
import org.molgenis.emx2.MolgenisException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Filter {
  private String field;
  private Map<Operator, Object[]> conditions = new LinkedHashMap<>();
  private Map<String, Filter> children = new LinkedHashMap<>();

  public static Filter f(String field, Filter... filters) {
    return new Filter(field, filters);
  }

  public static Filter f(String field, Operator equals, Object values) {
    return new Filter(field).add(equals, values);
  }

  public static Filter f(String field, Operator equals, Object... values) {
    return new Filter(field).add(equals, values);
  }

  public static Filter f(String field, Operator equals, List<Object> values) {
    return new Filter(field).add(equals, values);
  }

  public Filter(String field, Filter... children) {
    this.field = field;
    for (Filter f : children) {
      if (field != null && this.children.get(f.getField()) != null) {
        throw new MolgenisException(
            "Invalid filter", "already created filter for field " + f.getField());
      }
      this.children.put(f.getField(), f);
    }
  }

  public String getField() {
    return field;
  }

  public Filter getFilter(String name) {
    return this.children.get(name);
  }

  public Map<Operator, Object[]> getConditions() {
    return Collections.unmodifiableMap(conditions);
  }

  public Filter filter(Filter... filters) {
    for (Filter f : filters) {
      this.children.put(f.getField(), f);
    }
    return this;
  }

  protected boolean has(String columnName) {
    return this.children.containsKey(columnName);
  }

  public Filter add(Operator operator, Object... values) {
    this.conditions.put(operator, values);
    return this;
  }

  public Filter add(Operator operator, List<?> values) {
    this.conditions.put(operator, values.toArray());
    return this;
  }

  public Filter add(Operator operator, Object values) {
    this.conditions.put(operator, new Object[] {values});
    return this;
  }
}
