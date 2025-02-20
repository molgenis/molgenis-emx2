package org.molgenis.emx2;

import java.util.List;
import java.util.Map;

public interface Query {
  enum Options {
    INCLUDE_FILE_CONTENTS,
    INCLUDE_MG_COLUMNS
  }

  Query select(SelectColumn... columns);

  Query where(Filter... filters);

  Query search(String... terms);

  Query limit(int limit);

  Query offset(int offset);

  Query orderBy(Map<String, Order> values);

  Query orderBy(String column);

  Query orderBy(String column, Order order);

  List<Row> retrieveRows(Options... options);

  String retrieveJSON();

  Filter getFilter();

  SelectColumn getSelect();

  String[] getSearchTerms();

  Map<String, Order> getOrderBy();
}
