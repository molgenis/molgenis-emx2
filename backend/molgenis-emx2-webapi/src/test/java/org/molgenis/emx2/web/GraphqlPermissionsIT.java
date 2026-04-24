package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.TableMetadata;

/**
 * End-to-end HTTP integration test for fine-grained permissions over GraphQL.
 *
 * <p>Exercises the full lifecycle: change(roleDefinitions/permissions/tables/members) →
 * _session(schema).permissions → data access → UPDATE scope enforcement → drop(roles).
 */
@Tag("slow")
class GraphqlPermissionsIT extends ApiTestBase {

  private static final long UNIQUE_SUFFIX = System.currentTimeMillis();
  private static final String SCHEMA = "fg_perm_" + UNIQUE_SUFFIX;
  private static final String TABLE = "Patients";
  private static final String CLINICIAN_ROLE = "clinician_" + UNIQUE_SUFFIX;
  private static final String NON_ADMIN_USER = "gpit_user_" + UNIQUE_SUFFIX;
  private static final String NON_ADMIN_PASS = "nonAdminPass1";

  private static final String API_GQL = "/api/graphql";
  private static final String SCHEMA_GQL = "/" + SCHEMA + "/api/graphql";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @BeforeAll
  static void setupScenario() {
    database.becomeAdmin();

    for (String user : List.of(NON_ADMIN_USER)) {
      if (!database.hasUser(user)) database.addUser(user);
      database.setUserPassword(user, NON_ADMIN_PASS);
    }

    Schema schema = database.dropCreateSchema(SCHEMA);
    schema.create(
        TableMetadata.table(TABLE)
            .add(Column.column("id").setType(ColumnType.STRING).setKey(1))
            .add(Column.column("name").setType(ColumnType.STRING)));

    schema.getTable(TABLE).insert(new Row().setString("id", "r1").setString("name", "admin-row-1"));
    schema.getTable(TABLE).insert(new Row().setString("id", "r2").setString("name", "admin-row-2"));
  }

  @AfterAll
  static void tearDownScenario() {
    try {
      database.becomeAdmin();
      database.dropSchemaIfExists(SCHEMA);
    } catch (Exception ignored) {
    }
  }

  @Test
  void fullScenario() throws Exception {
    login("admin", "admin");
    String adminSession = sessionId;

    // Step 3: single change mutation — creates role, grants SELECT=ALL + UPDATE=OWN,
    // enables RLS, adds non-admin user as member
    String changeMutation =
        "{\"query\":\"mutation{change("
            + "tables:[{schema:\\\""
            + SCHEMA
            + "\\\",table:\\\""
            + TABLE
            + "\\\",rowLevelSecurity:true}],"
            + "roles:[{name:\\\""
            + CLINICIAN_ROLE
            + "\\\",permissions:[{schema:\\\""
            + SCHEMA
            + "\\\",table:\\\""
            + TABLE
            + "\\\",select:ALL,update:OWN}]}],"
            + "members:[{role:\\\""
            + CLINICIAN_ROLE
            + "\\\",user:\\\""
            + NON_ADMIN_USER
            + "\\\"}]"
            + "){status message}}\"}";

    String changeBody =
        given()
            .sessionId(adminSession)
            .body(changeMutation)
            .post(API_GQL)
            .then()
            .statusCode(200)
            .extract()
            .asString();

    JsonNode changeResult = MAPPER.readTree(changeBody);
    assertEquals(
        "SUCCESS",
        changeResult.at("/data/change/status").asText(),
        "change must succeed: " + changeBody);

    // Step 4: non-admin queries _session(schema).permissions — must show SELECT=ALL, UPDATE=OWN.
    login(NON_ADMIN_USER, NON_ADMIN_PASS);
    String nonAdminSession = sessionId;

    String sessionBody =
        given()
            .sessionId(nonAdminSession)
            .body(
                "{\"query\":\"{ _session(schema:\\\""
                    + SCHEMA
                    + "\\\") { permissions { schema table select update } } }\"}")
            .post(API_GQL)
            .then()
            .extract()
            .asString();

    JsonNode sessionPerms = MAPPER.readTree(sessionBody).at("/data/_session/permissions");
    assertFalse(
        sessionPerms.isMissingNode(), "permissions must be present in response: " + sessionBody);
    assertTrue(sessionPerms.isArray(), "permissions must be array");

    boolean permFound = false;
    for (JsonNode perm : sessionPerms) {
      if (SCHEMA.equals(perm.at("/schema").asText()) && TABLE.equals(perm.at("/table").asText())) {
        assertEquals("ALL", perm.at("/select").asText(), "select scope must be ALL");
        assertEquals("OWN", perm.at("/update").asText(), "update scope must be OWN");
        permFound = true;
      }
    }
    assertTrue(permFound, "Expected permission entry for " + SCHEMA + "." + TABLE);

    // Step 5: non-admin SELECTs all rows — must succeed (SELECT=ALL)
    String selectBody =
        given()
            .sessionId(nonAdminSession)
            .body("{\"query\":\"{ Patients { id name } }\"}")
            .post(SCHEMA_GQL)
            .then()
            .extract()
            .asString();

    assertNoGraphqlErrors(selectBody, "SELECT all rows must succeed with SELECT=ALL");
    assertTrue(
        selectBody.contains("admin-row-1"),
        "Must see admin-owned rows with SELECT=ALL: " + selectBody);

    // Step 6: non-admin INSERT — INSERT scope is NONE, so the INSERT PostgreSQL privilege
    // is not granted; GraphQL must return an error
    String insertBody =
        given()
            .sessionId(nonAdminSession)
            .body(
                "{\"query\":\"mutation{insert(Patients:[{id:\\\"r99\\\",name:\\\"clinician-row\\\"}]){message}}\"}")
            .post(SCHEMA_GQL)
            .then()
            .extract()
            .asString();

    assertTrue(
        insertBody.contains("errors") || insertBody.contains("permission"),
        "INSERT must be rejected when INSERT scope is NONE: " + insertBody);

    // Step 7: UPDATE admin-owned row — must be silently ignored (0 rows) or error,
    // because RLS USING=(mg_owner=current_user) hides non-owned rows from UPDATE
    String updateForeignBody =
        given()
            .sessionId(nonAdminSession)
            .body(
                "{\"query\":\"mutation{update(Patients:[{id:\\\"r1\\\",name:\\\"hacked\\\"}]){message}}\"}")
            .post(SCHEMA_GQL)
            .then()
            .extract()
            .asString();

    // RLS USING clause means non-admin cannot see r1 (owned by admin) for UPDATE.
    database.becomeAdmin();
    String verifyBody =
        given()
            .sessionId(adminSession)
            .body("{\"query\":\"{ Patients(filter:{id:{equals:\\\"r1\\\"}}) { id name } }\"}")
            .post(SCHEMA_GQL)
            .then()
            .extract()
            .asString();

    assertFalse(
        verifyBody.contains("hacked"),
        "Admin-owned row must not have been modified by non-admin UPDATE: " + verifyBody);

    // Step 8: admin drops clinician role
    String dropBody =
        given()
            .sessionId(adminSession)
            .body(
                "{\"query\":\"mutation{drop(roles:[\\\""
                    + CLINICIAN_ROLE
                    + "\\\"]){status message}}\"}")
            .post(API_GQL)
            .then()
            .statusCode(200)
            .extract()
            .asString();

    JsonNode dropResult = MAPPER.readTree(dropBody);
    assertEquals(
        "SUCCESS", dropResult.at("/data/drop/status").asText(), "drop must succeed: " + dropBody);

    // After drop: non-admin permissions must be empty for this schema+table.
    String postDropSessionBody =
        given()
            .sessionId(nonAdminSession)
            .body(
                "{\"query\":\"{ _session(schema:\\\""
                    + SCHEMA
                    + "\\\") { permissions { schema table } } }\"}")
            .post(API_GQL)
            .then()
            .extract()
            .asString();

    JsonNode postDropPerms = MAPPER.readTree(postDropSessionBody).at("/data/_session/permissions");
    boolean permStillPresent = false;
    if (!postDropPerms.isMissingNode() && postDropPerms.isArray()) {
      for (JsonNode perm : postDropPerms) {
        if (SCHEMA.equals(perm.at("/schema").asText())
            && TABLE.equals(perm.at("/table").asText())) {
          permStillPresent = true;
        }
      }
    }
    assertFalse(
        permStillPresent,
        "After drop, permissions must not include the schema+table entry: " + postDropSessionBody);
  }

  private static void assertNoGraphqlErrors(String responseBody, String context) {
    assertFalse(
        responseBody.contains("\"errors\""),
        context + " — unexpected GraphQL errors: " + responseBody);
  }
}
