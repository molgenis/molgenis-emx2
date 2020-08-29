package org.molgenis.emx2;

import java.util.List;

public interface Table {

  String getName();

  TableMetadata getMetadata();

  Schema getSchema();

  int insert(Row... row);

  int insert(Iterable<Row> rows);

  int update(Row... row);

  int update(Iterable<Row> rows); // wish list: update based on secondary key.

  int delete(Row... row);

  int delete(Iterable<Row> rows);

  Query select(SelectColumn... columns);

  //  Query filter(String path, Operator operator, Object... values);

  Query agg(SelectColumn columns);

  Query where(Filter... filters);

  Query search(String searchTerms);

  Query query();

  Query agg();

  List<Row> retrieveRows();
}
