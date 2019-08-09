package org.molgenis.sql;

import com.zaxxer.hikari.HikariDataSource;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.molgenis.Column;
import org.molgenis.Database;
import org.molgenis.MolgenisException;

import java.util.List;

import static org.jooq.impl.DSL.name;

public class DatabaseFactory {

  private static DSLContext jooq = null;
  private static SqlDatabase db = null;

  private DatabaseFactory() {
    // to hide the public constructor
  }

  public static Database getTestDatabase(String userName, String password)
      throws MolgenisException {
    if (db == null) {

      String url = "jdbc:postgresql:molgenis";

      // createColumn data source
      HikariDataSource dataSource = new HikariDataSource();
      dataSource.setJdbcUrl(url);
      dataSource.setUsername(userName);
      dataSource.setPassword(password);

      // setup Jooq
      jooq = DSL.using(dataSource, SQLDialect.POSTGRES_10);

      // delete all
      deleteAll();

      // get fresh database
      db = new SqlDatabase(dataSource);
    }
    return db;
  }

  private static void deleteAll() {
    jooq.dropSchemaIfExists("MOLGENIS").cascade().execute();
    deleteAllForeignKeyConstraints();
    deleteAllSchemas();
    deleteAllRoles();
  }

  private static void deleteAllRoles() {
    for (String roleName : jooq.selectFrom(name("pg_roles")).fetchSet("rolname", String.class)) {
      if (roleName.startsWith("testRole")) {
        jooq.execute("DROP ROLE {0}", name(roleName));
      }
    }
  }

  private static void deleteAllSchemas() {
    for (Schema s : jooq.meta().getSchemas()) {
      String schemaName = s.getName();

      if (!schemaName.startsWith("pg_")
          && !"information_schema".equals(schemaName)
          && !"public".equals(schemaName)) {
        jooq.dropSchema(name(s.getName())).cascade().execute();
      }
    }
  }

  private static void deleteAllForeignKeyConstraints() {
    for (Schema s : jooq.meta().getSchemas()) {
      String name = s.getName();
      if (!name.startsWith("pg_") && !"information_schema".equals(name) && !"public".equals(name)) {
        for (Table t : s.getTables()) {
          for (ForeignKey k : (List<ForeignKey>) t.getReferences()) {
            jooq.alterTable(t).dropConstraint(k.getName()).execute();
          }
        }
      }
    }
  }

  public static DSLContext getJooq() {
    return jooq;
  }

  public static void checkColumnExists(Column c) throws MolgenisException {
    List<Table<?>> tables = DatabaseFactory.getJooq().meta().getTables(c.getTable().getName());
    if (tables.isEmpty())
      throw new MolgenisException("Table '" + c.getTable().getName() + "' does not exist");
    Table<?> table = tables.get(0);
    Field f = table.field(c.getName());
    if (f == null)
      throw new MolgenisException("Field '" + c.getName() + "." + c.getName() + "' does not exist");
  }
}
