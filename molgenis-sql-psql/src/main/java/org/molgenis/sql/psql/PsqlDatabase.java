package org.molgenis.sql.psql;

import org.jooq.DSLContext;
import org.jooq.impl.SQLDataType;
import org.molgenis.sql.*;

import static org.jooq.impl.DSL.constraint;

public class PsqlDatabase implements SqlDatabase {

    private DSLContext sql;
    public static final String MOLGENISID = "molgenisid";

    public PsqlDatabase(DSLContext context) {
        this.sql = context;
    }

    @Override
    public SqlTable createTable(String name) {
        sql.createTable(name).column(MOLGENISID, SQLDataType.UUID)
                .constraints(constraint("PK_"+name).primaryKey(MOLGENISID)).execute();
        return new PsqlTable(this, sql, name);
    }

    @Override
    public SqlTable getTable(String name) {
        return new PsqlTable(this, sql, name);
    }

    @Override
    public void dropTable(String tableId) {
        sql.dropTable(tableId).execute();
    }

    @Override
    public void close() {
        sql.close();
    }

    @Override
    public SqlQuery getQuery() {
        return new PsqlQuery(this, sql);
    }
}