package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.PermissionSet;
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.PermissionSet.UpdateScope;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.TablePermission;

public class TestRlsEnabledScopeGuard {

  private static final String SCHEMA_NAME = TestRlsEnabledScopeGuard.class.getSimpleName();
  private static final String NON_RLS_TABLE = "NonRlsTable";
  private static final String ROLE_NAME = "testRole";
  private static final String RLS_REQUIRED_MSG = "RLS-enabled";

  private static Database db;
  private static SqlRoleManager roleManager;

  @BeforeAll
  static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
    db.becomeAdmin();
    Schema schema = db.dropCreateSchema(SCHEMA_NAME);
    schema.create(table(NON_RLS_TABLE).add(column("id").setPkey()));
    roleManager = new SqlRoleManager((SqlDatabase) db);
    roleManager.createRole(schema, ROLE_NAME, "");
  }

  @Test
  void rejectsOwnSelectOnNonRlsTable() {
    Schema schema = db.getSchema(SCHEMA_NAME);
    PermissionSet permissions =
        new PermissionSet()
            .putTable(NON_RLS_TABLE, new TablePermission(NON_RLS_TABLE).select(SelectScope.OWN));

    MolgenisException ex =
        assertThrows(
            MolgenisException.class,
            () -> roleManager.setPermissions(schema, ROLE_NAME, permissions));
    assertTrue(
        ex.getMessage().contains(RLS_REQUIRED_MSG),
        "Exception message must contain '" + RLS_REQUIRED_MSG + "' but was: " + ex.getMessage());
  }

  @Test
  void rejectsGroupUpdateOnNonRlsTable() {
    Schema schema = db.getSchema(SCHEMA_NAME);
    PermissionSet permissions =
        new PermissionSet()
            .putTable(NON_RLS_TABLE, new TablePermission(NON_RLS_TABLE).update(UpdateScope.GROUP));

    MolgenisException ex =
        assertThrows(
            MolgenisException.class,
            () -> roleManager.setPermissions(schema, ROLE_NAME, permissions));
    assertTrue(
        ex.getMessage().contains(RLS_REQUIRED_MSG),
        "Exception message must contain '" + RLS_REQUIRED_MSG + "' but was: " + ex.getMessage());
  }

  @Test
  void rejectsChangeOwnerOnNonRlsTable() {
    Schema schema = db.getSchema(SCHEMA_NAME);
    PermissionSet permissions =
        new PermissionSet()
            .setChangeOwner(true)
            .putTable(NON_RLS_TABLE, new TablePermission(NON_RLS_TABLE).select(SelectScope.ALL));

    MolgenisException ex =
        assertThrows(
            MolgenisException.class,
            () -> roleManager.setPermissions(schema, ROLE_NAME, permissions));
    assertTrue(
        ex.getMessage().contains(RLS_REQUIRED_MSG),
        "Exception message must contain '" + RLS_REQUIRED_MSG + "' but was: " + ex.getMessage());
  }

  @Test
  void acceptsAllScopeOnNonRlsTable() {
    Schema schema = db.getSchema(SCHEMA_NAME);
    PermissionSet permissions =
        new PermissionSet()
            .putTable(NON_RLS_TABLE, new TablePermission(NON_RLS_TABLE).select(SelectScope.ALL));

    assertDoesNotThrow(() -> roleManager.setPermissions(schema, ROLE_NAME, permissions));

    PermissionSet stored = roleManager.getPermissionSet(SCHEMA_NAME, ROLE_NAME);
    assertEquals(
        SelectScope.ALL,
        stored.getTables().get(NON_RLS_TABLE).select(),
        "SELECT=ALL on non-RLS table must be stored and readable back");
  }
}
