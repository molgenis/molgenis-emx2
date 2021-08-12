package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.sql.Migrations.executeMigrationFile;

import java.util.Collections;
import java.util.List;
import org.jooq.DSLContext;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Schema;

public class TestMigrations {

  private static SqlDatabase database;
  private static Schema schema;

  @BeforeClass
  public static void setUp() {
    database = (SqlDatabase) TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(TestMigrations.class.getSimpleName());
  }

  @Test
  public void testMigration2() {
    DSLContext jooq = database.getJooq();

    // ensure no legacy roles exist from a previous test
    List<String> roles =
        jooq.selectFrom(name("pg_catalog", "pg_roles"))
            .where(field("rolname").like("MG_ROLE_TESTMIGRATIONS%"))
            .fetch()
            .getValues("rolname", String.class);
    Collections.reverse(roles);
    for (String role : roles) {
      jooq.execute("DROP ROLE {0}", name(role));
    }

    // in current MOLGENIS role would be MG_ROLE_TestMigrations
    assertEquals(
        1,
        jooq.selectFrom(name("pg_catalog", "pg_roles"))
            .where(field("rolname").eq("MG_ROLE_TestMigrations/Viewer"))
            .fetch()
            .size());

    // in previous MOLGENIS roles where uppercase, i.e. MG_ROLE_TEST_MIGRATIONS
    jooq.execute(
        "ALTER ROLE \"MG_ROLE_TestMigrations/Viewer\" RENAME TO \"MG_ROLE_TESTMIGRATIONS/Viewer\"");

    // verify
    assertEquals(
        0,
        jooq.selectFrom(name("pg_catalog", "pg_roles"))
            .where(field("rolname").eq("MG_ROLE_TestMigrations/Viewer"))
            .fetch()
            .size());
    assertEquals(
        1,
        jooq.selectFrom(name("pg_catalog", "pg_roles"))
            .where(field("rolname").eq("MG_ROLE_TESTMIGRATIONS/Viewer"))
            .fetch()
            .size());

    // run the migration
    executeMigrationFile(
        database,
        "migration2.sql",
        "database migration: role names are made case-sensitive matching schema names, to fix issue where roles where conflicting between schemas with same uppercase(name)");

    // should now be MG_ROLE_TestMigrations
    assertEquals(
        1,
        jooq.selectFrom(name("pg_catalog", "pg_roles"))
            .where(field("rolname").eq("MG_ROLE_TestMigrations/Viewer"))
            .fetch()
            .size());
    assertEquals(
        0,
        jooq.selectFrom(name("pg_catalog", "pg_roles"))
            .where(field("rolname").eq("MG_ROLE_TESTMIGRATIONS/Viewer"))
            .fetch()
            .size());
  }
}
