package org.molgenis.emx2;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface Query extends Filter {

  Query select(SelectColumn... select);

  Query select(String... select);

  Query select(Collection<String> columnNames);

  @Override
  boolean has(String columnName);

  @Override
  Filter add(Operator operator, Object... values);

  @Override
  Filter add(Operator operator, List<?> values);

  Query search(String... terms);

  @Override
  String getField();

  @Override
  Map<Operator, Object[]> getConditions();

  @Override
  Filter getFilter(String name);

  @Override
  Query filter(Filter... filters);

  @Override
  Query filter(String columName, Filter... subfilters);

  @Override
  Query filter(String columnName, Operator operator, Serializable... values);

  List<Row> getRows();

  String retrieveJSON();

  Query setLimit(int limit);

  Query setOffset(int offset);

  Filter getFilter();

  SelectColumn getSelect();

  String[] getSearchTerms();

  void setOrderBy(Map<String, Order> values);

  Map<String, Order> getOrderBy();
}
