package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public class MigrationsTest {

  private static final String MOLGENIS = "MOLGENIS";

  @Test
  @Tag("slow")
  void migration32AppliesIdempotently() {
    SqlDatabase database = (SqlDatabase) TestDatabaseFactory.getTestDatabase();
    DSLContext jooq = database.getJooq();

    assertVersion33(database, jooq);
    assertRoleMetadataShape(jooq);
    assertTableMetadataHasRlsColumn(jooq);
    assertCurrentUserRolesFunctionExists(jooq);
    assertAdminBypassRls(jooq);
    assertGuardTriggerFunctionExists(jooq);

    // Idempotency: run initOrMigrate again, all assertions must still hold
    Migrations.initOrMigrate(database);

    assertVersion33(database, jooq);
    assertRoleMetadataShape(jooq);
    assertTableMetadataHasRlsColumn(jooq);
    assertCurrentUserRolesFunctionExists(jooq);
    assertAdminBypassRls(jooq);
    assertGuardTriggerFunctionExists(jooq);
  }

  private void assertVersion33(SqlDatabase database, DSLContext jooq) {
    assertEquals(33, Migrations.getSoftwareDatabaseVersion());
    int dbVersion = MetadataUtils.getVersion(jooq);
    assertEquals(33, dbVersion);
  }

  private void assertRoleMetadataShape(DSLContext jooq) {
    // Table must exist in information_schema
    int tableCount =
        jooq.fetchOne(
                "SELECT count(*) FROM information_schema.tables "
                    + "WHERE table_schema = 'MOLGENIS' AND table_name = 'role_metadata'")
            .get(0, Integer.class);
    assertEquals(1, tableCount, "role_metadata table must exist in MOLGENIS schema");

    // Expected columns with their nullable/default properties
    String[] expectedColumns = {
      "role_name",
      "schema_name",
      "description",
      "immutable",
      "status",
      "created_by",
      "created_on",
      "deleted_on"
    };
    for (String col : expectedColumns) {
      int colCount =
          jooq.fetchOne(
                  "SELECT count(*) FROM information_schema.columns "
                      + "WHERE table_schema = 'MOLGENIS' AND table_name = 'role_metadata' "
                      + "AND column_name = ?",
                  col)
              .get(0, Integer.class);
      assertEquals(1, colCount, "column " + col + " must exist in role_metadata");
    }

    // role_name NOT NULL
    String roleNameNullable =
        jooq.fetchOne(
                "SELECT is_nullable FROM information_schema.columns "
                    + "WHERE table_schema = 'MOLGENIS' AND table_name = 'role_metadata' "
                    + "AND column_name = 'role_name'")
            .get(0, String.class);
    assertEquals("NO", roleNameNullable, "role_name must be NOT NULL");

    // schema_name NOT NULL with DEFAULT '*'
    Record schemaNameCol =
        jooq.fetchOne(
            "SELECT is_nullable, column_default FROM information_schema.columns "
                + "WHERE table_schema = 'MOLGENIS' AND table_name = 'role_metadata' "
                + "AND column_name = 'schema_name'");
    assertEquals("NO", schemaNameCol.get(0, String.class), "schema_name must be NOT NULL");
    String schemaDefault = schemaNameCol.get(1, String.class);
    assertNotNull(schemaDefault, "schema_name must have a DEFAULT");
    assertTrue(schemaDefault.contains("*"), "schema_name DEFAULT must contain '*'");

    // immutable NOT NULL DEFAULT false
    Record immutableCol =
        jooq.fetchOne(
            "SELECT is_nullable, column_default FROM information_schema.columns "
                + "WHERE table_schema = 'MOLGENIS' AND table_name = 'role_metadata' "
                + "AND column_name = 'immutable'");
    assertEquals("NO", immutableCol.get(0, String.class), "immutable must be NOT NULL");
    assertNotNull(immutableCol.get(1, String.class), "immutable must have a DEFAULT");

    // status NOT NULL DEFAULT 'active' with CHECK constraint
    Record statusCol =
        jooq.fetchOne(
            "SELECT is_nullable, column_default FROM information_schema.columns "
                + "WHERE table_schema = 'MOLGENIS' AND table_name = 'role_metadata' "
                + "AND column_name = 'status'");
    assertEquals("NO", statusCol.get(0, String.class), "status must be NOT NULL");
    String statusDefault = statusCol.get(1, String.class);
    assertNotNull(statusDefault, "status must have a DEFAULT");
    assertTrue(statusDefault.contains("active"), "status DEFAULT must be 'active'");

    // CHECK constraint on status restricting to ('active','deleted')
    int checkCount =
        jooq.fetchOne(
                "SELECT count(*) FROM information_schema.check_constraints cc "
                    + "JOIN information_schema.constraint_column_usage ccu "
                    + "ON cc.constraint_name = ccu.constraint_name AND cc.constraint_schema = ccu.constraint_schema "
                    + "WHERE ccu.table_schema = 'MOLGENIS' AND ccu.table_name = 'role_metadata' "
                    + "AND ccu.column_name = 'status' "
                    + "AND cc.check_clause LIKE '%active%' AND cc.check_clause LIKE '%deleted%'")
            .get(0, Integer.class);
    assertTrue(checkCount >= 1, "status column must have CHECK constraint for active/deleted");

    // Composite PK (role_name, schema_name)
    int pkColCount =
        jooq.fetchOne(
                "SELECT count(*) FROM information_schema.key_column_usage kcu "
                    + "JOIN information_schema.table_constraints tc "
                    + "ON kcu.constraint_name = tc.constraint_name AND kcu.table_schema = tc.table_schema "
                    + "WHERE kcu.table_schema = 'MOLGENIS' AND kcu.table_name = 'role_metadata' "
                    + "AND tc.constraint_type = 'PRIMARY KEY'")
            .get(0, Integer.class);
    assertEquals(2, pkColCount, "role_metadata PK must be composite with 2 columns");

    // Table must be empty
    int rowCount =
        jooq.fetchOne("SELECT count(*) FROM \"MOLGENIS\".role_metadata").get(0, Integer.class);
    assertEquals(0, rowCount, "role_metadata must be empty after migration");
  }

  private void assertTableMetadataHasRlsColumn(DSLContext jooq) {
    Record rlsCol =
        jooq.fetchOne(
            "SELECT is_nullable, column_default FROM information_schema.columns "
                + "WHERE table_schema = 'MOLGENIS' AND table_name = 'table_metadata' "
                + "AND column_name = 'row_level_security'");
    assertNotNull(rlsCol, "table_metadata must have row_level_security column");
    assertEquals("NO", rlsCol.get(0, String.class), "row_level_security must be NOT NULL");
    String rlsDefault = rlsCol.get(1, String.class);
    assertNotNull(rlsDefault, "row_level_security must have a DEFAULT");
    assertTrue(rlsDefault.contains("false"), "row_level_security DEFAULT must be false");
  }

  private void assertCurrentUserRolesFunctionExists(DSLContext jooq) {
    // provolatile = 's' means STABLE
    Record funcRecord =
        jooq.fetchOne(
            "SELECT p.provolatile FROM pg_proc p "
                + "JOIN pg_namespace n ON n.oid = p.pronamespace "
                + "WHERE n.nspname = 'MOLGENIS' AND p.proname = 'current_user_roles'");
    assertNotNull(funcRecord, "MOLGENIS.current_user_roles() function must exist");
    assertEquals(
        "s",
        funcRecord.get(0, String.class),
        "current_user_roles() must be STABLE (provolatile='s')");
  }

  private void assertAdminBypassRls(DSLContext jooq) {
    // Admin PG role is MG_USER_admin
    Record adminRole =
        jooq.fetchOne("SELECT rolbypassrls FROM pg_authid WHERE rolname = 'MG_USER_admin'");
    assertNotNull(adminRole, "PG role MG_USER_admin must exist");
    assertTrue(adminRole.get(0, Boolean.class), "Admin role must have BYPASSRLS attribute");
  }

  private void assertGuardTriggerFunctionExists(DSLContext jooq) {
    int funcCount =
        jooq.fetchOne(
                "SELECT count(*) FROM pg_proc p "
                    + "JOIN pg_namespace n ON n.oid = p.pronamespace "
                    + "WHERE n.nspname = 'MOLGENIS' AND p.proname = 'mg_reserved_column_guard'")
            .get(0, Integer.class);
    assertEquals(1, funcCount, "MOLGENIS.mg_reserved_column_guard() trigger function must exist");
  }
}
