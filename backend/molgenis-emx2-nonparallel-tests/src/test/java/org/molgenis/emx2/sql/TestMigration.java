package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.sql.MetadataUtils.*;
import static org.molgenis.emx2.sql.Migrations.executeMigrationFile;
import static org.molgenis.emx2.sql.Migrations.migration5addMgTableclassUpdateTrigger;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep3;
import org.jooq.Record1;
import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Username;

public class TestMigration {

  static SqlDatabase database = (SqlDatabase) TestDatabaseFactory.getTestDatabase();
  static DSLContext jooq = database.getJooq();

  @Test
  @Tag("slow")
  @Tag("windowsFail")
  void testMigration2() {
    database.dropCreateSchema("TestMigrations");

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
    Schema testSchemm = database.dropCreateSchema(TestMigration.class.getSimpleName());
    testSchemm.getMetadata().create(table("pet", column("name").setPkey()));
    testSchemm.getMetadata().create(table("cat").setInheritName("pet"));

    executeMigrationFile(
        database, "migration9.sql", "database migration: schema metadata visible for aggregator");

    executeMigrationFile(database, "migration10.sql", "add aggregator roles for all schemas");

    migration5addMgTableclassUpdateTrigger(database);

    executeMigrationFile(database, "migration22.sql", "test migration for deletion of refback");
  }

  @Test
  @Tag("slow")
  @Tag("windowsFail")
  void testMigration36AllowedValuesColumnIsVarcharArrayAndRerunIsHarmless() {
    executeMigrationFile(database, "migration36.sql", "add column_metadata.allowedValues");

    assertEquals(
        "_varchar",
        jooq.resultQuery(
                "SELECT udt_name FROM information_schema.columns"
                    + " WHERE table_schema = 'MOLGENIS' AND table_name = 'column_metadata'"
                    + " AND column_name = 'allowedValues'")
            .fetch()
            .getValues("udt_name", String.class)
            .get(0));
  }

  @Test
  void testMigration37_shouldThrowWhenDuplicates() {
    try {
      rawInsertUsers("migrations_foobarbaz", "migrations_FooBarBaz");

      DataAccessException expected =
          assertThrows(
              DataAccessException.class,
              () -> executeMigrationFile(database, "migration37.sql", "should throw"));
      assertTrue(
          expected
              .getCause()
              .getMessage()
              .contains("duplicate usernames found (case/whitespace-insensitive)"));
    } finally {
      jooq.deleteFrom(USERS_METADATA)
          .where(USER_NAME.in("migrations_foobarbaz", "migrations_FooBarBaz"))
          .execute();
    }
  }

  @Test
  void testMigration37_shouldNormalizeUsernames() {
    String raw = "  migrations_toTRIMandLOWERCASE  ";
    String normalized = new Username(raw).value();

    try {
      rawInsertUsers(raw);
      executeMigrationFile(database, "migration37.sql", "should throw");
      List<String> usernames =
          jooq.select(USER_NAME).from(USERS_METADATA).stream().map(Record1::value1).toList();
      assertFalse(usernames.contains(raw));
      assertTrue(usernames.contains(normalized));
    } finally {
      jooq.deleteFrom(USERS_METADATA).where(USER_NAME.in(raw, normalized)).execute();
    }
  }

  private void rawInsertUsers(String... usernames) {
    InsertValuesStep3 query =
        jooq.insertInto(USERS_METADATA).columns(USER_NAME, USER_ENABLED, SETTINGS);

    for (String username : usernames) {
      query = query.values(username, true, Map.of());
    }

    query.onConflictDoNothing().execute();
  }
}
