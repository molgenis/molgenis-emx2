package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.junit.jupiter.api.Assertions.*;

import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;

class TestMemberPgRoleLifecycle {

  private static final String SCHEMA_NAME = "TestMemberPgRoleLifecycle";
  private static final String GROUP_ONE = "groupOne";
  private static final String GROUP_TWO = "groupTwo";
  private static final String ROLE_A = "roleA";
  private static final String ROLE_B = "roleB";

  private static final String USER_ALICE = "MplcAlice";
  private static final String USER_BOB = "MplcBob";

  private static final Database db = TestDatabaseFactory.getTestDatabase();
  private static final DSLContext jooq = ((SqlDatabase) db).getJooq();
  private static final SqlRoleManager roleManager = new SqlRoleManager((SqlDatabase) db);

  private Schema schema;

  @BeforeEach
  void setUp() {
    db.becomeAdmin();
    schema = db.dropCreateSchema(SCHEMA_NAME);
    if (!db.hasUser(USER_ALICE)) db.addUser(USER_ALICE);
    if (!db.hasUser(USER_BOB)) db.addUser(USER_BOB);
    roleManager.createGroup(schema, GROUP_ONE);
    roleManager.createGroup(schema, GROUP_TWO);
    roleManager.createRole(SCHEMA_NAME, ROLE_A);
    roleManager.createRole(SCHEMA_NAME, ROLE_B);
  }

  @AfterEach
  void tearDown() {
    db.becomeAdmin();
    db.dropSchemaIfExists(SCHEMA_NAME);
  }

  private boolean hasMemberRole(String pgUserName) {
    String memberRole = SqlRoleManager.memberRoleName(SCHEMA_NAME);
    return jooq.fetchExists(
        jooq.select()
            .from("pg_auth_members am")
            .join("pg_roles r")
            .on("r.oid = am.roleid")
            .join("pg_roles m")
            .on("m.oid = am.member")
            .where(field("r.rolname").eq(inline(memberRole)))
            .and(field("m.rolname").eq(inline(pgUserName))));
  }

  private String pgUser(String userName) {
    return "MG_USER_" + userName;
  }

  @Test
  void firstGroupMembershipGrantsMemberRole() {
    assertFalse(
        hasMemberRole(pgUser(USER_ALICE)), "Alice must not have MEMBER role before any membership");

    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, ROLE_A);

    assertTrue(
        hasMemberRole(pgUser(USER_ALICE)),
        "Alice must have MEMBER role after first group membership");

    roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, ROLE_A);
  }

  @Test
  void secondGroupMembershipIsIdempotentForMemberRole() {
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, ROLE_A);
    assertDoesNotThrow(
        () -> roleManager.addGroupMembership(SCHEMA_NAME, GROUP_TWO, USER_ALICE, ROLE_B),
        "Second addGroupMembership must not throw");

    assertTrue(
        hasMemberRole(pgUser(USER_ALICE)),
        "Alice must still have MEMBER role after second membership");

    roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, ROLE_A);
    roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_TWO, USER_ALICE, ROLE_B);
  }

  @Test
  void removeLastGroupMembershipRevokesReaderRole() {
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, ROLE_A);
    assertTrue(hasMemberRole(pgUser(USER_ALICE)), "Alice must have MEMBER role before remove");

    roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, ROLE_A);

    assertFalse(
        hasMemberRole(pgUser(USER_ALICE)),
        "Alice must not have MEMBER role after last group membership removed");
  }

  @Test
  void removeOneOfTwoMembershipsRetainsMemberRole() {
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, ROLE_A);
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_TWO, USER_ALICE, ROLE_B);

    roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_ALICE, ROLE_A);

    assertTrue(
        hasMemberRole(pgUser(USER_ALICE)),
        "Alice must retain MEMBER role while still having one membership");

    roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_TWO, USER_ALICE, ROLE_B);
  }

  @Test
  void deleteRoleRevokesFromAffectedUsers() {
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_BOB, ROLE_A);
    assertTrue(hasMemberRole(pgUser(USER_BOB)));

    roleManager.deleteRole(SCHEMA_NAME, ROLE_A);

    assertFalse(
        hasMemberRole(pgUser(USER_BOB)),
        "Bob must lose MEMBER role when the role is deleted and no other memberships remain");
  }

  @Test
  void deleteRoleKeepsMemberRoleIfUserHasOtherMemberships() {
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_ONE, USER_BOB, ROLE_A);
    roleManager.addGroupMembership(SCHEMA_NAME, GROUP_TWO, USER_BOB, ROLE_B);

    roleManager.deleteRole(SCHEMA_NAME, ROLE_A);

    assertTrue(
        hasMemberRole(pgUser(USER_BOB)),
        "Bob must retain MEMBER role because he still has membership via roleB");

    roleManager.removeGroupMembership(SCHEMA_NAME, GROUP_TWO, USER_BOB, ROLE_B);
  }

  @Test
  void memberRoleIsCreatedOnSchemaCreate() {
    String memberRole = SqlRoleManager.memberRoleName(SCHEMA_NAME);
    boolean exists =
        jooq.fetchExists(
            jooq.select().from("pg_roles").where(field("rolname").eq(inline(memberRole))));
    assertTrue(exists, "MEMBER PG role must exist after schema creation");
  }
}
