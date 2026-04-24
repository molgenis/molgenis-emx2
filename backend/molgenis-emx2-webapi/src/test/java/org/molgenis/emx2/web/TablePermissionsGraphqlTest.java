package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.TablePermission;
import org.molgenis.emx2.TablePermission.Scope;
import org.molgenis.emx2.TableType;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("slow")
class TablePermissionsGraphqlTest extends ApiTestBase {

  private static final String SCHEMA = "TablePermissionsGqlTest";
  private static final String TABLE_A = "ArticleA";
  private static final String TABLE_B = "ArticleB";
  private static final String TABLE_ONTOLOGY = "TagOntology";
  private static final String TABLE_WITH_REF = "ArticleWithRef";

  private static final String OWNER_USER = "tpgql_owner";
  private static final String CUSTOM_USER = "tpgql_custom";
  private static final String VIEWER_USER = "tpgql_viewer";

  private static final String OWNER_PASS = "ownerpass1";
  private static final String CUSTOM_PASS = "custompass1";
  private static final String VIEWER_PASS = "viewerpass1";

  private static final String SCHEMA_GQL = "/" + SCHEMA + "/api/graphql";

  @BeforeAll
  static void setup() {
    database.becomeAdmin();

    for (String user : List.of(OWNER_USER, CUSTOM_USER, VIEWER_USER)) {
      if (!database.hasUser(user)) database.addUser(user);
    }
    database.setUserPassword(OWNER_USER, OWNER_PASS);
    database.setUserPassword(CUSTOM_USER, CUSTOM_PASS);
    database.setUserPassword(VIEWER_USER, VIEWER_PASS);

    Schema schema = database.dropCreateSchema(SCHEMA);
    schema.create(
        table(TABLE_A).add(column("id").setPkey()).add(column("value")),
        table(TABLE_B).add(column("id").setPkey()).add(column("value")),
        table(TABLE_ONTOLOGY).setTableType(TableType.ONTOLOGIES),
        table(TABLE_WITH_REF)
            .add(column("id").setPkey())
            .add(column("ref").setType(ColumnType.REF).setRefTable(TABLE_B)));
    schema.getTable(TABLE_A).insert(new Row().setString("id", "a1").setString("value", "hello"));
    schema.getTable(TABLE_B).insert(new Row().setString("id", "b1").setString("value", "world"));
    schema.getTable(TABLE_ONTOLOGY).insert(new Row().setString("name", "tag1").setInt("order", 1));
    schema.getTable(TABLE_WITH_REF).insert(new Row().setString("id", "wr1").setString("ref", "b1"));

    schema.addMember(OWNER_USER, Privileges.OWNER.toString());
    schema.addMember(VIEWER_USER, Privileges.VIEWER.toString());
  }

  @Test
  @Order(1)
  void ownerCanCreateCustomRoleWithPermission() {
    login(OWNER_USER, OWNER_PASS);
    String body =
        given()
            .sessionId(sessionId)
            .body(
                """
                {"query":"mutation{change(roles:[{name:\\"ReaderA\\",permissions:[{table:\\"ArticleA\\",select:true}]}]){message}}"}
                """)
            .post(SCHEMA_GQL)
            .then()
            .statusCode(200)
            .extract()
            .asString();

    assertNoGraphqlErrors(body);

    String schemaBody =
        given()
            .sessionId(sessionId)
            .body(
                """
                {"query":"{ _schema { roles { name system permissions { table select } } } }"}
                """)
            .post(SCHEMA_GQL)
            .then()
            .extract()
            .asString();

    assertTrue(schemaBody.contains("\"ReaderA\""), "Custom role must appear in _schema.roles");
    assertTrue(schemaBody.contains("\"ArticleA\""), "Grant on ArticleA must be reflected");
    assertTrue(schemaBody.contains("true"), "SELECT grant must be reflected");
    assertTrue(
        schemaBody.contains("\"system\" : false"),
        "Custom role must have system:false, response: " + schemaBody);
  }

  @Test
  @Order(2)
  void nonOwnerCannotCreateRole() {
    login(VIEWER_USER, VIEWER_PASS);
    given()
        .sessionId(sessionId)
        .body(
            """
              {"query":"mutation{change(roles:[{name:\\"ForbiddenRole\\"}]){message}}"}
              """)
        .post(SCHEMA_GQL)
        .then()
        .statusCode(400)
        .extract()
        .asString();

    String schemaBody =
        given()
            .sessionId(sessionId)
            .body(
                """
                {"query":"{ _schema { roles { name } } }"}
                """)
            .post(SCHEMA_GQL)
            .then()
            .extract()
            .asString();

    assertFalse(
        schemaBody.contains("ForbiddenRole"), "Unauthorized role must not have been created");
  }

  @Test
  @Order(3)
  void customUserSessionShowsPermissionsAfterMembership() {
    database.becomeAdmin();
    database.getSchema(SCHEMA).addMember(CUSTOM_USER, "ReaderA");
    login(CUSTOM_USER, CUSTOM_PASS);

    String body =
        given()
            .sessionId(sessionId)
            .body(
                """
                {"query":"{ _session(schema:\\"TablePermissionsGqlTest\\") { permissions { table select insert update delete } } }"}
                """)
            .post(SCHEMA_GQL)
            .then()
            .extract()
            .asString();

    assertTrue(body.contains("\"ArticleA\""), "_session.permissions must list ArticleA");
    assertTrue(body.contains("\"ALL\""), "select must be ALL for SELECT grant");
  }

  @Test
  @Order(4)
  void customUserCanQueryGrantedTable() {
    String body =
        given()
            .sessionId(sessionId)
            .body(
                """
                {"query":"{ ArticleA { id value } }"}
                """)
            .post(SCHEMA_GQL)
            .then()
            .extract()
            .asString();

    assertNoGraphqlErrors(body);
    assertTrue(body.contains("a1"), "Custom user must be able to read rows from granted table");
  }

  @Test
  @Order(5)
  void customUserCannotQueryUngrantedTable() {
    String body =
        given()
            .sessionId(sessionId)
            .body(
                """
                {"query":"{ ArticleB { id value } }"}
                """)
            .post(SCHEMA_GQL)
            .then()
            .extract()
            .asString();

    assertTrue(body.contains("errors"), "Custom user must be blocked from ungranted table");
    assertFalse(body.contains("b1"), "Row data must not leak for ungranted table");
  }

  @Test
  @Order(6)
  void addInsertPermissionViaGraphqlAndVerifySession() {
    login(OWNER_USER, OWNER_PASS);
    String body =
        given()
            .sessionId(sessionId)
            .body(
                """
                {"query":"mutation{change(roles:[{name:\\"ReaderA\\",permissions:[{table:\\"ArticleA\\",select:true,insert:true}]}]){message}}"}
                """)
            .post(SCHEMA_GQL)
            .then()
            .statusCode(200)
            .extract()
            .asString();

    assertNoGraphqlErrors(body);

    login(CUSTOM_USER, CUSTOM_PASS);

    String sessionBody =
        given()
            .sessionId(sessionId)
            .body(
                """
                {"query":"{ _session(schema:\\"TablePermissionsGqlTest\\") { permissions { table select insert } } }"}
                """)
            .post(SCHEMA_GQL)
            .then()
            .extract()
            .asString();

    assertTrue(sessionBody.contains("\"ALL\""), "select and insert must be ALL after grant");
    assertTrue(sessionBody.contains("\"ArticleA\""), "ArticleA must appear in permissions");
  }

  @Test
  @Order(7)
  void revokeSelectViaFalseBlocksQueryAccess() {
    login(OWNER_USER, OWNER_PASS);
    String body =
        given()
            .sessionId(sessionId)
            .body(
                """
                {"query":"mutation{change(roles:[{name:\\"ReaderA\\",permissions:[{table:\\"ArticleA\\",select:false}]}]){message}}"}
                """)
            .post(SCHEMA_GQL)
            .then()
            .statusCode(200)
            .extract()
            .asString();

    assertNoGraphqlErrors(body);

    login(CUSTOM_USER, CUSTOM_PASS);
    String queryBody =
        given()
            .sessionId(sessionId)
            .body(
                """
                {"query":"{ ArticleA { id value } }"}
                """)
            .post(SCHEMA_GQL)
            .then()
            .extract()
            .asString();

    assertTrue(queryBody.contains("errors"), "SELECT must be blocked after revoke");
    assertFalse(queryBody.contains("a1"), "Row data must not be returned after SELECT revoke");
  }

  @Test
  @Order(8)
  void dropCustomRoleViaGraphql() {
    database.becomeAdmin();
    database
        .getSchema(SCHEMA)
        .grant(
            "ReaderA",
            new TablePermission(
                null, TABLE_A, Scope.ALL, Scope.NONE, Scope.NONE, Scope.NONE, false, false));

    login(OWNER_USER, OWNER_PASS);
    String body =
        given()
            .sessionId(sessionId)
            .body(
                """
                {"query":"mutation{drop(roles:[\\"ReaderA\\"]){message}}"}
                """)
            .post(SCHEMA_GQL)
            .then()
            .statusCode(200)
            .extract()
            .asString();

    assertNoGraphqlErrors(body);

    String schemaBody =
        given()
            .sessionId(sessionId)
            .body(
                """
                {"query":"{ _schema { roles { name } } }"}
                """)
            .post(SCHEMA_GQL)
            .then()
            .extract()
            .asString();

    assertFalse(schemaBody.contains("ReaderA"), "Dropped role must not appear in _schema.roles");
  }

  @Test
  @Order(9)
  void systemRolesVisibleWithWildcardPermissions() {
    String body =
        given()
            .sessionId(sessionId)
            .body(
                """
                {"query":"{ _schema { roles { name system permissions { table select } } } }"}
                """)
            .post(SCHEMA_GQL)
            .then()
            .extract()
            .asString();

    assertTrue(body.contains("\"Viewer\""), "System Viewer role must appear");
    assertTrue(body.contains("\"system\" : true"), "System roles must have system:true");
    assertTrue(body.contains("\"*\""), "System roles must report wildcard table permission");
  }

  // ── Schema roles query ──────────────────────────────────────────────────

  @Test
  @Order(10)
  void customRoleWithMixedPermissionsShownInSchemaQuery() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("MixedRole");
    schema.grant(
        "MixedRole",
        new TablePermission(
            null, TABLE_A, Scope.ALL, Scope.ALL, Scope.NONE, Scope.NONE, false, false));

    login(OWNER_USER, OWNER_PASS);
    String body =
        given()
            .sessionId(sessionId)
            .body(
                """
                {"query":"{ _schema { roles { name system permissions { table select insert update delete } } } }"}
                """)
            .post(SCHEMA_GQL)
            .then()
            .extract()
            .asString();

    assertTrue(body.contains("\"MixedRole\""), "MixedRole must appear in schema roles");
    assertTrue(body.contains("\"system\" : false"), "Custom role must have system:false");
    assertTrue(body.contains("\"ArticleA\""), "Permission should reference ArticleA");

    // Cleanup
    database.becomeAdmin();
    schema.deleteRole("MixedRole");
  }

  @Test
  @Order(11)
  void editorSystemRoleShownWithFullPermissions() {
    login(OWNER_USER, OWNER_PASS);
    String body =
        given()
            .sessionId(sessionId)
            .body(
                """
                {"query":"{ _schema { roles { name system permissions { table select insert update delete } } } }"}
                """)
            .post(SCHEMA_GQL)
            .then()
            .extract()
            .asString();

    assertTrue(body.contains("\"Editor\""), "Editor role must appear");
    assertTrue(body.contains("\"system\" : true"), "System roles must have system:true");
  }

  // ── Session edge cases ────────────────────────────────────────────────────

  @Test
  @Order(12)
  void userWithEmptyRoleSessionHasNoPermissions() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("EmptyGqlRole");
    schema.addMember(CUSTOM_USER, "EmptyGqlRole");

    login(CUSTOM_USER, CUSTOM_PASS);
    String body =
        given()
            .sessionId(sessionId)
            .body(
                """
                {"query":"{ _session(schema:\\"TablePermissionsGqlTest\\") { permissions { table select } } }"}
                """)
            .post(SCHEMA_GQL)
            .then()
            .extract()
            .asString();

    assertNoGraphqlErrors(body);
    assertFalse(
        body.contains("\"ALL\""), "User with empty role should not have any ALL scope permissions");

    // Cleanup
    database.becomeAdmin();
    schema.removeMember(CUSTOM_USER);
    schema.deleteRole("EmptyGqlRole");
  }

  @Test
  @Order(13)
  void revokeInsertShowsCorrectSessionPermission() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("FalseTestRole");
    schema.grant(
        "FalseTestRole",
        new TablePermission(
            null, TABLE_A, Scope.ALL, Scope.ALL, Scope.ALL, Scope.NONE, false, false));
    schema.grant(
        "FalseTestRole",
        new TablePermission(
            null, TABLE_A, Scope.ALL, Scope.NONE, Scope.ALL, Scope.NONE, false, false));
    schema.addMember(CUSTOM_USER, "FalseTestRole");

    login(CUSTOM_USER, CUSTOM_PASS);
    String body =
        given()
            .sessionId(sessionId)
            .body(
                """
                {"query":"{ _session(schema:\\"TablePermissionsGqlTest\\") { permissions { table select insert update delete } } }"}
                """)
            .post(SCHEMA_GQL)
            .then()
            .extract()
            .asString();

    assertNoGraphqlErrors(body);
    assertTrue(body.contains("\"ArticleA\""), "ArticleA must appear in permissions");
    assertTrue(body.contains("\"ALL\""), "select and update should be ALL");

    // Cleanup
    database.becomeAdmin();
    schema.removeMember(CUSTOM_USER);
    schema.deleteRole("FalseTestRole");
  }

  // ── Access enforcement ───────────────────────────────────────────────────

  @Test
  @Order(14)
  void ontologyTableAccessibleToCustomUserWithoutGrant() {
    database.becomeAdmin();
    Schema schema = database.getSchema(SCHEMA);
    schema.createRole("OntologyTestRole");
    schema.grant(
        "OntologyTestRole",
        new TablePermission(
            null, TABLE_A, Scope.ALL, Scope.NONE, Scope.NONE, Scope.NONE, false, false));
    schema.addMember(CUSTOM_USER, "OntologyTestRole");

    login(CUSTOM_USER, CUSTOM_PASS);
    String body =
        given()
            .sessionId(sessionId)
            .body(
                """
                {"query":"{ TagOntology { name } }"}
                """)
            .post(SCHEMA_GQL)
            .then()
            .extract()
            .asString();

    assertNoGraphqlErrors(body);
    assertTrue(
        body.contains("tag1"), "Ontology table data must be accessible without explicit grant");

    // Cleanup
    database.becomeAdmin();
    schema.removeMember(CUSTOM_USER);
    schema.deleteRole("OntologyTestRole");
  }

  @Test
  @Order(15)
  void refColumnToUngrantedTableIsHiddenInGraphqlSchema() {
    login(OWNER_USER, OWNER_PASS);
    String setupBody =
        given()
            .sessionId(sessionId)
            .body(
                """
                {"query":"mutation{change(roles:[{name:\\"RefTestRole\\",permissions:[{table:\\"ArticleWithRef\\",select:true}]}],members:[{email:\\"tpgql_custom\\",role:\\"RefTestRole\\"}]){message}}"}
                """)
            .post(SCHEMA_GQL)
            .then()
            .statusCode(200)
            .extract()
            .asString();
    assertNoGraphqlErrors(setupBody);

    login(CUSTOM_USER, CUSTOM_PASS);
    String body =
        given()
            .sessionId(sessionId)
            .body(
                """
                {"query":"{ ArticleWithRef { id } }"}
                """)
            .post(SCHEMA_GQL)
            .then()
            .extract()
            .asString();

    assertNoGraphqlErrors(body);
    assertTrue(body.contains("wr1"), "User should be able to read granted table");

    String refBody =
        given()
            .sessionId(sessionId)
            .body(
                """
                {"query":"{ ArticleWithRef { id ref { id } } }"}
                """)
            .post(SCHEMA_GQL)
            .then()
            .extract()
            .asString();

    assertTrue(
        refBody.contains("errors") || !refBody.contains("b1"),
        "Ref to ungranted table should be hidden or produce error, response: " + refBody);

    // Cleanup
    login(OWNER_USER, OWNER_PASS);
    given()
        .sessionId(sessionId)
        .body(
            """
            {"query":"mutation{drop(members:\\"tpgql_custom\\"){message}}"}
            """)
        .post(SCHEMA_GQL)
        .then()
        .statusCode(200);
    given()
        .sessionId(sessionId)
        .body(
            """
            {"query":"mutation{drop(roles:\\"RefTestRole\\"){message}}"}
            """)
        .post(SCHEMA_GQL)
        .then()
        .statusCode(200);
  }

  private static void assertNoGraphqlErrors(String responseBody) {
    assertFalse(
        responseBody.contains("\"errors\""),
        "Unexpected GraphQL errors in response: " + responseBody);
  }
}
