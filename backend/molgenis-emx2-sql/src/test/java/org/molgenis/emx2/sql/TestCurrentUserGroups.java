package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;

public class TestCurrentUserGroups {

  private static final String SCHEMA_NAME = TestCurrentUserGroups.class.getSimpleName();
  private static Database db;
  private static DSLContext jooq;

  @BeforeAll
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
    jooq = ((SqlDatabase) db).getJooq();
    db.dropSchemaIfExists(SCHEMA_NAME);
    db.createSchema(SCHEMA_NAME);

    String pgUser = jooq.fetchOne("SELECT current_user").get(0, String.class);
    jooq.execute(
        "INSERT INTO \"MOLGENIS\".users_metadata (username, enabled)"
            + " VALUES (?, true) ON CONFLICT DO NOTHING",
        pgUser);

    jooq.execute(
        "INSERT INTO \"MOLGENIS\".groups_metadata (schema, name) VALUES (?, ?) ON CONFLICT DO NOTHING",
        SCHEMA_NAME,
        "alpha");
    jooq.execute(
        "INSERT INTO \"MOLGENIS\".groups_metadata (schema, name) VALUES (?, ?) ON CONFLICT DO NOTHING",
        SCHEMA_NAME,
        "beta");
    jooq.execute(
        "INSERT INTO \"MOLGENIS\".groups_metadata (schema, name) VALUES (?, ?) ON CONFLICT DO NOTHING",
        SCHEMA_NAME,
        "gamma");
  }

  @AfterAll
  public static void tearDown() {
    db.becomeAdmin();
    jooq.execute(
        "DELETE FROM \"MOLGENIS\".group_membership_metadata WHERE schema_name = ?", SCHEMA_NAME);
    jooq.execute("DELETE FROM \"MOLGENIS\".groups_metadata WHERE schema = ?", SCHEMA_NAME);
    db.dropSchemaIfExists(SCHEMA_NAME);
  }

  @Test
  public void functionReturnsGroupsForCurrentUser() {
    String pgUser = jooq.fetchOne("SELECT current_user").get(0, String.class);

    jooq.execute(
        "INSERT INTO \"MOLGENIS\".group_membership_metadata"
            + " (user_name, schema_name, group_name, role_name)"
            + " VALUES (?, ?, 'alpha', 'Viewer'), (?, ?, 'beta', 'Viewer')"
            + " ON CONFLICT DO NOTHING",
        pgUser,
        SCHEMA_NAME,
        pgUser,
        SCHEMA_NAME);

    try {
      String[] result =
          jooq.fetchOne("SELECT \"MOLGENIS\".current_user_groups(?)", SCHEMA_NAME)
              .get(0, String[].class);

      assertNotNull(result);
      List<String> groups = List.of(result);
      assertTrue(groups.contains("alpha"), "current user must be in alpha");
      assertTrue(groups.contains("beta"), "current user must be in beta");
      assertFalse(groups.contains("gamma"), "current user must not be in gamma");
    } finally {
      jooq.execute(
          "DELETE FROM \"MOLGENIS\".group_membership_metadata"
              + " WHERE user_name = ? AND schema_name = ?",
          pgUser,
          SCHEMA_NAME);
    }
  }

  @Test
  public void functionReturnsEmptyArrayForUnknownSchema() {
    String[] result =
        jooq.fetchOne("SELECT \"MOLGENIS\".current_user_groups(?)", "no_such_schema_xyz")
            .get(0, String[].class);
    assertNotNull(result);
    assertEquals(0, result.length, "unknown schema must return empty array");
  }

  @Test
  public void functionOnlyReturnsGroupsForRequestedSchema() {
    String pgUser = jooq.fetchOne("SELECT current_user").get(0, String.class);
    String otherSchema = SCHEMA_NAME + "Other";
    db.dropSchemaIfExists(otherSchema);
    db.createSchema(otherSchema);

    try {
      jooq.execute(
          "INSERT INTO \"MOLGENIS\".groups_metadata (schema, name) VALUES (?, ?) ON CONFLICT DO NOTHING",
          otherSchema,
          "other-group");
      jooq.execute(
          "INSERT INTO \"MOLGENIS\".group_membership_metadata"
              + " (user_name, schema_name, group_name, role_name)"
              + " VALUES (?, ?, 'other-group', 'Viewer')"
              + " ON CONFLICT DO NOTHING",
          pgUser,
          otherSchema);
      jooq.execute(
          "INSERT INTO \"MOLGENIS\".group_membership_metadata"
              + " (user_name, schema_name, group_name, role_name)"
              + " VALUES (?, ?, 'alpha', 'Viewer')"
              + " ON CONFLICT DO NOTHING",
          pgUser,
          SCHEMA_NAME);

      String[] result =
          jooq.fetchOne("SELECT \"MOLGENIS\".current_user_groups(?)", SCHEMA_NAME)
              .get(0, String[].class);

      assertNotNull(result);
      assertFalse(
          List.of(result).contains("other-group"),
          "function must only return groups for the requested schema");
      assertTrue(
          List.of(result).contains("alpha"),
          "function must include groups from the requested schema");
    } finally {
      jooq.execute(
          "DELETE FROM \"MOLGENIS\".group_membership_metadata"
              + " WHERE user_name = ? AND (schema_name = ? OR schema_name = ?)",
          pgUser,
          SCHEMA_NAME,
          otherSchema);
      jooq.execute("DELETE FROM \"MOLGENIS\".groups_metadata WHERE schema = ?", otherSchema);
      db.dropSchemaIfExists(otherSchema);
    }
  }

  @Test
  public void functionHandlesMultipleMembershipsDistinctly() {
    String pgUser = jooq.fetchOne("SELECT current_user").get(0, String.class);

    jooq.execute(
        "INSERT INTO \"MOLGENIS\".group_membership_metadata"
            + " (user_name, schema_name, group_name, role_name)"
            + " VALUES (?, ?, 'alpha', 'Viewer'), (?, ?, 'alpha', 'Editor')"
            + " ON CONFLICT DO NOTHING",
        pgUser,
        SCHEMA_NAME,
        pgUser,
        SCHEMA_NAME);

    try {
      String[] result =
          jooq.fetchOne("SELECT \"MOLGENIS\".current_user_groups(?)", SCHEMA_NAME)
              .get(0, String[].class);

      assertNotNull(result);
      long alphaCount = List.of(result).stream().filter("alpha"::equals).count();
      assertEquals(1, alphaCount, "multiple memberships in same group must return group once");
    } finally {
      jooq.execute(
          "DELETE FROM \"MOLGENIS\".group_membership_metadata"
              + " WHERE user_name = ? AND schema_name = ? AND group_name = 'alpha'",
          pgUser,
          SCHEMA_NAME);
    }
  }
}
