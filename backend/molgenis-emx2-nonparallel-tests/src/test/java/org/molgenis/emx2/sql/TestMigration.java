package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Constants.MG_TABLECLASS;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.sql.Migrations.executeMigrationFile;
import static org.molgenis.emx2.sql.Migrations.migration5addMgTableclassUpdateTrigger;

import java.util.Collections;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;

public class TestMigration {

  static Database database;

  private static final String MIGRATION33 = "migration33.sql";
  private static final String MIGRATION33_MESSAGE =
      "convert table_inherits to VARCHAR[] and add column values array to metadata";

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
  void testMigration33() {
    SqlDatabase sqlDatabase = (SqlDatabase) TestDatabaseFactory.getTestDatabase();
    DSLContext jooq = sqlDatabase.getJooq();

    downgradeMetadataToPreV33(jooq);

    assertEquals(
        "character varying",
        jooq.resultQuery(
                "SELECT data_type FROM information_schema.columns"
                    + " WHERE table_schema = 'MOLGENIS' AND table_name = 'table_metadata'"
                    + " AND column_name = 'table_inherits'")
            .fetch()
            .getValues("data_type", String.class)
            .get(0));

    assertEquals(
        0,
        jooq.resultQuery(
                "SELECT column_name FROM information_schema.columns"
                    + " WHERE table_schema = 'MOLGENIS' AND table_name = 'column_metadata'"
                    + " AND column_name = 'values'")
            .fetch()
            .size());

    executeMigrationFile(sqlDatabase, MIGRATION33, MIGRATION33_MESSAGE);

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

    assertEquals(
        "ARRAY",
        jooq.resultQuery(
                "SELECT data_type FROM information_schema.columns"
                    + " WHERE table_schema = 'MOLGENIS' AND table_name = 'column_metadata'"
                    + " AND column_name = 'values'")
            .fetch()
            .getValues("data_type", String.class)
            .get(0));

    assertEquals(
        "_varchar",
        jooq.resultQuery(
                "SELECT udt_name FROM information_schema.columns"
                    + " WHERE table_schema = 'MOLGENIS' AND table_name = 'column_metadata'"
                    + " AND column_name = 'values'")
            .fetch()
            .getValues("udt_name", String.class)
            .get(0));
  }

  @Test
  @Tag("slow")
  @Tag("windowsFail")
  void testMigration33UpgradesPre33ThreeLevelHierarchy() {
    SqlDatabase sqlDatabase = (SqlDatabase) TestDatabaseFactory.getTestDatabase();
    DSLContext jooq = sqlDatabase.getJooq();

    final String schemaName = "TestMigrationUpgrade";
    final String cat = "cat";
    final String kitten = "kitten";

    Schema schema = sqlDatabase.dropCreateSchema(schemaName);
    schema.create(table("pet", column("name").setPkey()));
    schema.create(table(cat).setInheritNames("pet").add(column("sound")));
    schema.create(table(kitten).setInheritNames(cat).add(column("age")));

    schema.getTable(kitten).insert(row("name", "tom", "sound", "meow", "age", "1"));

    // simulate pre-v33 physical layout: every subclass table carried its own mg_tableclass column
    jooq.execute(
        "ALTER TABLE {0} ADD COLUMN {1} VARCHAR",
        DSL.table(name(schemaName, cat)), name(MG_TABLECLASS));
    jooq.execute(
        "ALTER TABLE {0} ADD COLUMN {1} VARCHAR",
        DSL.table(name(schemaName, kitten)), name(MG_TABLECLASS));

    // simulate pre-v33 metadata: scalar table_inherits, no column values
    downgradeMetadataToPreV33(jooq);

    executeMigrationFile(sqlDatabase, MIGRATION33, MIGRATION33_MESSAGE);

    // scalar inherit value 'pet' is rewritten to array {'pet'}
    assertArrayEquals(
        new String[] {"pet"},
        jooq.resultQuery(
                "SELECT table_inherits FROM \"MOLGENIS\".table_metadata"
                    + " WHERE table_schema = {0} AND table_name = {1}",
                inline(schemaName), inline(cat))
            .fetch()
            .getValues("table_inherits", String[].class)
            .get(0));

    // upgraded 3-level hierarchy is queryable: intermediate mg_tableclass dropped so no ambiguity
    sqlDatabase.clearCache();
    Schema reread = sqlDatabase.getSchema(schemaName);
    assertEquals(1, reread.getTable(cat).retrieveRows().size());
    assertEquals(1, reread.getTable(kitten).retrieveRows().size());
  }

  private static void downgradeMetadataToPreV33(DSLContext jooq) {
    jooq.execute("ALTER TABLE \"MOLGENIS\".column_metadata DROP COLUMN IF EXISTS \"values\"");
    jooq.execute(
        "DO $$ BEGIN"
            + " IF (SELECT data_type FROM information_schema.columns"
            + "     WHERE table_schema = 'MOLGENIS' AND table_name = 'table_metadata'"
            + "     AND column_name = 'table_inherits') = 'ARRAY'"
            + " THEN ALTER TABLE \"MOLGENIS\".table_metadata"
            + "      ALTER COLUMN table_inherits TYPE VARCHAR USING (table_inherits[1]);"
            + " END IF; END $$");
  }
}
