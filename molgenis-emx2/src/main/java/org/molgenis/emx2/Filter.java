package org.molgenis.emx2;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface Filter {

  String getField();

  Map<Operator, Object[]> getConditions();

  Filter getFilter(String name);

  Filter filter(Filter... filters);

  Filter filter(String columnName, Operator operator, Serializable... values);

  Filter filter(String columName, Filter... subfilters);

  boolean has(String columnName);

  Filter add(Operator operator, Object... values);

  Filter add(Operator operator, List<?> values);

  Filter filter(String refColumnName, Map<Operator, Object[]> conditions);

  Collection<Filter> getSubfilters();
}
