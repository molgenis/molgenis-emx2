package org.molgenis.emx2;

import org.molgenis.emx2.utils.MolgenisException;

import java.io.Serializable;
import java.util.List;

public interface Table {

  Schema getSchema();

  TableMetadata getMetadata();

  int insert(Row... row) throws MolgenisException;

  int insert(Iterable<Row> rows)
      throws MolgenisException; // consider use Iterable or Iterator instead?

  int update(Row... row) throws MolgenisException;

  int update(Iterable<Row> rows)
      throws MolgenisException; // wish list: update based on secondary key.

  int delete(Row... row) throws MolgenisException;

  int delete(Iterable<Row> rows) throws MolgenisException;

  void deleteByPrimaryKey(Object... name);

  Query select(String... path);

  Query where(String path, Operator operator, Serializable... values);

  Query query();

  List<Row> retrieve() throws MolgenisException;

  <E> List<E> retrieve(String columnName, Class<E> klazz) throws MolgenisException;

  String getName();
}
