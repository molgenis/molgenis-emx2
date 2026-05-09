package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.PermissionSet.SelectScope;

class TestRlsEnableDisableLifecycle {

  private static final String SCHEMA_NAME = "TestRlsEnableDisableLifecycle";
  private static final String TABLE_NAME = "SomeTable";
  private static final String ROLE_READER = "reader";
  private static final String REMOVE_PERMISSIONS_MSG = "remove permissions";

  private static final Database db = TestDatabaseFactory.getTestDatabase();
  private static final DSLContext jooq = ((SqlDatabase) db).getJooq();
  private static final SqlRoleManager roleManager = new SqlRoleManager((SqlDatabase) db);

  private Schema schema;

  @BeforeEach
  void setUp() {
    db.becomeAdmin();
    schema = db.dropCreateSchema(SCHEMA_NAME);
  }

  @AfterEach
  void tearDown() {
    db.becomeAdmin();
    db.dropSchemaIfExists(SCHEMA_NAME);
  }

  private boolean tableHasRls(String tableName) {
    Boolean result =
        jooq.fetchOne(
                "SELECT c.relrowsecurity FROM pg_class c"
                    + " JOIN pg_namespace n ON n.oid = c.relnamespace"
                    + " WHERE n.nspname = ? AND c.relname = ?",
                SCHEMA_NAME,
                tableName)
            .get(0, Boolean.class);
    return Boolean.TRUE.equals(result);
  }

  private long countPoliciesForTable(String tableName) {
    return jooq.fetchOne(
            "SELECT count(*) FROM pg_policies WHERE schemaname = ? AND tablename = ?",
            SCHEMA_NAME,
            tableName)
        .get(0, Long.class);
  }

  private boolean columnExists(String tableName, String columnName) {
    Long count =
        jooq.fetchOne(
                "SELECT count(*) FROM information_schema.columns"
                    + " WHERE table_schema = ? AND table_name = ? AND column_name = ?",
                SCHEMA_NAME,
                tableName,
                columnName)
            .get(0, Long.class);
    return count != null && count > 0L;
  }

  @Test
  void enableCreatesColumnsAndPoliciesAndBackfillsOwner() {
    schema.create(table(TABLE_NAME).add(column("id").setPkey()).add(column("val")));
    schema.getTable(TABLE_NAME).save(new Row().set("id", "r1").set("val", "a"));
    schema.getTable(TABLE_NAME).save(new Row().set("id", "r2").set("val", "b"));
    schema.getTable(TABLE_NAME).save(new Row().set("id", "r3").set("val", "c"));

    SqlTableMetadata meta = (SqlTableMetadata) schema.getTable(TABLE_NAME).getMetadata();
    meta.setRlsEnabled(true);

    assertTrue(columnExists(TABLE_NAME, "mg_owner"), "mg_owner column must exist after enable");
    assertTrue(columnExists(TABLE_NAME, "mg_groups"), "mg_groups column must exist after enable");
    assertEquals(4L, countPoliciesForTable(TABLE_NAME), "4 policies must exist after enable");
    assertTrue(tableHasRls(TABLE_NAME), "RLS must be enabled in pg_class");

    Long unbackfilled =
        jooq.fetchOne(
                "SELECT count(*) FROM {0} WHERE mg_owner IS NULL",
                org.jooq.impl.DSL.name(SCHEMA_NAME, TABLE_NAME))
            .get(0, Long.class);
    assertEquals(0L, unbackfilled, "All rows must have mg_owner backfilled from mg_insertedBy");
  }

  @Test
  void enableOnEmptyTable() {
    schema.create(table(TABLE_NAME).add(column("id").setPkey()));

    SqlTableMetadata meta = (SqlTableMetadata) schema.getTable(TABLE_NAME).getMetadata();
    meta.setRlsEnabled(true);

    assertTrue(columnExists(TABLE_NAME, "mg_owner"), "mg_owner column must exist after enable");
    assertTrue(columnExists(TABLE_NAME, "mg_groups"), "mg_groups column must exist after enable");
    assertEquals(4L, countPoliciesForTable(TABLE_NAME), "4 policies must exist after enable");
    assertTrue(tableHasRls(TABLE_NAME), "RLS must be enabled in pg_class");
  }

  @Test
  void disableOnCleanTableDropsColumnsAndPolicies() {
    schema.create(table(TABLE_NAME).add(column("id").setPkey()));
    SqlTableMetadata meta = (SqlTableMetadata) schema.getTable(TABLE_NAME).getMetadata();
    meta.setRlsEnabled(true);

    assertTrue(tableHasRls(TABLE_NAME), "RLS must be on before disable");

    meta.setRlsEnabled(false);

    assertFalse(tableHasRls(TABLE_NAME), "RLS must be disabled after setRlsEnabled(false)");
    assertEquals(0L, countPoliciesForTable(TABLE_NAME), "Policies must be dropped after disable");
    assertFalse(
        columnExists(TABLE_NAME, "mg_owner"), "mg_owner column must be dropped after disable");
    assertFalse(
        columnExists(TABLE_NAME, "mg_groups"), "mg_groups column must be dropped after disable");
  }

  @Test
  void disableRejectedWhenPermissionsExist() {
    schema.create(table(TABLE_NAME).add(column("id").setPkey()));
    SqlTableMetadata meta = (SqlTableMetadata) schema.getTable(TABLE_NAME).getMetadata();
    meta.setRlsEnabled(true);

    roleManager.createRole(SCHEMA_NAME, ROLE_READER);
    PermissionSet ps =
        new PermissionSet()
            .putTable(TABLE_NAME, new TablePermission(TABLE_NAME).setSelect(SelectScope.OWN));
    roleManager.setPermissions(schema, ROLE_READER, ps);

    MolgenisException ex = assertThrows(MolgenisException.class, () -> meta.setRlsEnabled(false));
    assertTrue(
        ex.getMessage().contains(REMOVE_PERMISSIONS_MSG),
        "Exception must mention 'remove permissions' but was: " + ex.getMessage());

    assertTrue(tableHasRls(TABLE_NAME), "RLS must still be on after rejected disable");
    assertEquals(
        4L, countPoliciesForTable(TABLE_NAME), "Policies must still exist after rejected disable");
    assertTrue(
        columnExists(TABLE_NAME, "mg_owner"), "mg_owner must still exist after rejected disable");
  }

  @Test
  void enableRls_createsGinIndexOnMgGroups() {
    schema.create(table(TABLE_NAME).add(column("id").setPkey()));
    SqlTableMetadata meta = (SqlTableMetadata) schema.getTable(TABLE_NAME).getMetadata();
    String expectedIndexName = TABLE_NAME + "_mg_groups_idx";

    meta.setRlsEnabled(true);

    Long indexCountAfterEnable =
        jooq.fetchOne(
                "SELECT count(*) FROM pg_indexes"
                    + " WHERE schemaname = ? AND tablename = ? AND indexname = ?",
                SCHEMA_NAME,
                TABLE_NAME,
                expectedIndexName)
            .get(0, Long.class);
    assertEquals(1L, indexCountAfterEnable, "GIN index on mg_groups must exist after enable");

    meta.setRlsEnabled(false);

    Long indexCountAfterDisable =
        jooq.fetchOne(
                "SELECT count(*) FROM pg_indexes"
                    + " WHERE schemaname = ? AND tablename = ? AND indexname = ?",
                SCHEMA_NAME,
                TABLE_NAME,
                expectedIndexName)
            .get(0, Long.class);
    assertEquals(0L, indexCountAfterDisable, "GIN index on mg_groups must be gone after disable");
  }

  @Test
  void enableRls_registersOwnerAndGroupsAsColumns() {
    schema.create(table(TABLE_NAME).add(column("id").setPkey()).add(column("val")));
    SqlTableMetadata meta = (SqlTableMetadata) schema.getTable(TABLE_NAME).getMetadata();
    meta.setRlsEnabled(true);

    assertNotNull(
        schema.getTable(TABLE_NAME).getMetadata().getColumn("mg_owner"),
        "mg_owner must be a first-class Column after RLS enable");
    assertNotNull(
        schema.getTable(TABLE_NAME).getMetadata().getColumn("mg_groups"),
        "mg_groups must be a first-class Column after RLS enable");

    String owner = "admin";
    schema
        .getTable(TABLE_NAME)
        .insert(
            new Row()
                .setString("id", "r1")
                .setString("val", "v")
                .setString("mg_owner", owner)
                .setStringArray("mg_groups", new String[] {"grp1"}));

    List<Row> rows = schema.getTable(TABLE_NAME).retrieveRows();
    assertEquals(1, rows.size(), "inserted row must be visible to admin");
    assertEquals(owner, rows.get(0).getString("mg_owner"), "mg_owner must match inserted value");

    meta.setRlsEnabled(false);

    assertNull(
        schema.getTable(TABLE_NAME).getMetadata().getColumn("mg_owner"),
        "mg_owner must be absent from Column metadata after RLS disable");
    assertNull(
        schema.getTable(TABLE_NAME).getMetadata().getColumn("mg_groups"),
        "mg_groups must be absent from Column metadata after RLS disable");
  }

  @Test
  void reEnableAfterDisableStartsEmpty() {
    schema.create(table(TABLE_NAME).add(column("id").setPkey()));
    SqlTableMetadata meta = (SqlTableMetadata) schema.getTable(TABLE_NAME).getMetadata();
    meta.setRlsEnabled(true);
    meta.setRlsEnabled(false);
    meta.setRlsEnabled(true);

    assertTrue(columnExists(TABLE_NAME, "mg_groups"), "mg_groups must exist after re-enable");
    assertEquals(4L, countPoliciesForTable(TABLE_NAME), "4 policies must exist after re-enable");
    assertTrue(tableHasRls(TABLE_NAME), "RLS must be on after re-enable");

    Long nonNullGroups =
        jooq.fetchOne(
                "SELECT count(*) FROM {0} WHERE mg_groups IS NOT NULL AND array_length(mg_groups, 1) > 0",
                org.jooq.impl.DSL.name(SCHEMA_NAME, TABLE_NAME))
            .get(0, Long.class);
    assertEquals(0L, nonNullGroups, "mg_groups must be empty (no stale data) after re-enable");
  }
}
