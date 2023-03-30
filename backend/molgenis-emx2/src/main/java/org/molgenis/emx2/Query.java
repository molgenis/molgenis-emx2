package org.molgenis.emx2;

import java.util.List;
import java.util.Map;

public interface Query {

    Query select(SelectColumn... columns);

    Query where(Filter... filters);

    Query search(String... terms);

    Query limit(int limit);

    Query offset(int offset);

    Query orderBy(Map<String, Order> values);

    Query orderBy(String column);

    Query orderBy(String column, Order order);

    Query secondsTimeout(Integer second);

    List<Row> retrieveRows();

    String retrieveJSON();

    Filter getFilter();

    SelectColumn getSelect();

    String[] getSearchTerms();

    Map<String, Order> getOrderBy();

    Integer getSecondsTimeout();

}
