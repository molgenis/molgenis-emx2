package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public class MigrationsTest {

  @Test
  @Tag("slow")
  void migration32AppliesIdempotently() {
    SqlDatabase database = (SqlDatabase) TestDatabaseFactory.getTestDatabase();
    DSLContext jooq = database.getJooq();

    assertVersion33(database, jooq);
    assertTableMetadataHasRlsColumn(jooq);
    assertCurrentUserRolesFunctionExists(jooq);
    assertAdminBypassRls(jooq);
    assertGuardTriggerFunctionExists(jooq);

    Migrations.initOrMigrate(database);

    assertVersion33(database, jooq);
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
                    + "WHERE n.nspname = 'MOLGENIS' AND p.proname = 'mg_enforce_row_authorisation'")
            .get(0, Integer.class);
    assertEquals(
        1, funcCount, "MOLGENIS.mg_enforce_row_authorisation() trigger function must exist");
  }
}
