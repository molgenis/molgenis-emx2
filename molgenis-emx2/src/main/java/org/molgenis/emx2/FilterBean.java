package org.molgenis.emx2;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Stream;

public class FilterBean implements Filter {
  private String field;
  private Map<Operator, Object[]> conditions = new LinkedHashMap<>();
  private Map<String, Filter> children = new LinkedHashMap<>();

  public static Filter f(String field, Filter... filters) {
    return new FilterBean(field, filters);
  }

  public static Filter f(String field, Operator equals, Object values) {
    return new FilterBean(field).add(equals, values);
  }

  public static Filter f(String field, Operator equals, Object... values) {
    return new FilterBean(field).add(equals, values);
  }

  public static Filter f(String field, Operator equals, List<Object> values) {
    return new FilterBean(field).add(equals, values);
  }

  public FilterBean(String field, Filter... children) {
    this.field = field;
    for (Filter f : children) {
      if (field != null && this.children.get(f.getField()) != null) {
        throw new MolgenisException(
            "Invalid filter", "already created filter for field " + f.getField());
      }
      this.children.put(f.getField(), f);
    }
  }

  @Override
  public String getField() {
    return field;
  }

  @Override
  public Filter getFilter(String name) {
    return this.children.get(name);
  }

  @Override
  public Map<Operator, Object[]> getConditions() {
    return Collections.unmodifiableMap(conditions);
  }

  @Override
  public Filter filter(Filter... filters) {
    for (Filter f : filters) {
      this.children.put(f.getField(), f);
    }
    return this;
  }

  @Override
  public Filter filter(String columnName, Operator operator, Serializable... values) {
    if (this.children.get(columnName) == null) {
      this.filter(f(columnName, operator, values));
    } else {
      Map<Operator, Object[]> con = this.children.get(columnName).getConditions();
      if (con.containsKey(operator)) {
        con.put(operator, Stream.of(con.get(operator), values).flatMap(Stream::of).toArray());
      } else {
        this.children.get(columnName).add(operator, values);
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
    return this.children.containsKey(columnName);
  }

  @Override
  public Filter add(Operator operator, Object... values) {
    this.conditions.put(operator, values);
    return this;
  }

  @Override
  public Filter add(Operator operator, List<?> values) {
    this.conditions.put(operator, values.toArray());
    return this;
  }

  @Override
  public Filter filter(String columnName, Map<Operator, Object[]> conditions) {
    for (Map.Entry<Operator, Object[]> con : conditions.entrySet()) {
      this.filter(columnName, con.getKey(), con.getValue());
    }
    return this;
  }

  @Override
  public Collection<Filter> getSubfilters() {
    return children.values();
  }
}
