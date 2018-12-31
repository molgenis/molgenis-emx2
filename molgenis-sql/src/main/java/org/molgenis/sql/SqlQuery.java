package org.molgenis.sql;

import java.util.List;
import java.util.UUID;

public interface SqlQuery {

  SqlQuery from(String table) throws SqlDatabaseException;

  SqlQuery join(String table, String toTable, String on) throws SqlDatabaseException;

  SqlQuery select(String column) throws SqlDatabaseException;

  SqlQuery as(String alias) throws SqlDatabaseException, SqlDatabaseException;

  SqlQuery eq(String table, String column, UUID... value) throws SqlDatabaseException;

  SqlQuery eq(String table, String column, String... value) throws SqlDatabaseException;

  SqlQuery eq(String table, String column, Integer... value) throws SqlDatabaseException;

  // TODO: other operators and type safe overloaded methods

  List<SqlRow> retrieve() throws SqlDatabaseException;
}
