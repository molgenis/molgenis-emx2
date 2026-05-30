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
  private static final String USER_NOPERM = "TestMgCanReferenceNoPermUser";
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
    if (!db.hasUser(USER_NOPERM)) db.addUser(USER_NOPERM);

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

  private boolean canReferenceWithExplicitUser(
      String pgUser, String tableName, String[] groups, String owner) {
    return Boolean.TRUE.equals(
        jooq.fetchOne(
                "SELECT \"MOLGENIS\".mg_can_reference(?, ?, ?, ?, ?) AS result",
                SCHEMA_NAME,
                tableName,
                groups,
                owner,
                pgUser)
            .get("result", Boolean.class));
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

  @Test
  public void mgCanReference_returnsTrue_whenSelectScopeGroupAndRowInUsersGroup() {
    roleManager.createRole(schema(), "group-select-role", "");
    roleManager.setPermissions(
        schema(),
        "group-select-role",
        new PermissionSet()
            .putTable(
                "groupSelectTable",
                new TablePermission("groupSelectTable")
                    .select(SelectScope.GROUP)
                    .reference(ReferenceScope.NONE)));
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_A, USER_ALICE, "group-select-role");

    boolean result =
        canReferenceAsUser(USER_ALICE, "groupSelectTable", new String[] {GROUP_A}, "someone-else");
    assertTrue(
        result,
        "SELECT_GROUP must grant reference when row is in user's group (VIEW >= REFERENCE carry rule)");
  }

  @Test
  public void mgCanReference_returnsFalse_whenSelectScopeGroupAndRowNotInUsersGroup() {
    roleManager.createRole(schema(), "group-select-other-role", "");
    roleManager.setPermissions(
        schema(),
        "group-select-other-role",
        new PermissionSet()
            .putTable(
                "groupSelectOtherTable",
                new TablePermission("groupSelectOtherTable")
                    .select(SelectScope.GROUP)
                    .reference(ReferenceScope.NONE)));
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_A, USER_ALICE, "group-select-other-role");

    boolean result =
        canReferenceAsUser(
            USER_ALICE, "groupSelectOtherTable", new String[] {GROUP_B}, "someone-else");
    assertFalse(result, "SELECT_GROUP must deny reference when row is NOT in user's group");
  }

  @Test
  public void mgCanReference_withExplicitUser_honorsPassedUser() {
    roleManager.createRole(schema(), "explicit-user-role", "");
    roleManager.setPermissions(
        schema(),
        "explicit-user-role",
        new PermissionSet()
            .putTable(
                "explicitUserTable",
                new TablePermission("explicitUserTable")
                    .select(SelectScope.ALL)
                    .reference(ReferenceScope.NONE)));
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_A, USER_ALICE, "explicit-user-role");

    String pgUserAlice = "MG_USER_" + USER_ALICE;
    boolean aliceCanReference =
        canReferenceWithExplicitUser(
            pgUserAlice, "explicitUserTable", new String[] {GROUP_A}, "someone-else");
    assertTrue(
        aliceCanReference,
        "Explicit p_user for alice's PG role must grant reference when alice has VIEW_ALL");

    String pgUserNoPerm = "MG_USER_" + USER_NOPERM;
    boolean noPermCanReference =
        canReferenceWithExplicitUser(
            pgUserNoPerm, "explicitUserTable", new String[] {}, "someone-else");
    assertFalse(
        noPermCanReference, "Explicit p_user for user with no permissions must deny reference");
  }
}
