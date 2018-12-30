package org.molgenis.sql.psql;

import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.impl.SQLDataType;
import org.molgenis.sql.SqlDatabase;
import org.molgenis.sql.SqlQuery;
import org.molgenis.sql.SqlTable;

import java.util.ArrayList;
import java.util.List;

import static org.jooq.impl.DSL.constraint;
import static org.molgenis.sql.SqlRow.MOLGENISID;

public class PsqlDatabase implements SqlDatabase {

  private DSLContext sql;

  public PsqlDatabase(DSLContext context) {
    this.sql = context;
  }

  @Override
  public SqlTable createTable(String name) {
    sql.createTableIfNotExists(name)
        .column(MOLGENISID, SQLDataType.UUID)
        .constraints(constraint("PK_" + name).primaryKey(MOLGENISID))
        .execute();
    return new PsqlTable(this, sql, name);
  }

  @Override
  public List<SqlTable> getTables() {
    List<SqlTable> tables = new ArrayList<>();
    for (Table t : sql.meta().getTables()) {
      tables.add(new PsqlTable(this, sql, t.getName()));
    }
    return tables;
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
