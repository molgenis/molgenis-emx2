package org.molgenis;

import java.util.List;
import java.util.UUID;

public interface QueryOld {

  QueryOld join(String table, String toTable, String on) throws MolgenisException;

  QueryOld select(String column) throws MolgenisException;

  QueryOld as(String alias) throws MolgenisException;

  QueryOld eq(String table, String column, UUID... value) throws MolgenisException;

  QueryOld eq(String table, String column, String... value) throws MolgenisException;

  QueryOld eq(String table, String column, Integer... value) throws MolgenisException;

  // TODO: other operators and type safe overloaded methods

  List<Row> retrieve() throws MolgenisException;
}
