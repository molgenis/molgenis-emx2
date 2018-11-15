package org.molgenis.sql;

public interface SqlDatabase {

    SqlTable createTable(String name);

    SqlTable getTable(String name);

    void dropTable(String name);

    SqlQuery getQuery();

    void close();


}
