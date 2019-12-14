package org.molgenis.emx2.sql;

import org.molgenis.emx2.Operator;
import org.molgenis.emx2.utils.MolgenisException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Filter {
  private String field;
  private Map<Operator, Object[]> conditions = new LinkedHashMap<>();
  private Map<String, Filter> children = new LinkedHashMap<>();

  protected Filter(String field, Filter... children) {
    this.field = field;
    for (Filter f : children) {
      if (field != null && this.children.get(f.getField()) != null) {
        throw new MolgenisException("already created filter for field " + f.getField());
      }
      this.children.put(f.getField(), f);
    }
  }

  public static Filter f(String field, Filter... filters) {
    return new Filter(field, filters);
  }

  public Filter is(Object... values) {
    validate();
    this.conditions.put(Operator.EQUALS, values);
    return this;
  }

  public Filter similar(Object... values) {
    validate();
    this.conditions.put(Operator.TRIGRAM_SEARCH, values);
    return this;
  }

  private void validate() {
    if (children.size() > 0)
      throw new MolgenisException(
          "cannot eq filter on '" + this.field + "' when you also created sub filters");
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

  public void add(Operator operator, Object... values) {
    this.conditions.put(operator, values);
  }

  public void add(Operator operator, List<?> values) {
    this.conditions.put(operator, values.toArray());
  }

  public void add(Operator operator, Object values) {
    this.conditions.put(operator, new Object[] {values});
  }
}
