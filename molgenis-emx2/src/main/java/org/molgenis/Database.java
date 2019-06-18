package org.molgenis;

import java.util.Collection;

public interface Database {

  Schema getSchema() throws MolgenisException;

  QueryOld query(String name) throws MolgenisException;

  void insert(String table, Collection<Row> rows) throws MolgenisException;

  void insert(String table, Row row) throws MolgenisException;

  int update(String table, Collection<Row> rows) throws MolgenisException;

  void update(String table, Row row) throws MolgenisException;

  int delete(String table, Collection<Row> rows) throws MolgenisException;

  void delete(String table, Row row) throws MolgenisException;
}
