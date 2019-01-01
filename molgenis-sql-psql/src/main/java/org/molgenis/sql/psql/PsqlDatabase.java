package org.molgenis.sql.psql;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.molgenis.sql.SqlDatabase;
import org.molgenis.sql.SqlQuery;
import org.molgenis.sql.SqlTable;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.jooq.impl.DSL.constraint;
import static org.molgenis.sql.SqlRow.MOLGENISID;

public class PsqlDatabase implements SqlDatabase {

  private DSLContext sql;
  private Map<String, PsqlTable> tables = new LinkedHashMap<String, PsqlTable>();

  public PsqlDatabase(DataSource source) {
    DSLContext context = DSL.using(source, SQLDialect.POSTGRES_10);
    this.sql = context;
    for (Table t : sql.meta().getTables()) {
      tables.put(t.getName(), new PsqlTable(this, sql, t.getName()));
    }
    // todo, create a reload that reloads table and field metadata
  }

  @Override
  public SqlTable createTable(String name) {
    sql.createTableIfNotExists(name)
        .column(MOLGENISID, SQLDataType.UUID)
        .constraints(constraint("PK_" + name).primaryKey(MOLGENISID))
        .execute();
    tables.put(name, new PsqlTable(this, sql, name));
    return getTable(name);
  }

  @Override
  public Collection<SqlTable> getTables() {
    return Collections.unmodifiableCollection(tables.values());
  }

  @Override
  public SqlTable getTable(String name) {
    return tables.get(name);
  }

  @Override
  public void dropTable(String tableId) {
    sql.dropTable(tableId).execute();
    tables.remove(tableId);
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
