package org.molgenis.sql;

import java.util.Collection;

public interface SqlTable {
    String getName();

    SqlColumn getColumn(String name);

    Collection<SqlColumn> getColumns();

    Collection<SqlUnique> getUniques();

    SqlColumn addColumn(String name, SqlType type);

    SqlColumn addColumn(String name, SqlTable otherTable);

    SqlUnique addUnique(String ... name) throws SqlDatabaseException;

    //TODD: remove column, remove unique

    void insert(SqlRow row) throws SqlDatabaseException;

    void insert(Collection<SqlRow> rows) throws SqlDatabaseException;

    void delete(SqlRow row);

    void delete(Collection<SqlRow> rows);

    //TODO: update

    void validate(SqlRow row);
}
