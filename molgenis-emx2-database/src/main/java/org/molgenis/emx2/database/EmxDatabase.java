package org.molgenis.emx2.database;

import org.molgenis.emx2.EmxException;
import org.molgenis.emx2.EmxFilter;
import org.molgenis.sql.SqlRow;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Stream;

public interface EmxDatabase {
  Stream<EmxRow> find(String tableName, EmxFilter... filters);

  EmxRow findById(String tableName, UUID id);

  void save(String tableName, SqlRow row) throws EmxException;

  int save(String tableName, Collection<SqlRow> rows) throws EmxException;

  int delete(String tableName, Collection<SqlRow> rows) throws EmxException;

  void delete(String tableName, SqlRow row) throws EmxException;
}
