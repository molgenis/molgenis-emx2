package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Constants.MG_ROLE_PREFIX;

import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;

class TestSystemRolesNoBypassRls {

  private static final String SCHEMA_NAME = "TestSysRolesNoBypassRls";
  private static final List<String> SYSTEM_ROLE_SUFFIXES =
      List.of("Owner", "Manager", "Editor", "Viewer", "Exists", "Range", "Aggregator", "Count");

  private static Database db;
  private static DSLContext jooq;

  @BeforeAll
  static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
    jooq = ((SqlDatabase) db).getJooq();
    db.becomeAdmin();
    Schema schema = db.dropCreateSchema(SCHEMA_NAME);
    assertNotNull(schema);
  }

  @AfterAll
  static void tearDown() {
    db.becomeAdmin();
    db.dropSchemaIfExists(SCHEMA_NAME);
  }

  @Test
  void systemPgRolesDoNotHaveBypassRls() {
    String rolePrefix = MG_ROLE_PREFIX + SCHEMA_NAME + "/";
    List<Record> rows =
        jooq.fetch(
            "SELECT rolname, rolbypassrls FROM pg_roles WHERE rolname LIKE ?", rolePrefix + "%");
    assertFalse(rows.isEmpty(), "At least one MG_ROLE_<schema>/... PG role must exist");
    for (Record row : rows) {
      String rolName = row.get("rolname", String.class);
      boolean bypassRls = Boolean.TRUE.equals(row.get("rolbypassrls", Boolean.class));
      assertFalse(bypassRls, "PG role must not have BYPASSRLS: " + rolName);
    }
  }

  @Test
  void memberUmbrellaRoleDoesNotHaveBypassRls() {
    String memberRole = MG_ROLE_PREFIX + SCHEMA_NAME + "_MEMBER";
    Record row =
        jooq.fetchOne("SELECT rolname, rolbypassrls FROM pg_roles WHERE rolname = ?", memberRole);
    assertNotNull(row, "MG_ROLE_<schema>_MEMBER must exist");
    assertFalse(
        Boolean.TRUE.equals(row.get("rolbypassrls", Boolean.class)),
        "MG_ROLE_<schema>_MEMBER must not have BYPASSRLS");
  }

  @Test
  void eachExpectedSystemRolePgRoleExists() {
    String rolePrefix = MG_ROLE_PREFIX + SCHEMA_NAME + "/";
    List<String> existingRoles =
        jooq.fetch("SELECT rolname FROM pg_roles WHERE rolname LIKE ?", rolePrefix + "%").stream()
            .map(r -> r.get("rolname", String.class).substring(rolePrefix.length()))
            .toList();
    for (String suffix : SYSTEM_ROLE_SUFFIXES) {
      assertTrue(
          existingRoles.contains(suffix),
          "Expected PG role MG_ROLE_" + SCHEMA_NAME + "/" + suffix + " to exist");
    }
  }
}
