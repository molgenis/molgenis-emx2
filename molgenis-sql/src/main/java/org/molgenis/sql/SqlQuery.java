package org.molgenis.sql;

import java.util.List;
import java.util.UUID;

public interface SqlQuery {

    SqlQuery from(String table);

    SqlQuery join(String table, String toTable, String on);

    SqlQuery select(String column);

    SqlQuery as(String alias);

    SqlQuery eq(String table, String column, UUID... value);

    SqlQuery eq(String table, String column, String... value);

    SqlQuery eq(String table, String column, Integer... value);

    //TODO: other operators and type safe overloaded methods

    List<SqlRow> retrieve();
}
