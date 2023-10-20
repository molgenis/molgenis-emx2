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

  int save(Row... row);

  int save(Iterable<Row> rows);

  int delete(Row... row);

  int delete(Iterable<Row> rows);

  void truncate();

  Query select(SelectColumn... columns);

  Query agg(SelectColumn columns);

  Query groupBy(SelectColumn columns);

  Query where(Filter... filters);

  Query search(String searchTerms);

  Query query();

  Query agg();

  Query groupBy();

  List<Row> retrieveRows();

  Table getInheritedTable();
}
