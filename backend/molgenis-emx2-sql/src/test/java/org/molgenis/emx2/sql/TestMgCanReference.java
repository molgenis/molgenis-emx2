package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;

import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.PermissionSet;
import org.molgenis.emx2.PermissionSet.ReferenceScope;
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.TablePermission;

public class TestMgCanReference {

  private static final String SCHEMA_NAME = TestMgCanReference.class.getSimpleName();
  private static final String USER_ALICE = "TestMgCanReferenceAlice";
  private static final String USER_SYSTEM = "TestMgCanReferenceSystem";
  private static final String GROUP_A = "groupA";
  private static final String GROUP_B = "groupB";

  private static Database db;
  private static DSLContext jooq;
  private static SqlRoleManager roleManager;

  @BeforeAll
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
    jooq = ((SqlDatabase) db).getJooq();
    roleManager = ((SqlDatabase) db).getRoleManager();

    Migrations.executeMigrationFile(
        db, "migration32.sql", "re-apply migration32 for mg_can_reference");

    db.dropSchemaIfExists(SCHEMA_NAME);
    Schema schema = db.createSchema(SCHEMA_NAME);

    if (!db.hasUser(USER_ALICE)) db.addUser(USER_ALICE);
    if (!db.hasUser(USER_SYSTEM)) db.addUser(USER_SYSTEM);

    roleManager.createGroup(schema, GROUP_A);
    roleManager.createGroup(schema, GROUP_B);
  }

  private boolean canReferenceAsUser(
      String userName, String tableName, String[] groups, String owner) {
    db.setActiveUser(userName);
    try {
      return Boolean.TRUE.equals(
          jooq.fetchOne(
                  "SELECT \"MOLGENIS\".mg_can_reference(?, ?, ?, ?) AS result",
                  SCHEMA_NAME,
                  tableName,
                  groups,
                  owner)
              .get("result", Boolean.class));
    } finally {
      db.becomeAdmin();
    }
  }

  private Schema schema() {
    return db.getSchema(SCHEMA_NAME);
  }

  @Test
  public void mgCanReference_returnsFalse_whenNoRoleAndNoOwnership() {
    boolean result = canReferenceAsUser(USER_ALICE, "noneTable", new String[] {}, "someone-else");
    assertFalse(result, "No role and no ownership must deny reference");
  }

  @Test
  public void mgCanReference_returnsTrue_whenViewScopeAllows() {
    roleManager.createRole(schema(), "view-all-reader", "");
    roleManager.setPermissions(
        schema(),
        "view-all-reader",
        new PermissionSet()
            .putTable(
                "viewAllTable",
                new TablePermission("viewAllTable")
                    .select(SelectScope.ALL)
                    .reference(ReferenceScope.NONE)));
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_A, USER_ALICE, "view-all-reader");

    boolean result =
        canReferenceAsUser(USER_ALICE, "viewAllTable", new String[] {GROUP_A}, "someone-else");
    assertTrue(result, "VIEW_ALL must imply reference (VIEW >= REFERENCE at same scope tier)");
  }

  @Test
  public void mgCanReference_returnsTrue_whenReferenceAll() {
    roleManager.createRole(schema(), "ref-all-role", "");
    roleManager.setPermissions(
        schema(),
        "ref-all-role",
        new PermissionSet()
            .putTable(
                "refAllTable",
                new TablePermission("refAllTable")
                    .select(SelectScope.NONE)
                    .reference(ReferenceScope.ALL)));
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_A, USER_ALICE, "ref-all-role");

    boolean resultAnyGroupAnyOwner =
        canReferenceAsUser(USER_ALICE, "refAllTable", new String[] {GROUP_B}, "someone-else");
    assertTrue(
        resultAnyGroupAnyOwner,
        "REFERENCE_ALL must grant reference for any row regardless of group/owner");
  }

  @Test
  public void mgCanReference_returnsTrue_whenReferenceOwn_andUserIsOwner() {
    roleManager.createRole(schema(), "ref-own-role", "");
    roleManager.setPermissions(
        schema(),
        "ref-own-role",
        new PermissionSet()
            .putTable(
                "refOwnTable",
                new TablePermission("refOwnTable")
                    .select(SelectScope.NONE)
                    .reference(ReferenceScope.OWN)));
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_A, USER_ALICE, "ref-own-role");

    boolean resultOwned =
        canReferenceAsUser(USER_ALICE, "refOwnTable", new String[] {}, USER_ALICE);
    assertTrue(resultOwned, "REFERENCE_OWN with owner=current_user must allow reference");

    boolean resultOtherOwner =
        canReferenceAsUser(USER_ALICE, "refOwnTable", new String[] {}, "someone-else");
    assertFalse(resultOtherOwner, "REFERENCE_OWN with owner != current_user must deny reference");
  }

  @Test
  public void mgCanReference_returnsTrue_whenReferenceGroup_andUserInGroup() {
    roleManager.createRole(schema(), "ref-group-role", "");
    roleManager.setPermissions(
        schema(),
        "ref-group-role",
        new PermissionSet()
            .putTable(
                "refGroupTable",
                new TablePermission("refGroupTable")
                    .select(SelectScope.NONE)
                    .reference(ReferenceScope.GROUP)));
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_A, USER_ALICE, "ref-group-role");

    boolean resultGroupMatch =
        canReferenceAsUser(USER_ALICE, "refGroupTable", new String[] {GROUP_A}, "someone-else");
    assertTrue(resultGroupMatch, "REFERENCE_GROUP with matching group must allow reference");

    boolean resultNoGroupMatch =
        canReferenceAsUser(USER_ALICE, "refGroupTable", new String[] {GROUP_B}, "someone-else");
    assertFalse(resultNoGroupMatch, "REFERENCE_GROUP with no matching group must deny reference");
  }

  @Test
  public void mgCanReference_returnsFalse_whenPrivacyScopeOnly() {
    roleManager.createRole(schema(), "privacy-count-role", "");
    roleManager.setPermissions(
        schema(),
        "privacy-count-role",
        new PermissionSet()
            .putTable(
                "countOnlyTable",
                new TablePermission("countOnlyTable")
                    .select(SelectScope.COUNT)
                    .reference(ReferenceScope.NONE)));
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_A, USER_ALICE, "privacy-count-role");

    boolean result =
        canReferenceAsUser(USER_ALICE, "countOnlyTable", new String[] {GROUP_A}, "someone-else");
    assertFalse(result, "COUNT privacy scope must not grant mg_can_reference");
  }

  @Test
  public void mgCanReference_returnsTrue_forSystemRole() {
    schema().addMember(USER_SYSTEM, "Owner");

    boolean result = canReferenceAsUser(USER_SYSTEM, "anyTable", new String[] {}, "someone-else");
    assertTrue(result, "Schema Owner system role must always allow reference");
  }
}
