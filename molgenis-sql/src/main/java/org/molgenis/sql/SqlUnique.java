package org.molgenis.sql;

import java.util.Collection;

public interface SqlUnique {

    SqlTable getTable();

    Collection<SqlColumn> getColumns();

    Collection<String> getColumnNames();
}
