package org.molgenis.sql;

public class SqlDatabaseException extends Exception {

    public SqlDatabaseException(Exception e) {
        super(e);
    }

    public SqlDatabaseException(String message) {
        super(message);
    }
}
