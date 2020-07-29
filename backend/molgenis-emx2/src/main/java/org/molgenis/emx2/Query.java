package org.molgenis.emx2;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface Query extends Filter {

  Query select(SelectColumn... columns);

  Query select(String... columns);

  Query select(Collection<String> columns);

  Query search(String... terms);

  @Override
  Query filter(Filter... filters);

  @Override
  Query filter(String columName, Filter... subfilters);

  @Override
  Query filter(String columnName, Operator operator, Object... values);

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
