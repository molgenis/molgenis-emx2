package org.molgenis;

import java.util.Collection;
import java.util.List;

public interface Query {
  List<Select> getSelectList();

  List<Where> getWhereLists();

  Select select(String... path);

  Select expand(String column);

  Query avg(String... path);

  Query sum(String... path);

  Where where(String... path);

  Where and(String... path);

  Where or(String... path);

  Query asc(String... column);

  Query desc(String... column);

  Collection<Sort> getSortList();
}
