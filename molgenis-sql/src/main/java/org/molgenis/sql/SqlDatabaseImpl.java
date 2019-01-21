package org.molgenis.sql;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.jooq.impl.DSL.constraint;
import static org.molgenis.sql.SqlRow.MOLGENISID;

public class SqlDatabaseImpl implements SqlDatabase {

  private DSLContext sql;
  private Map<String, SqlTableImpl> tables = new LinkedHashMap<>();

  public SqlDatabaseImpl(DataSource source) throws SqlDatabaseException {
    DSLContext context = DSL.using(source, SQLDialect.POSTGRES_10);
    this.sql = context;
    for (Table t : sql.meta().getTables()) {
      tables.put(t.getName(), new SqlTableImpl(this, sql, t.getName()));
    }
    // todo, create a reload that reloads table and field metadata
  }

  @Override
  public SqlTable createTable(String name) throws SqlDatabaseException {
    sql.createTableIfNotExists(name)
        .column(MOLGENISID, SQLDataType.UUID)
        .constraints(constraint("PK_" + name).primaryKey(MOLGENISID))
        .execute();
    tables.put(name, new SqlTableImpl(this, sql, name));
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
  public SqlQuery query(String name) throws SqlDatabaseException {
    return new SqlQueryImpl(this, sql, name);
  }
}
