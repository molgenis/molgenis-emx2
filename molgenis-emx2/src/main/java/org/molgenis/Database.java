package org.molgenis;

import java.util.Collection;

public interface Database {

  Schema getSchema() throws DatabaseException;

  Query query(String name) throws DatabaseException;

  void insert(String table, Collection<Row> rows) throws DatabaseException;

  void insert(String table, Row row) throws DatabaseException;

  int update(String table, Collection<Row> rows) throws DatabaseException;

  void update(String table, Row row) throws DatabaseException;

  int delete(String table, Collection<Row> rows) throws DatabaseException;

  void delete(String table, Row row) throws DatabaseException;
}
