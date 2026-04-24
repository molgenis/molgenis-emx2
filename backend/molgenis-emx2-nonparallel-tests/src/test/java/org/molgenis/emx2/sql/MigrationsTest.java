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
    assertCurrentUserRolesFunctionExists(jooq);
    assertAdminBypassRls(jooq);
    assertGuardTriggerFunctionExists(jooq);

    Migrations.initOrMigrate(database);

    assertVersion33(database, jooq);
    assertCurrentUserRolesFunctionExists(jooq);
    assertAdminBypassRls(jooq);
    assertGuardTriggerFunctionExists(jooq);
  }

  private void assertVersion33(SqlDatabase database, DSLContext jooq) {
    assertEquals(33, Migrations.getSoftwareDatabaseVersion());
    int dbVersion = MetadataUtils.getVersion(jooq);
    assertEquals(33, dbVersion);
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
