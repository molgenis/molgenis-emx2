package org.molgenis.sql;

public interface SqlColumn {

    SqlTable getTable();

    String getName();

    SqlType getType();

    Boolean isNullable();

    SqlColumn setNullable(boolean nillable);

    SqlTable getRefTable();
}
