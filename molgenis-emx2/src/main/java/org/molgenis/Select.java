package org.molgenis;

public interface Select {
  Select include(String column);

  Select avg(String column);

  Select expand(String column);

  Select select(String column);

  Query eq(String... values);

  Query eq(Integer... values);

  Query asc(String... path);

  Query desc(String... path);

  Where where(String... path);

  String[] getPath();

  public enum Aggregation {
    HIDDEN,
    EXPAND,
    AVG,
    SUM
  }
}
