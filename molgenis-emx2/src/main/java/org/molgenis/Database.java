package org.molgenis;

import java.util.Collection;

public interface Database {

  Schema getSchema() throws MolgenisException;

  void transaction(Transaction transaction) throws MolgenisException;

  int insert(String table, Collection<Row> rows) throws MolgenisException;

  int insert(String table, Row... row) throws MolgenisException;

  int update(String table, Row... row) throws MolgenisException;

  int update(String table, Collection<Row> rows) throws MolgenisException;

  int delete(String table, Row... row) throws MolgenisException;

  int delete(String table, Collection<Row> rows) throws MolgenisException;

  Query query(String name) throws MolgenisException;
}
