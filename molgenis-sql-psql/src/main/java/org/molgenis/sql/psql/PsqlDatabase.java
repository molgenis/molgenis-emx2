package org.molgenis.sql.psql;

import org.jooq.DSLContext;
import org.jooq.impl.SQLDataType;
import org.molgenis.sql.*;

import static org.jooq.impl.DSL.constraint;

public class PsqlDatabase implements SqlDatabase {

    DSLContext sql;
    public static final String MOLGENISID = "molgenisid";

    public PsqlDatabase(DSLContext context) {
        this.sql = context;
    }

    public SqlTable createTable(String name) {
        sql.createTable(name).column(MOLGENISID, SQLDataType.UUID)
                .constraints(constraint("PK_"+name).primaryKey(MOLGENISID)).execute();
        PsqlTable table = new PsqlTable(this, name);
        table.reloadMetaData();
        return table;
    }

    public SqlTable getTable(String name) {
        PsqlTable table = new PsqlTable(this, name);
        table.reloadMetaData(); //todo: cache these results?
        return table;
    }

    public void dropTable(String tableId) {
        sql.dropTable(tableId).execute();
    }

    public void close() {
        sql.close();
    }

    @Override
    public SqlQuery getQuery() {
        return new PsqlQuery(this, sql);
    }

    DSLContext getDslContext() {
        return sql;
    }
}