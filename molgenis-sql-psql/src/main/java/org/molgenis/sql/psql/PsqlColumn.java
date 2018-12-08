package org.molgenis.sql.psql;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.molgenis.sql.SqlColumn;
import org.molgenis.sql.SqlTable;
import org.molgenis.sql.SqlType;

public class PsqlColumn implements SqlColumn
{
    private DSLContext sql;
    private SqlTable table;
    private String name;
    private SqlType type = SqlType.STRING;
    private SqlTable refTable = null;
    private boolean nullable = false;

    PsqlColumn(DSLContext sql, SqlTable table, String name, SqlTable otherTable) {
        this.sql = sql;
        this.table = table;
        this.name = name;
        this.refTable = otherTable;
    }


    PsqlColumn(DSLContext sql, SqlTable table, String name, SqlType type) {
        if(SqlType.REF.equals(type)) throw new IllegalArgumentException("type cannot be REF in constructor PsqlColumn(sql, table, name, type)");
        this.sql = sql;
        this.table = table;
        this.name = name;
        this.type = type;
    }

    PsqlColumn(DSLContext sql, SqlTable table, Field f) {
        this.sql = sql;
        this.table = table;
        this.name = f.getName();
        this.type = TypeUtils.getSqlType(f);
        this.nullable = f.getDataType().nullable();
    }

    public SqlColumn setNullable(boolean nillable) {
        if(nillable)
            sql.alterTable(table.getName()).alter(name).dropNotNull().execute();
        else
            sql.alterTable(table.getName()).alter(name).setNotNull().execute();
        this.nullable = nillable;
        return this;
    }

    @Override
    public SqlTable getTable() {
        return this.table;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public SqlType getType() {
        return this.type;
    }

    @Override
    public Boolean isNullable() {
        return this.nullable;
    }

    @Override
    public SqlTable getRefTable() {
        return refTable;
    }

    public String toString(){
        StringBuilder builder = new StringBuilder();
        builder.append(name).append(" ");
        if(SqlType.REF.equals(getType())) builder.append("ref(").append(refTable.getName()).append(")");
        else builder.append(type.toString().toLowerCase());
        if(isNullable()) builder.append(" nullable");
        return builder.toString();
    }
}