package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.graphql.GraphqlApiFactory.convertExecutionResultToJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQL;
import org.junit.jupiter.api.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.TestDatabaseFactory;

@Tag("rowlevel")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestGraphqlRolePermissions {
  private static Database database;
  private static Schema schema;
  private static GraphQL graphql;
  private static final String SCHEMA_NAME = "TestGqlRolePerms";

  @BeforeAll
  static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(SCHEMA_NAME);
    schema.create(
        table("Patients").add(column("id").setPkey()).add(column("name")),
        table("Samples").add(column("id").setPkey()));
    graphql = new GraphqlApiFactory().createGraphqlForSchema(schema, null);
  }

  private JsonNode execute(String query) throws Exception {
    String result = convertExecutionResultToJson(graphql.execute(query));
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
  public void testQueryRolesAsNonAdmin() throws Exception {
    try {
      database.setActiveUser("gql_analyst1");
      graphql = new GraphqlApiFactory().createGraphqlForSchema(schema, null);

      JsonNode roles = execute("{ _schema { roles { name } } }");
      boolean foundAnalysts = false;
      for (JsonNode role : roles.at("/_schema/roles")) {
        if ("Analysts".equals(role.get("name").asText())) {
          foundAnalysts = true;
          break;
        }
      }
      assertTrue(foundAnalysts, "Non-admin users should be able to query their own role");

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
      graphql = new GraphqlApiFactory().createGraphqlForSchema(schema, null);
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
      graphql = new GraphqlApiFactory().createGraphqlForSchema(schema, null);

      JsonNode result =
          execute("{ _schema { myPermissions { table select insert update delete } } }");
      JsonNode myPermissions = result.at("/_schema/myPermissions");
      assertNotNull(myPermissions, "myPermissions should be present");
      assertTrue(myPermissions.isArray(), "myPermissions should be an array");
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
      graphql = new GraphqlApiFactory().createGraphqlForSchema(schema, null);
      execute("mutation { drop(roles: [\"MyTestRole\"]) { message } }");
    }
  }

  @AfterAll
  static void tearDown() {
    if (database != null) {
      database.dropSchemaIfExists(SCHEMA_NAME);
    }
  }
}
