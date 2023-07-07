package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.sql.Migrations.executeMigrationFile;
import static org.molgenis.emx2.sql.Migrations.migration5addMgTableclassUpdateTrigger;

import java.util.Collections;
import java.util.List;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;

class InitTestDatabaseAndRunNonParallelTests {

  @Test
  void createDatabase() {
    // we want this run only once and NOT parallel for total test suite
    // AND we want run all other tests in parallel
    // so tests are in molgenis-emx2-sql-it ('integration test')
    // and 'init' only happens once, here
    System.out.println("INITIALIZING DATABASE");
    Database db = new SqlDatabase(true);
    assertTrue(db.getDatabaseVersion() > 0);
  }

  @Test
  @Tag("slow")
  void testMigration2() {
    SqlDatabase database = (SqlDatabase) TestDatabaseFactory.getTestDatabase();
    database.dropCreateSchema("TestMigrations");

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

    // create test schema and run migration 5
    Schema testSchemm =
        database.dropCreateSchema(InitTestDatabaseAndRunNonParallelTests.class.getSimpleName());
    testSchemm.getMetadata().create(table("pet", column("name").setPkey()));
    testSchemm.getMetadata().create(table("cat").setInherit("pet"));

    // run migration 9
    executeMigrationFile(
        database, "migration9.sql", "database migration: schema metadata visible for aggregator");

    executeMigrationFile(database, "migration10.sql", "add aggregator roles for all schemas");

    migration5addMgTableclassUpdateTrigger(database);
  }
}
