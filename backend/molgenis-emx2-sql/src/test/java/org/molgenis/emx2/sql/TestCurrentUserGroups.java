package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;

public class TestCurrentUserGroups {

  private static final String SCHEMA_NAME = TestCurrentUserGroups.class.getSimpleName();
  private static final String TEST_USER = "TcugTestUser";
  private static Database db;
  private static DSLContext jooq;
  private static SqlRoleManager roleManager;
  private static Schema schema;

  @BeforeAll
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
    jooq = ((SqlDatabase) db).getJooq();
    roleManager = ((SqlDatabase) db).getRoleManager();
    db.dropSchemaIfExists(SCHEMA_NAME);
    schema = db.createSchema(SCHEMA_NAME);

    if (!db.hasUser(TEST_USER)) db.addUser(TEST_USER);

    roleManager.createGroup(schema, "alpha");
    roleManager.createGroup(schema, "beta");
    roleManager.createGroup(schema, "gamma");
  }

  private String[] currentUserGroupsAs(String userName) {
    db.setActiveUser(userName);
    try {
      return jooq.fetchOne("SELECT \"MOLGENIS\".current_user_groups(?)", SCHEMA_NAME)
          .get(0, String[].class);
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  public void functionReturnsGroupsForCurrentUser() {
    roleManager.addGroupMember(schema, "alpha", TEST_USER);
    roleManager.addGroupMember(schema, "beta", TEST_USER);

    try {
      String[] result = currentUserGroupsAs(TEST_USER);

      assertNotNull(result);
      List<String> groups = List.of(result);
      assertTrue(groups.contains("alpha"), "current user must be in alpha");
      assertTrue(groups.contains("beta"), "current user must be in beta");
      assertFalse(groups.contains("gamma"), "current user must not be in gamma");
    } finally {
      roleManager.removeGroupMember(schema, "alpha", TEST_USER);
      roleManager.removeGroupMember(schema, "beta", TEST_USER);
    }
  }

  @Test
  public void functionReturnsEmptyArrayForUnknownSchema() {
    db.setActiveUser(TEST_USER);
    try {
      String[] result =
          jooq.fetchOne("SELECT \"MOLGENIS\".current_user_groups(?)", "no_such_schema_xyz")
              .get(0, String[].class);
      assertNotNull(result);
      assertEquals(0, result.length, "unknown schema must return empty array");
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  public void functionOnlyReturnsGroupsForRequestedSchema() {
    String otherSchema = SCHEMA_NAME + "Other";
    db.dropSchemaIfExists(otherSchema);
    Schema other = db.createSchema(otherSchema);

    try {
      roleManager.createGroup(other, "other-group");
      roleManager.addGroupMember(other, "other-group", TEST_USER);
      roleManager.addGroupMember(schema, "alpha", TEST_USER);

      String[] result = currentUserGroupsAs(TEST_USER);

      assertNotNull(result);
      assertFalse(
          List.of(result).contains("other-group"),
          "function must only return groups for the requested schema");
      assertTrue(
          List.of(result).contains("alpha"),
          "function must include groups from the requested schema");
    } finally {
      roleManager.removeGroupMember(schema, "alpha", TEST_USER);
      db.dropSchemaIfExists(otherSchema);
    }
  }

  @Test
  public void functionHandlesMultipleMembershipsDistinctly() {
    roleManager.addGroupMember(schema, "alpha", TEST_USER);

    try {
      String[] result = currentUserGroupsAs(TEST_USER);

      assertNotNull(result);
      long alphaCount = List.of(result).stream().filter("alpha"::equals).count();
      assertEquals(1, alphaCount, "multiple memberships in same group must return group once");
    } finally {
      roleManager.removeGroupMember(schema, "alpha", TEST_USER);
    }
  }
}
