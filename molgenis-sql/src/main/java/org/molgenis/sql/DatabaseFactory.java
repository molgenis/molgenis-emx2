package org.molgenis.sql;

import com.zaxxer.hikari.HikariDataSource;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.molgenis.Column;
import org.molgenis.Database;
import org.molgenis.MolgenisException;

import java.util.List;

import static org.jooq.impl.DSL.name;
import static org.molgenis.Database.Prefix.MGROLE_;
import static org.molgenis.Database.Roles.*;

public class DatabaseFactory {

  private static HikariDataSource dataSource = null;
  private static DSLContext jooq = null;
  private static SqlDatabase db = null;

  public static Database getDatabase(String userName, String password) throws MolgenisException {
    if (db == null) {

      String url = "jdbc:postgresql:molgenis";

      // createColumn data source
      dataSource = new HikariDataSource();
      dataSource.setJdbcUrl(url);
      dataSource.setUsername(userName);
      dataSource.setPassword(password);

      // setup Jooq
      jooq = DSL.using(dataSource, SQLDialect.POSTGRES_10);
      deleteAll();
      // createColumn database to test against
      db = new SqlDatabase(dataSource);
    }
    return db;
  }

  public static void deleteAll() {
    // delete all foreign key constaints
    for (org.jooq.Schema s : jooq.meta().getSchemas()) {
      String name = s.getName();
      if (!name.startsWith("pg_") && !"information_schema".equals(name) && !"public".equals(name)) {
        for (org.jooq.Table t : s.getTables()) {
          for (ForeignKey k : (List<ForeignKey>) t.getReferences()) {
            jooq.alterTable(t).dropConstraint(k.getName()).execute();
          }
        }
      }
    }

    for (org.jooq.Schema s : jooq.meta().getSchemas()) {
      String schemaName = s.getName();

      if (!schemaName.startsWith("pg_")
          && !"information_schema".equals(schemaName)
          && !"public".equals(schemaName)) {
        jooq.dropSchema(s.getName()).cascade().execute();
        Name viewer = name(MGROLE_ + schemaName.toUpperCase() + _VIEWER);
        Name editor = name(MGROLE_ + schemaName.toUpperCase() + _EDITOR);
        Name manager = name(MGROLE_ + schemaName.toUpperCase() + _MANAGER);
        Name admin = name(MGROLE_ + schemaName.toUpperCase() + _ADMIN);
        jooq.execute("DROP ROLE IF EXISTS {0}", viewer);
        jooq.execute("DROP ROLE IF EXISTS {0}", editor);
        jooq.execute("DROP ROLE IF EXISTS {0}", manager);
        jooq.execute("DROP ROLE IF EXISTS {0}", admin);
      }
    }
    //    // delete all tables
    //    for (org.jooq.Table t : jooq.meta().getTableNames()) {
    //      jooq.dropTable(t).execute();
    //    }

  }

  public static DSLContext getJooq() {
    return jooq;
  }

  public static void checkColumnExists(Column c) throws MolgenisException {
    List<org.jooq.Table<?>> tables =
        DatabaseFactory.getJooq().meta().getTables(c.getTable().getName());
    if (tables.size() == 0)
      throw new MolgenisException("Table '" + c.getTable().getName() + "' does not exist");
    org.jooq.Table<?> table = tables.get(0);
    Field f = table.field(c.getName());
    if (f == null)
      throw new MolgenisException("Field '" + c.getName() + "." + c.getName() + "' does not exist");
  }
}
