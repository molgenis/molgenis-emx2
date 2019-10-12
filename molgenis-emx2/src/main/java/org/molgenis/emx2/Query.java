package org.molgenis.emx2;

import org.molgenis.emx2.utils.MolgenisException;

import java.io.Serializable;
import java.util.List;

public interface Query {

  List<Sort> getSortList();

  List<Where> getWhereLists();

  List<String> getSearchList();

  Query select(String... columns);

  Query expand(String column);

  Query collapse();

  Query search(String terms);

  Query where(String path, Operator operator, Serializable... values);

  Query and(String path, Operator operator, Serializable... values);

  Query or(String path, Operator operator, Serializable... values);

  Query asc(String column);

  Query desc(String column);

  List<Row> retrieve();

  <E> List<E> retrieve(String columnName, Class<E> asClass);

  List<String> getSelectList();
}
