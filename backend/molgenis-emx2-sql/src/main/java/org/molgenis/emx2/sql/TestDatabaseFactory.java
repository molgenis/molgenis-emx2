package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.utils.EnvironmentProperty.getParameter;

import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.molgenis.emx2.Database;

public class TestDatabaseFactory {

  private static DSLContext jooq = null;
  private static SqlDatabase db = null;
  private static HikariDataSource dataSource = null;
  private static Logger logger = Logger.getLogger(TestDatabaseFactory.class.getSimpleName());

  private TestDatabaseFactory() {
    // to hide the public constructor
  }

  public static Database getTestDatabase(DataSource source, boolean deleteAll) {
    if (db == null) {

      // setup local Jooq
      jooq = DSL.using(source, SQLDialect.POSTGRES);

      // delete all, only for test databases
      if (deleteAll) deleteAll();

      // get fresh database
      db = new SqlDatabase(source);
    }
    db.clearActiveUser();
    return db;
  }

  public static Database getTestDatabase() {
    return getTestDatabase(false);
  }

  public static synchronized Database getTestDatabase(boolean deleteAll) {

    String url = (String) getParameter(MOLGENIS_POSTGRES_URI, "jdbc:postgresql:molgenis", STRING);
    String user = (String) getParameter(MOLGENIS_POSTGRES_USER, "molgenis", STRING);
    String pass = (String) getParameter(MOLGENIS_POSTGRES_PASS, "molgenis", STRING);

    logger.info("Using database:");
    logger.info("with " + org.molgenis.emx2.Constants.MOLGENIS_POSTGRES_URI + "=" + url);
    logger.info("with " + org.molgenis.emx2.Constants.MOLGENIS_POSTGRES_USER + "=" + user);
    logger.info("with " + org.molgenis.emx2.Constants.MOLGENIS_POSTGRES_PASS + "=<HIDDEN>");

    // createColumn data source
    if (dataSource == null) {
      dataSource = new HikariDataSource();
      dataSource.setJdbcUrl(url);
      dataSource.setUsername(user);
      dataSource.setPassword(pass);
    }

    return getTestDatabase(dataSource, deleteAll);
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
}
