package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

/**
 * Verifies that RLS-enabled tables have exactly 4 policies regardless of how many custom roles are
 * configured.
 */
class TestPolicyCount {

  private static final String SCHEMA_NAME = "TestPolicyCount";
  private static final String TABLE_A = "TableA";
  private static final String TABLE_B = "TableB";
  private static final String ROLE_ONE = "roleOne";
  private static final String ROLE_TWO = "roleTwo";
  private static final String ROLE_THREE = "roleThree";

  private static final Database db = TestDatabaseFactory.getTestDatabase();
  private static final DSLContext jooq = ((SqlDatabase) db).getJooq();
  private static final SqlRoleManager roleManager = new SqlRoleManager((SqlDatabase) db);

  private Schema schema;

  @BeforeEach
  void setUp() {
    db.becomeAdmin();
    schema = db.dropCreateSchema(SCHEMA_NAME);
    schema.create(table(TABLE_A).add(column("id").setPkey()).add(column("val")));
    schema.create(table(TABLE_B).add(column("id").setPkey()).add(column("val")));
    roleManager.createRole(SCHEMA_NAME, ROLE_ONE);
    roleManager.createRole(SCHEMA_NAME, ROLE_TWO);
    roleManager.createRole(SCHEMA_NAME, ROLE_THREE);
  }

  @AfterEach
  void tearDown() {
    db.becomeAdmin();
    db.dropSchemaIfExists(SCHEMA_NAME);
  }

  private long countPoliciesForTable(String tableName) {
    return jooq.fetchOne(
            "SELECT count(*) FROM pg_policies WHERE schemaname = ? AND tablename = ?",
            SCHEMA_NAME,
            tableName)
        .get(0, Long.class);
  }

  private PermissionSet groupPermissions(String tableName) {
    PermissionSet ps = new PermissionSet();
    PermissionSet.TablePermissions tp = new PermissionSet.TablePermissions();
    tp.setSelect(SelectScope.GROUP);
    tp.setInsert(UpdateScope.GROUP);
    tp.setUpdate(UpdateScope.GROUP);
    tp.setDelete(UpdateScope.GROUP);
    ps.putTable(tableName, tp);
    return ps;
  }

  @Test
  void fourPoliciesPerTable() {
    roleManager.setPermissions(schema, ROLE_ONE, groupPermissions(TABLE_A));
    roleManager.setPermissions(schema, ROLE_TWO, groupPermissions(TABLE_A));
    roleManager.setPermissions(schema, ROLE_THREE, groupPermissions(TABLE_A));

    assertEquals(
        4L,
        countPoliciesForTable(TABLE_A),
        "Exactly 4 policies per RLS-enabled table regardless of custom-role count");
  }

  @Test
  void fourPoliciesPerTableAcrossMultipleTables() {
    roleManager.setPermissions(schema, ROLE_ONE, groupPermissions(TABLE_A));
    roleManager.setPermissions(schema, ROLE_ONE, groupPermissions(TABLE_B));
    roleManager.setPermissions(schema, ROLE_TWO, groupPermissions(TABLE_A));
    roleManager.setPermissions(schema, ROLE_TWO, groupPermissions(TABLE_B));

    assertEquals(
        4L,
        countPoliciesForTable(TABLE_A),
        "TABLE_A must have exactly 4 policies even with multiple roles");
    assertEquals(
        4L,
        countPoliciesForTable(TABLE_B),
        "TABLE_B must have exactly 4 policies even with multiple roles");
  }
}
