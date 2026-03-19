package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.*;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.TablePermission;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("slow")
class TablePermissionsGraphqlTest extends ApiTestBase {

  private static final String SCHEMA = "TablePermissionsGqlTest";
  private static final String TABLE_A = "ArticleA";
  private static final String TABLE_B = "ArticleB";

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
        table(TABLE_B).add(column("id").setPkey()).add(column("value")));
    schema.getTable(TABLE_A).insert(new Row().setString("id", "a1").setString("value", "hello"));
    schema.getTable(TABLE_B).insert(new Row().setString("id", "b1").setString("value", "world"));

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
  void customUserSessionShowsTablePermissionsAfterMembership() {
    database.becomeAdmin();
    database.getSchema(SCHEMA).addMember(CUSTOM_USER, "ReaderA");
    login(CUSTOM_USER, CUSTOM_PASS);

    String body =
        given()
            .sessionId(sessionId)
            .body(
                """
                {"query":"{ _session { tablePermissions { name canView canInsert canUpdate canDelete } } }"}
                """)
            .post(SCHEMA_GQL)
            .then()
            .extract()
            .asString();

    assertTrue(body.contains("\"ArticleA\""), "_session.tablePermissions must list ArticleA");
    assertTrue(body.contains("\"canView\" : true"), "canView must be true for SELECT grant");
    assertTrue(body.contains("\"canInsert\" : false"), "canInsert must be false (not granted)");
    assertTrue(body.contains("\"canUpdate\" : false"), "canUpdate must be false (not granted)");
    assertTrue(body.contains("\"canDelete\" : false"), "canDelete must be false (not granted)");
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
                {"query":"mutation{change(roles:[{name:\\"ReaderA\\",permissions:[{table:\\"ArticleA\\",insert:true}]}]){message}}"}
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
                {"query":"{ _session { tablePermissions { name canView canInsert } } }"}
                """)
            .post(SCHEMA_GQL)
            .then()
            .extract()
            .asString();

    assertTrue(sessionBody.contains("\"canView\" : true"), "canView must still be true");
    assertTrue(sessionBody.contains("\"canInsert\" : true"), "canInsert must be true after grant");
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
        .grant("ReaderA", new TablePermission(TABLE_A, true, null, null, null));

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

  private static void assertNoGraphqlErrors(String responseBody) {
    assertFalse(
        responseBody.contains("\"errors\""),
        "Unexpected GraphQL errors in response: " + responseBody);
  }
}
