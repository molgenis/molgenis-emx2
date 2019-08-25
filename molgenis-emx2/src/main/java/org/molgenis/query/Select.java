package org.molgenis.query;

public interface Select {

  Select include(String column); // todo unclear, is part of expand

  Select avg(String column);

  Select expand(String... path);

  Select select(String column);

  Query eq(String... values);

  Query eq(Integer... values);

  // for fluent API also include table level operations

  Query asc(String... path);

  Query desc(String... path);

  Where where(String... path);

  String[] getPath(); // todo move to implementation?

  enum Aggregation {
    HIDDEN,
    EXPAND,
    AVG,
    SUM
  }
}
