package org.molgenis.sql;

public class SqlException extends Exception {
    public SqlException(Exception e) {
        super(e);
    }

    public SqlException(String message) {
        super(message);
    }
}
