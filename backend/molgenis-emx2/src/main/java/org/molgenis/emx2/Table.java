package org.molgenis.emx2;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public interface Table {

  String getName();

  TableMetadata getMetadata();

  Schema getSchema();

  default int insert(Row... row) {
    return insert(Arrays.asList(row), Set.of());
  }

  default int insert(Iterable<Row> rows) {
    return insert(rows, Set.of());
  }

  int insert(Iterable<Row> rows, Set<String> columnsToInsert);

  default int update(Row... row) {
    return update(Arrays.asList(row), Set.of());
  }

  default int update(Iterable<Row> rows) { // wish list: update based on secondary key.
    return update(rows, Set.of());
  }

  /**
   * TODO: do we want to be able to change the columns to insert? Later down the line we have
   * hardcoded to pass all table columns into the update query, where as for insert/save, this is
   * set up more flexible.
   *
   * @see SqlTable#executeBatch
   */
  int update(Iterable<Row> rows, Set<String> columnsToInsert);

  default int save(Row... row) {
    return save(Arrays.asList(row), Set.of());
  }

  default int save(Iterable<Row> rows) {
    return save(rows, Set.of());
  }

  int save(Iterable<Row> rows, Set<String> columnsToInsert);

  default int delete(Row... rows) {
    return delete(Arrays.asList(rows));
  }

  default int delete(Iterable<Row> rows) {
    return delete(rows, false);
  }

  int delete(Iterable<Row> rows, boolean strict);

  void truncate();

  Query select(SelectColumn... columns);

  Query agg(SelectColumn columns);

  Query groupBy(SelectColumn columns);

  Query where(Filter... filters);

  Query search(String searchTerms);

  Query query();

  Query agg();

  Query groupBy();

  List<Row> retrieveRows(Query.Option... options);

  Table getInheritedTable();

  String getIdentifier();
}
