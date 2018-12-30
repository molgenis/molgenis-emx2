package org.molgenis.sql;

import java.util.List;

public interface SqlDatabase {

  SqlTable createTable(String name);

  List<SqlTable> getTables();

  SqlTable getTable(String name);

  void dropTable(String name);

  SqlQuery getQuery();

  void close();
}
