package org.molgenis.sql;

import java.util.Collection;

public interface SqlTable {
    String getName();

    Collection<SqlColumn> getColumns();

    SqlColumn getColumn(String name);

    SqlColumn addColumn(String name, SqlType type);

    SqlColumn addColumn(String name, SqlTable otherTable);

    void removeColumn(String name) throws SqlDatabaseException;

    Collection<SqlUnique> getUniques();

    SqlUnique addUnique(String ... name) throws SqlDatabaseException;

    void removeUnique(String ... name) throws SqlDatabaseException;

    void insert(Collection<SqlRow> rows) throws SqlDatabaseException;

    void update(Collection<SqlRow> rows) throws SqlDatabaseException;

    void delete(Collection<SqlRow> rows);

    void insert(SqlRow rows) throws SqlDatabaseException;

    void update(SqlRow rows) throws SqlDatabaseException;

    void delete(SqlRow rows);
}
