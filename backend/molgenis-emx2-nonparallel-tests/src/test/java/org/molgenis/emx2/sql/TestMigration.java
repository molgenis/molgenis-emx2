package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.sql.MetadataUtils.*;
import static org.molgenis.emx2.sql.Migrations.executeMigrationFile;
import static org.molgenis.emx2.sql.Migrations.migration5addMgTableclassUpdateTrigger;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Privileges;
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

  @Nested
  class Migration37 {

    @Test
    void shouldThrowWhenDuplicates() {
      Username username1 = rawUsername("migrations_FOOBARBAZ");
      Username username2 = rawUsername("migrations_FooBarBaz");
      try {
        database.addUser(username1);
        database.addUser(username2);

        DataAccessException expected =
            assertThrows(
                DataAccessException.class,
                () -> executeMigrationFile(database, "migration37.sql", "should throw"));

        assertTrue(
            expected
                .getCause()
                .getMessage()
                .contains("duplicate usernames found (trimmed/case-insensitive)"));
      } finally {
        if (database.hasUser(username1)) {
          database.removeUser(username1);
        }
        if (database.hasUser(username2)) {
          database.removeUser(username2);
        }
      }
    }

    @Test
    void shouldNormalizeUsernames() {
      Username username = rawUsername("  migrations_toTRIMandLOWERCASE  ");
      Username normalized = new Username(username.value());
      try {
        database.addUser(username);
        executeMigrationFile(database, "migration37.sql", "should throw");
        List<String> usernames =
            jooq.select(USER_NAME).from(USERS_METADATA).stream().map(Record1::value1).toList();
        assertFalse(usernames.contains(username.value()));
        assertTrue(usernames.contains(normalized.value()));

      } finally {
        if (database.hasUser(username)) {
          database.removeUser(username);
        }
        if (database.hasUser(normalized)) {
          database.removeUser(normalized);
        }
      }
    }

    @Test
    void whenUpdatingUsers_thenUpdateRolesAndKeepAccessToTheSameSchemas() {
      Username uppercase = rawUsername("UPPERCASE_USERNAME");
      Username normalized = new Username("uppercase_username");
      String schemaName = Migration37.class.getSimpleName() + "_updateRoles";

      try {
        database.addUser(uppercase);

        Schema schema = database.dropCreateSchema(schemaName);
        schema.addMember(uppercase, Privileges.VIEWER.toString());

        database.setActiveUser(uppercase);
        Collection<String> beforeMigration = database.getSchemaNames();

        database.becomeAdmin();
        executeMigrationFile(database, "migration37.sql", "should throw");

        assertFalse(database.hasUser(uppercase));

        // Check if the user still has access to the expected schemas
        database.setActiveUser(normalized);
        assertEquals(beforeMigration, database.getSchemaNames());
      } finally {
        database.becomeAdmin();
        if (database.hasUser(normalized)) {
          database.removeUser(normalized);
        }
      }
    }

    private Username rawUsername(String raw) {
      Username username = mock(Username.class);
      doReturn(raw).when(username).value();
      return username;
    }
  }
}
