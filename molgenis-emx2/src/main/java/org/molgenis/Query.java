package org.molgenis;

import java.util.List;
import java.util.UUID;

public interface Query {

  Query join(String table, String toTable, String on) throws DatabaseException;

  Query select(String column) throws DatabaseException;

  Query as(String alias) throws DatabaseException;

  Query eq(String table, String column, UUID... value) throws DatabaseException;

  Query eq(String table, String column, String... value) throws DatabaseException;

  Query eq(String table, String column, Integer... value) throws DatabaseException;

  // TODO: other operators and type safe overloaded methods

  List<Row> retrieve() throws DatabaseException;
}
