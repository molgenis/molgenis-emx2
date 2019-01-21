package org.molgenis.emx2.database;

import org.molgenis.emx2.EmxException;
import org.molgenis.emx2.EmxModel;
import org.molgenis.sql.SqlDatabaseException;
import org.molgenis.sql.SqlQuery;
import org.molgenis.sql.SqlRow;

import java.util.Collection;
import java.util.UUID;

public interface EmxDatabase {
  EmxModel getModel();

  SqlQuery query(String tableName) throws SqlDatabaseException;

  SqlRow findById(String tableName, UUID id);

  void save(String tableName, SqlRow row) throws SqlDatabaseException;

  int save(String tableName, Collection<SqlRow> rows) throws SqlDatabaseException;

  int delete(String tableName, Collection<SqlRow> rows) throws SqlDatabaseException;

  void delete(String tableName, SqlRow row) throws SqlDatabaseException;
}
