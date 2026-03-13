package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.graphql.GraphqlExecutor.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.TestDatabaseFactory;

@Tag("rowlevel")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestGraphqlRolePermissions {
  private static Database database;
  private static Schema schema;
  private static GraphqlExecutor graphqlExecutor;
  private static final String SCHEMA_NAME = "TestGqlRolePerms";

  @BeforeAll
  static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(SCHEMA_NAME);
    schema.create(
        table("Patients").add(column("id").setPkey()).add(column("name")),
        table("Samples").add(column("id").setPkey()));
    graphqlExecutor = new GraphqlExecutor(schema);
  }

  private JsonNode execute(String query) throws Exception {
    String result = convertExecutionResultToJson(graphqlExecutor.executeWithoutSession(query));
    JsonNode node = new ObjectMapper().readTree(result);
    if (node.get("errors") != null) {
      throw new MolgenisException(node.get("errors").toString());
    }
    return node.get("data");
  }

  @Test
  @org.junit.jupiter.api.Order(1)
  public void testCreateRoleViaGraphql() throws Exception {
    JsonNode result =
        execute(
            "mutation { change(roles: [{name: \"Analysts\", description: \"Data analysts\"}]) { message } }");
    assertTrue(result.at("/change/message").asText().contains("success"));

    JsonNode roles = execute("{ _schema { roles { name description system } } }");
    boolean foundAnalysts = false;
    for (JsonNode role : roles.at("/_schema/roles")) {
      if ("Analysts".equals(role.get("name").asText())) {
        foundAnalysts = true;
        assertEquals("Data analysts", role.get("description").asText());
        assertFalse(role.get("system").asBoolean());
        break;
      }
    }
    assertTrue(foundAnalysts, "Analysts role should exist");
  }

  @Test
  @org.junit.jupiter.api.Order(2)
  public void testGrantTablePermissionViaGraphql() throws Exception {
    execute(
        "mutation { change(roles: [{name: \"Analysts\", permissions: [{table: \"Patients\", select: \"TABLE\", insert: \"TABLE\"}]}]) { message } }");

    JsonNode roles =
        execute("{ _schema { roles { name permissions { table select insert update delete } } } }");
    JsonNode analystsRole = null;
    for (JsonNode role : roles.at("/_schema/roles")) {
      if ("Analysts".equals(role.get("name").asText())) {
        analystsRole = role;
        break;
      }
    }
    assertNotNull(analystsRole, "Analysts role should exist");

    boolean foundPatientsPermission = false;
    for (JsonNode perm : analystsRole.get("permissions")) {
      if ("Patients".equals(perm.get("table").asText())) {
        foundPatientsPermission = true;
        assertEquals("TABLE", perm.get("select").asText());
        assertEquals("TABLE", perm.get("insert").asText());
        JsonNode updateNode = perm.get("update");
        JsonNode deleteNode = perm.get("delete");
        assertTrue(updateNode == null || updateNode.isNull());
        assertTrue(deleteNode == null || deleteNode.isNull());
        break;
      }
    }
    assertTrue(foundPatientsPermission, "Patients permission should exist");
  }

  @Test
  @org.junit.jupiter.api.Order(3)
  public void testGrantRowLevelPermissionViaGraphql() throws Exception {
    execute(
        "mutation { change(roles: [{name: \"Analysts\", permissions: [{table: \"Patients\", select: \"ROW\"}]}]) { message } }");

    JsonNode roles =
        execute("{ _schema { roles { name permissions { table select insert update delete } } } }");
    JsonNode analystsRole = null;
    for (JsonNode role : roles.at("/_schema/roles")) {
      if ("Analysts".equals(role.get("name").asText())) {
        analystsRole = role;
        break;
      }
    }
    assertNotNull(analystsRole);

    boolean foundPatientsPermission = false;
    for (JsonNode perm : analystsRole.get("permissions")) {
      if ("Patients".equals(perm.get("table").asText())) {
        foundPatientsPermission = true;
        assertEquals("ROW", perm.get("select").asText());
        assertEquals("TABLE", perm.get("insert").asText());
        break;
      }
    }
    assertTrue(foundPatientsPermission);
  }

  @Test
  @org.junit.jupiter.api.Order(4)
  public void testGrantWildcardPermissionViaGraphql() throws Exception {
    execute(
        "mutation { change(roles: [{name: \"Analysts\", permissions: [{table: \"*\", select: \"TABLE\"}]}]) { message } }");

    JsonNode roles =
        execute("{ _schema { roles { name permissions { table select insert update delete } } } }");
    JsonNode analystsRole = null;
    for (JsonNode role : roles.at("/_schema/roles")) {
      if ("Analysts".equals(role.get("name").asText())) {
        analystsRole = role;
        break;
      }
    }
    assertNotNull(analystsRole);

    boolean foundWildcard = false;
    boolean foundPatients = false;
    for (JsonNode perm : analystsRole.get("permissions")) {
      if ("*".equals(perm.get("table").asText())) {
        foundWildcard = true;
        assertEquals("TABLE", perm.get("select").asText());
      }
      if ("Patients".equals(perm.get("table").asText())) {
        foundPatients = true;
      }
    }
    assertTrue(foundWildcard, "Wildcard permission should exist");
    assertTrue(foundPatients, "Patients-specific permission should still exist");
  }

  @Test
  @org.junit.jupiter.api.Order(5)
  public void testAddMemberToRoleViaGraphql() throws Exception {
    database.addUser("gql_analyst1");

    execute(
        "mutation { change(members: {email: \"gql_analyst1\", role: \"Analysts\"}) { message } }");

    JsonNode members = execute("{ _schema { members { email role } } }");
    boolean foundMember = false;
    for (JsonNode member : members.at("/_schema/members")) {
      if ("gql_analyst1".equals(member.get("email").asText())) {
        foundMember = true;
        assertEquals("Analysts", member.get("role").asText());
        break;
      }
    }
    assertTrue(foundMember, "Member gql_analyst1 should exist in role Analysts");
  }

  @Test
  @org.junit.jupiter.api.Order(6)
  public void testQueryAsNonAdmin() throws Exception {
    try {
      database.setActiveUser("gql_analyst1");
      Schema freshSchema = database.getSchema(SCHEMA_NAME);
      graphqlExecutor = new GraphqlExecutor(freshSchema);

      JsonNode perms = execute("{ _session { permissions { table select } } }");
      assertNotNull(perms.at("/_session/permissions"), "Non-admin can query own permissions");

      try {
        execute("{ _schema { roles { name } } }");
        fail("Non-Manager user should not be able to query roles");
      } catch (Exception e) {
        assertTrue(e.getMessage().contains("roles"), "Expected field error for roles");
      }

      try {
        execute("mutation { change(roles: [{name: \"TestRole\"}]) { message } }");
        fail("Non-Manager user should not be able to create roles");
      } catch (MolgenisException e) {
        assertTrue(
            e.getMessage().contains("permission") || e.getMessage().contains("Manager"),
            "Expected permission error");
      }
    } finally {
      database.becomeAdmin();
      graphqlExecutor = new GraphqlExecutor(schema);
    }
  }

  @Test
  @org.junit.jupiter.api.Order(7)
  public void testDeleteRoleViaGraphql() throws Exception {
    execute("mutation { drop(roles: [\"Analysts\"]) { message } }");

    JsonNode roles = execute("{ _schema { roles { name } } }");
    for (JsonNode role : roles.at("/_schema/roles")) {
      assertNotEquals("Analysts", role.get("name").asText(), "Analysts role should be deleted");
    }
  }

  @Test
  @org.junit.jupiter.api.Order(8)
  public void testCreateRoleWithColumnAccess() throws Exception {
    execute(
        "mutation { change(roles: [{name: \"Restricted\", permissions: [{table: \"Patients\", select: \"TABLE\", columns: {hidden: [\"name\"]}}]}]) { message } }");

    JsonNode roles =
        execute(
            "{ _schema { roles { name permissions { table select columns { hidden readonly editable } } } } }");
    JsonNode restrictedRole = null;
    for (JsonNode role : roles.at("/_schema/roles")) {
      if ("Restricted".equals(role.get("name").asText())) {
        restrictedRole = role;
        break;
      }
    }
    assertNotNull(restrictedRole, "Restricted role should exist");

    JsonNode patientsPermission = null;
    for (JsonNode perm : restrictedRole.get("permissions")) {
      if ("Patients".equals(perm.get("table").asText())) {
        patientsPermission = perm;
        break;
      }
    }
    assertNotNull(patientsPermission, "Patients permission should exist");
    JsonNode columns = patientsPermission.get("columns");
    assertNotNull(columns, "Column access should be set");
    assertNotNull(columns.get("hidden"), "Hidden columns should be set");
    assertEquals(1, columns.get("hidden").size(), "Should have one hidden column");
    assertEquals("name", columns.get("hidden").get(0).asText(), "name column should be hidden");

    execute("mutation { drop(roles: [\"Restricted\"]) { message } }");
  }

  @Test
  @org.junit.jupiter.api.Order(9)
  public void testQueryMyPermissions() throws Exception {
    execute("mutation { change(roles: [{name: \"MyTestRole\"}]) { message } }");
    execute(
        "mutation { change(roles: [{name: \"MyTestRole\", permissions: [{table: \"Patients\", select: \"ROW\", insert: \"ROW\"}, {table: \"Samples\", select: \"TABLE\"}]}]) { message } }");

    database.addUser("myperms_test_user");
    execute(
        "mutation { change(members: {email: \"myperms_test_user\", role: \"MyTestRole\"}) { message } }");

    try {
      database.setActiveUser("myperms_test_user");
      graphqlExecutor = new GraphqlExecutor(schema);

      JsonNode result =
          execute("{ _session { permissions { table select insert update delete } } }");
      JsonNode myPermissions = result.at("/_session/permissions");
      assertNotNull(myPermissions, "permissions should be present");
      assertTrue(myPermissions.isArray(), "permissions should be an array");
      assertEquals(2, myPermissions.size(), "Should have 2 permissions");

      boolean foundPatients = false;
      boolean foundSamples = false;
      for (JsonNode perm : myPermissions) {
        if ("Patients".equals(perm.get("table").asText())) {
          foundPatients = true;
          assertEquals("ROW", perm.get("select").asText());
          assertEquals("ROW", perm.get("insert").asText());
        }
        if ("Samples".equals(perm.get("table").asText())) {
          foundSamples = true;
          assertEquals("TABLE", perm.get("select").asText());
        }
      }
      assertTrue(foundPatients, "Should find Patients permission");
      assertTrue(foundSamples, "Should find Samples permission");
    } finally {
      database.becomeAdmin();
      graphqlExecutor = new GraphqlExecutor(schema);
      execute("mutation { drop(roles: [\"MyTestRole\"]) { message } }");
    }
  }

  @Test
  @org.junit.jupiter.api.Order(10)
  public void testDropPermissionsViaGraphql() throws Exception {
    execute("mutation { change(roles: [{name: \"DropPermsRole\"}]) { message } }");
    execute(
        "mutation { change(roles: [{name: \"DropPermsRole\", permissions: [{table: \"Samples\", select: \"TABLE\"}]}]) { message } }");

    JsonNode rolesBefore = execute("{ _schema { roles { name permissions { table select } } } }");
    JsonNode roleBefore = null;
    for (JsonNode role : rolesBefore.at("/_schema/roles")) {
      if ("DropPermsRole".equals(role.get("name").asText())) {
        roleBefore = role;
        break;
      }
    }
    assertNotNull(roleBefore, "DropPermsRole should exist");

    boolean hasSamplesPermissionBefore = false;
    for (JsonNode perm : roleBefore.get("permissions")) {
      if ("Samples".equals(perm.get("table").asText())) {
        hasSamplesPermissionBefore = true;
        break;
      }
    }
    assertTrue(hasSamplesPermissionBefore, "Should have Samples permission before drop");

    execute(
        "mutation { drop(permissions: [{role: \"DropPermsRole\", table: \"Samples\"}]) { message } }");

    JsonNode rolesAfter = execute("{ _schema { roles { name permissions { table select } } } }");
    JsonNode roleAfter = null;
    for (JsonNode role : rolesAfter.at("/_schema/roles")) {
      if ("DropPermsRole".equals(role.get("name").asText())) {
        roleAfter = role;
        break;
      }
    }
    assertNotNull(roleAfter, "DropPermsRole should still exist");

    boolean hasSamplesPermissionAfter = false;
    for (JsonNode perm : roleAfter.get("permissions")) {
      if ("Samples".equals(perm.get("table").asText())) {
        hasSamplesPermissionAfter = true;
        break;
      }
    }
    assertFalse(hasSamplesPermissionAfter, "Samples permission should be removed");

    execute("mutation { drop(roles: [\"DropPermsRole\"]) { message } }");
  }

  @Test
  @org.junit.jupiter.api.Order(11)
  public void testGrantFieldRoundTrip() throws Exception {
    execute(
        "mutation { change(roles: [{name: \"GrantTestRole\", permissions: [{table: \"Patients\", select: \"TABLE\", grant: true}]}]) { message } }");

    JsonNode roles = execute("{ _schema { roles { name permissions { table select grant } } } }");
    JsonNode grantTestRole = null;
    for (JsonNode role : roles.at("/_schema/roles")) {
      if ("GrantTestRole".equals(role.get("name").asText())) {
        grantTestRole = role;
        break;
      }
    }
    assertNotNull(grantTestRole, "GrantTestRole should exist");

    boolean foundPatientsPermission = false;
    for (JsonNode perm : grantTestRole.get("permissions")) {
      if ("Patients".equals(perm.get("table").asText())) {
        foundPatientsPermission = true;
        assertEquals("TABLE", perm.get("select").asText());
        assertNotNull(perm.get("grant"), "grant field should be present");
        assertTrue(perm.get("grant").asBoolean(), "grant should be true");
        break;
      }
    }
    assertTrue(foundPatientsPermission, "Patients permission should exist");

    execute(
        "mutation { change(roles: [{name: \"GrantTestRole\", permissions: [{table: \"Samples\", select: \"TABLE\", grant: false}]}]) { message } }");

    JsonNode rolesAfter =
        execute("{ _schema { roles { name permissions { table select grant } } } }");
    JsonNode grantTestRoleAfter = null;
    for (JsonNode role : rolesAfter.at("/_schema/roles")) {
      if ("GrantTestRole".equals(role.get("name").asText())) {
        grantTestRoleAfter = role;
        break;
      }
    }
    assertNotNull(grantTestRoleAfter, "GrantTestRole should still exist");

    boolean foundPatientsWithGrant = false;
    boolean foundSamplesWithoutGrant = false;
    for (JsonNode perm : grantTestRoleAfter.get("permissions")) {
      if ("Patients".equals(perm.get("table").asText())) {
        foundPatientsWithGrant = true;
        assertTrue(perm.get("grant").asBoolean(), "Patients grant should still be true");
      }
      if ("Samples".equals(perm.get("table").asText())) {
        foundSamplesWithoutGrant = true;
        JsonNode grantNode = perm.get("grant");
        assertTrue(
            grantNode == null || grantNode.isNull() || !grantNode.asBoolean(),
            "Samples grant should be false or null");
      }
    }
    assertTrue(foundPatientsWithGrant, "Patients permission with grant=true should exist");
    assertTrue(foundSamplesWithoutGrant, "Samples permission should exist");

    execute("mutation { drop(roles: [\"GrantTestRole\"]) { message } }");
  }

  @AfterAll
  static void tearDown() {
    if (database != null) {
      database.dropSchemaIfExists(SCHEMA_NAME);
    }
  }
}
