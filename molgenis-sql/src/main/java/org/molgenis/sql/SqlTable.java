package org.molgenis.sql;

import java.util.Collection;

public interface SqlTable {
    String getName();

    SqlColumn getColumn(String name);

    Collection<SqlColumn> getColumns();

    Collection<SqlUnique> getUniques();

    SqlColumn addColumn(String name, SqlType type);

    SqlColumn addColumn(String name, SqlTable otherTable);

    SqlUnique addUnique(String ... name);

    //TODD: remove column, remove unique

    void insert(SqlRow row) throws SqlException;

    void insert(Collection<SqlRow> rows) throws SqlException;

    void delete(SqlRow row);

    void delete(Collection<SqlRow> rows);

    //TODO: update

    void validate(SqlRow row);
}
