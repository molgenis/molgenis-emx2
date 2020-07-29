package org.molgenis.emx2;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface Query { // extends Filter {

  Query select(SelectColumn... columns);

  Query where(Filter... filters);

  Query search(String... terms);

  Query limit(int limit);

  Query offset(int offset);

  void orderBy(Map<String, Order> values);

  List<Row> retrieveRows();

  String retrieveJSON();

  Filter getFilter();

  SelectColumn getSelect();

  String[] getSearchTerms();

  Map<String, Order> getOrderBy();
}
