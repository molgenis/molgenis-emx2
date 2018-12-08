package org.molgenis.sql;

import java.util.List;
import java.util.UUID;

public interface SqlQuery {

    SqlQuery from(String table) throws SqlQueryException;

    SqlQuery join(String table, String toTable, String on) throws SqlQueryException;

    SqlQuery select(String column) throws SqlQueryException;

    SqlQuery as(String alias) throws SqlQueryException;

    SqlQuery eq(String table, String column, UUID... value) throws SqlQueryException;

    SqlQuery eq(String table, String column, String... value) throws SqlQueryException;

    SqlQuery eq(String table, String column, Integer... value) throws SqlQueryException;

    //TODO: other operators and type safe overloaded methods

    List<SqlRow> retrieve() throws SqlQueryException;
}
