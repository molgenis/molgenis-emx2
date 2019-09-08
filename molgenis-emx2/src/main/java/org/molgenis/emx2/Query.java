package org.molgenis.emx2;

import org.molgenis.emx2.query.Sort;
import org.molgenis.emx2.utils.MolgenisException;

import java.util.List;

public interface Query {

  Select select(String... path);

  Select expand(String... column); // do we need this one?

  Query search(String terms);

  Query avg(String... path);

  Where where(String... path);

  Where and(String... path);

  Query or();

  Where or(String... path);

  Query asc(String... column);

  Query desc(String... column);

  List<Select> getSelectList(); // move to implementation?

  List<Where> getWhereLists(); // move to implementation

  List<Sort> getSortList(); // move to implementation

  List<Row> retrieve() throws MolgenisException;

  <E> List<E> retrieve(String columnName, Class<E> asClass);
}
