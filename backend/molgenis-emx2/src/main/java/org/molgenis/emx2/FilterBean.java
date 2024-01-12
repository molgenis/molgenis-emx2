package org.molgenis.emx2;

import static org.molgenis.emx2.Constants.TEXT_SEARCH_COLUMN_NAME;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FilterBean implements Filter {
  private String column;
  private Operator operator;
  private Object[] values;
  private List<Filter> subFilters = new ArrayList<>();

  public static Filter or(Filter... filters) {
    return new FilterBean(Operator.OR, filters);
  }

  public static Filter or(List<Filter> filters) {
    return new FilterBean(Operator.OR, filters.toArray(new Filter[filters.size()]));
  }

  public static Filter and(Filter... filters) {
    return new FilterBean(Operator.AND, filters);
  }

  public static Filter and(List<Filter> filters) {
    return new FilterBean(Operator.AND, filters.toArray(new Filter[filters.size()]));
  }

  public static Filter f(String columnName, Filter... filters) {
    return new FilterBean(columnName, filters);
  }

  public static Filter f(String columnName, List<Filter> filters) {
    return new FilterBean(columnName, filters.toArray(new Filter[filters.size()]));
  }

  public static Filter f(String columnName, Operator operator, Object... values) {
    return new FilterBean(columnName, operator, values);
  }

  public static Filter f(Operator operator, Object... values) {
    // this will translate to search
    if (!Operator.TEXT_SEARCH.equals(operator)
        && !Operator.TRIGRAM_SEARCH.equals(operator)
        && !Operator.LIKE.equals(operator)) {
      throw new MolgenisException("Column missing for filter " + operator + " " + values);
    }
    return new FilterBean(TEXT_SEARCH_COLUMN_NAME, operator, values);
  }

  public static Filter f(String columnName, Operator operator, List<Object> values) {
    return new FilterBean(columnName, operator, values.toArray());
  }

  public FilterBean(String columnName, Operator operator, Object[] values) {
    this.column = columnName;
    this.operator = operator;
    this.values = values;
  }

  public FilterBean(Operator operator, Filter... childFilters) {
    this.operator = operator;
    if (!Operator.OR.equals(operator) && !Operator.AND.equals(operator))
      throw new MolgenisException(
          "Invalid filter: subquery filters without column name should have operator OR or AND. Other operators not yet supported");
    for (Filter f : childFilters) {
      this.subFilters.add(f);
    }
  }

  public FilterBean(String columnName, Filter... childFilters) {
    this.column = columnName;
    for (Filter f : childFilters) {
      if (columnName != null
          && this.subFilters.stream().filter(c -> c.getColumn().equals(columnName)).count() > 0) {
        throw new MolgenisException(
            "Invalid filter: already created filter for field '" + f.getColumn() + "'");
      }
      this.subFilters.add(f);
    }
  }

  @Override
  public String getColumn() {
    return column;
  }

  @Override
  public Filter getSubfilter(String column) {
    for (Filter f : this.subFilters) {
      if (column.equals(f.getColumn())) {
        return f;
      }
    }
    return null;
  }

  @Override
  public Filter addSubfilters(Filter... subfilters) {
    for (Filter f : subfilters) {
      this.subFilters.add(f);
    }
    return this;
  }

  @Override
  public Collection<Filter> getSubfilters() {
    return subFilters;
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
