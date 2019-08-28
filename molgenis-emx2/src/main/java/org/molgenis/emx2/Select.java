package org.molgenis.emx2;

public interface Select {

  Select include(String column); // might be unclear, is part of expand

  Select avg(String column);

  Select expand(String... path);

  Select select(String column);

  Query eq(String... values);

  Query eq(Integer... values);

  // for fluent API also include table level operations

  Query asc(String... path);

  Query desc(String... path);

  Where where(String... path);

  String[] getPath();

  enum Aggregation {
    HIDDEN,
    EXPAND,
    AVG,
    SUM
  }
}
