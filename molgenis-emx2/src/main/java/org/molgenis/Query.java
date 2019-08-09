package org.molgenis;

import java.util.List;

public interface Query {

  Select select(String... path);

  Select expand(String... column);

  Query search(String terms);

  Query avg(String... path);

  Query sum(String... path);

  Where where(String... path);

  Where and(String... path);

  Query or();

  Where or(String... path);

  Query asc(String... column);

  Query desc(String... column);

  List<Select> getSelectList();

  List<Where> getWhereLists();

  List<Sort> getSortList();

  List<Row> retrieve() throws MolgenisException;

  <E> List<E> retrieve(String columnName, Class<E> asClass);
}
