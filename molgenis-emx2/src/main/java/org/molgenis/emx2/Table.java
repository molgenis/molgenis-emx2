package org.molgenis.emx2;

import org.molgenis.emx2.utils.MolgenisException;

import java.io.Serializable;
import java.util.List;

public interface Table {

  Schema getSchema();

  TableMetadata getMetadata();

  int insert(Row... row);

  int insert(Iterable<Row> rows); // consider use Iterable or Iterator instead?

  int update(Row... row);

  int update(Iterable<Row> rows); // wish list: update based on secondary key.

  int delete(Row... row);

  int delete(Iterable<Row> rows);

  void deleteByPrimaryKey(Object... name);

  Query select(String... path);

  Query where(String path, Operator operator, Serializable... values);

  Query query();

  List<Row> retrieve();

  <E> List<E> retrieve(String columnName, Class<E> klazz);

  String getName();
}
