package org.molgenis.emx2.sql;

import com.zaxxer.hikari.HikariDataSource;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.utils.MolgenisException;
import org.molgenis.emx2.Column;

import javax.sql.DataSource;
import java.util.List;

import static org.jooq.impl.DSL.name;

public class DatabaseFactory {

  private static DSLContext jooq = null;
  private static SqlDatabase db = null;

  private DatabaseFactory() {
    // to hide the public constructor
  }

  public static Database getTestDatabase(DataSource source) {
    if (db == null) {

      // setup local Jooq
      jooq = DSL.using(source, SQLDialect.POSTGRES_10);

      // delete all
      deleteAll();

      // get fresh database
      db = new SqlDatabase(source);
    }
    db.clearActiveUser();
    return db;
  }

  public static Database getTestDatabase() {
    String url = "jdbc:postgresql:molgenis";

    // createColumn data source
    HikariDataSource dataSource = new HikariDataSource();
    dataSource.setJdbcUrl(url);
    dataSource.setUsername("molgenis");
    dataSource.setPassword("molgenis");

    return getTestDatabase(dataSource);
  }

  private static void deleteAll() {
    jooq.dropSchemaIfExists("MOLGENIS").cascade().execute();
    deleteAllForeignKeyConstraints();
    deleteAllSchemas();
    deleteAllRoles();
  }

  private static void deleteAllRoles() {
    for (String roleName : jooq.selectFrom(name("pg_roles")).fetchSet("rolname", String.class)) {
      if (roleName.startsWith("MG_")
          || roleName.startsWith("test")
          || roleName.startsWith("user_")) {
        String dbName = jooq.fetchOne("SELECT current_database()").get(0, String.class);
        jooq.execute(
            "REVOKE ALL PRIVILEGES ON DATABASE {0} FROM {1}", name(dbName), name(roleName));
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

  protected static DSLContext getJooq() {
    return jooq;
  }

  public static void checkColumnExists(Column c) {
    List<Table<?>> tables = DatabaseFactory.getJooq().meta().getTables(c.getTable().getTableName());
    if (tables.isEmpty())
      throw new MolgenisException(
          "invalid_table",
          "Table cannot be found",
          "Table '" + c.getTable().getTableName() + "' could not be found");
    Table<?> table = tables.get(0);
    Field f = table.field(c.getColumnName());
    if (f == null)
      throw new MolgenisException(
          "invalid_column",
          "Column cannot be found",
          "Column '"
              + c.getTable().getTableName()
              + "'.'    "
              + c.getColumnName()
              + "' could not be found");
  }
}
