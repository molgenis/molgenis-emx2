package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

public class TestMigration {

  static Database database;

  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  @Tag("slow")
  @Tag("windowsFail")
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
    Schema testSchemm = database.dropCreateSchema(TestMigration.class.getSimpleName());
    testSchemm.getMetadata().create(table("pet", column("name").setPkey()));
    testSchemm.getMetadata().create(table("cat").setInheritNames("pet"));

    executeMigrationFile(
        database, "migration9.sql", "database migration: schema metadata visible for aggregator");

    executeMigrationFile(database, "migration10.sql", "add aggregator roles for all schemas");

    migration5addMgTableclassUpdateTrigger(database);

    executeMigrationFile(database, "migration22.sql", "test migration for deletion of refback");
  }

  @Test
  @Tag("slow")
  @Tag("windowsFail")
  void testMigration35ConvertsTableInheritsToArray() {
    SqlDatabase sqlDatabase = (SqlDatabase) TestDatabaseFactory.getTestDatabase();
    DSLContext jooq = sqlDatabase.getJooq();

    String migration35Schema = "TestMigration35";
    Schema schema = sqlDatabase.dropCreateSchema(migration35Schema);
    schema.getMetadata().create(table("pet", column("name").setPkey()));
    schema.getMetadata().create(table("cat").setInheritNames("pet"));

    // simulate the pre-v33 physical layout: table_inherits is a scalar VARCHAR column
    jooq.execute(
        "DO $$ BEGIN"
            + " IF (SELECT data_type FROM information_schema.columns"
            + "     WHERE table_schema = 'MOLGENIS' AND table_name = 'table_metadata'"
            + "     AND column_name = 'table_inherits') = 'ARRAY'"
            + " THEN ALTER TABLE \"MOLGENIS\".table_metadata"
            + "      ALTER COLUMN table_inherits TYPE VARCHAR USING (table_inherits[1]);"
            + " END IF; END $$");

    assertEquals(
        "character varying",
        jooq.resultQuery(
                "SELECT data_type FROM information_schema.columns"
                    + " WHERE table_schema = 'MOLGENIS' AND table_name = 'table_metadata'"
                    + " AND column_name = 'table_inherits'")
            .fetch()
            .getValues("data_type", String.class)
            .get(0));

    executeMigrationFile(
        sqlDatabase, "migration35.sql", "convert table_metadata.table_inherits to VARCHAR[]");

    assertEquals(
        "ARRAY",
        jooq.resultQuery(
                "SELECT data_type FROM information_schema.columns"
                    + " WHERE table_schema = 'MOLGENIS' AND table_name = 'table_metadata'"
                    + " AND column_name = 'table_inherits'")
            .fetch()
            .getValues("data_type", String.class)
            .get(0));
    assertEquals(
        "_varchar",
        jooq.resultQuery(
                "SELECT udt_name FROM information_schema.columns"
                    + " WHERE table_schema = 'MOLGENIS' AND table_name = 'table_metadata'"
                    + " AND column_name = 'table_inherits'")
            .fetch()
            .getValues("udt_name", String.class)
            .get(0));

    // reload confirms the array values are still readable as inheritNames
    sqlDatabase.clearCache();
    assertEquals(
        List.of("pet"),
        sqlDatabase
            .getSchema(migration35Schema)
            .getMetadata()
            .getTableMetadata("cat")
            .getInheritNames());
  }
}
