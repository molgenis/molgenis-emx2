package org.molgenis.data;

import org.molgenis.MolgenisException;
import org.molgenis.metadata.TableMetadata;
import org.molgenis.query.Query;
import org.molgenis.query.Select;
import org.molgenis.query.Where;

import java.util.Collection;
import java.util.List;

public interface Table {

  Schema getSchema();

  TableMetadata getMetadata();

  int insert(Row... row) throws MolgenisException;

  int insert(Collection<Row> rows)
      throws MolgenisException; // todo: use Iterable or Iterator instead?

  int update(Row... row) throws MolgenisException;

  int update(Collection<Row> rows) throws MolgenisException; // todo: update based on secondary key.

  int delete(Row... row) throws MolgenisException;

  int delete(Collection<Row> rows) throws MolgenisException;

  void deleteByPrimaryKey(Object... name); // todo: remove?

  Select select(String... path);

  Where where(String... path);

  Query query();

  List<Row> retrieve() throws MolgenisException;

  <E> List<E> retrieve(String columnName, Class<E> klazz) throws MolgenisException;

  String getName();
}
