package org.molgenis.emx2;

import java.io.Serializable;
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

  Query select(String... path);

  Query where(String path, Operator operator, Serializable... values);

  Query search(String searchTerms);

  Query query();

  List<Row> retrieve();

  <E> List<E> retrieve(String columnName, Class<E> klazz);
}
