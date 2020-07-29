package org.molgenis.emx2;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface Filter {

  String getColumn();

  Map<Operator, Object[]> getConditions();

  Filter getColumnFilter(String column);

  Collection<Filter> getColumnFilters();

  // Filter addCondition(String refColumn, Operator operator, Object... values);

  Filter addCondition(Operator operator, Object... values);

  Filter addCondition(Operator operator, List<?> values);

  Filter filter(Filter... filters);

  Filter filter(String refColumnName, Operator operator, Object... values);

  Filter filter(String refColumnName, Filter... refFilters);

  boolean has(String columnName);
}
