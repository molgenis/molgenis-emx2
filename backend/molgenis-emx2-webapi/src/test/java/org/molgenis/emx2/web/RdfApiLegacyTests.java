package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.datamodels.DataModels.Profile.PET_STORE;
import static org.molgenis.emx2.sql.SqlDatabase.*;
import static org.molgenis.emx2.web.Constants.*;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSender;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.utils.EnvironmentProperty;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@TestMethodOrder(MethodOrderer.MethodName.class)
@Tag("slow")
@ExtendWith(SystemStubsExtension.class)
class RdfApiLegacyTests {

  private static final String EXCEPTION_CONTENT_TYPE = Constants.ACCEPT_JSON;

  private static final String ADMIN_PASS =
      (String) EnvironmentProperty.getParameter(MOLGENIS_ADMIN_PW, ADMIN_PW_DEFAULT, STRING);

  public static final String PET_SHOP_OWNER = "pet_shop_owner";
  public static final String PET_SHOP_VIEWER = "shopviewer";
  public static final String PET_SHOP_MANAGER = "shopmanager";

  public static final String TABLE_WITH_SPACES = "table with spaces";
  public static final String PET_STORE_SCHEMA = "pet store";

  private static final int PORT = 8082;

  private static String sessionId;
  private static Database db;
  private static Schema schema;

  @BeforeAll
  static void before() throws Exception {
    db = TestDatabaseFactory.getTestDatabase();

    new EnvironmentVariables(
            org.molgenis.emx2.Constants.MOLGENIS_METRICS_ENABLED, Boolean.TRUE.toString())
        .execute(
            () -> {
              RunMolgenisEmx2.main(new String[] {String.valueOf(PORT)});
            });

    RestAssured.port = PORT;
    RestAssured.baseURI = "http://localhost";

    setAdminSession();
    setupDatabase();
  }

  private static void setupDatabase() {
    db.dropSchemaIfExists(PET_STORE_SCHEMA);
    PET_STORE.getImportTask(db, PET_STORE_SCHEMA, "", true).run();
    schema = db.getSchema(PET_STORE_SCHEMA);

    db.setUserPassword(PET_SHOP_OWNER, PET_SHOP_OWNER);
    db.setUserPassword(PET_SHOP_VIEWER, PET_SHOP_VIEWER);
    db.setUserPassword(PET_SHOP_MANAGER, PET_SHOP_MANAGER);
    schema.addMember(PET_SHOP_MANAGER, Privileges.MANAGER.toString());
    schema.addMember(PET_SHOP_VIEWER, Privileges.VIEWER.toString());
    schema.addMember(PET_SHOP_OWNER, Privileges.OWNER.toString());
    schema.addMember(org.molgenis.emx2.Constants.ANONYMOUS, Privileges.VIEWER.toString());
    db.grantCreateSchema(PET_SHOP_OWNER);
    if (schema.getTable(TABLE_WITH_SPACES) == null) {
      schema.create(table(TABLE_WITH_SPACES, column("name", STRING).setKey(1)));
    }
  }

  private static void setAdminSession() {
    sessionId =
        given()
            .body(
                "{\"query\":\"mutation{signin(email:\\\""
                    + db.getAdminUserName()
                    + "\\\",password:\\\""
                    + ADMIN_PASS
                    + "\\\"){message}}\"}")
            .when()
            .post("api/graphql")
            .sessionId();
  }

  @AfterAll
  static void after() {
    db.dropSchemaIfExists(PET_STORE_SCHEMA);
  }

  @Test
  void testRdfApiRequest() {
    final String urlPrefix = "http://localhost:" + PORT;

    final String defaultContentType = Constants.ACCEPT_TTL;
    final String jsonldContentType = Constants.ACCEPT_JSONLD;
    final String ttlContentType = Constants.ACCEPT_TTL;
    final String n3ContentType = Constants.ACCEPT_N3;
    final String defaultContentTypeWithCharset = Constants.ACCEPT_TTL + "; charset=utf-8";
    final String defaultContentTypeWithInvalidCharset = Constants.ACCEPT_TTL + "; charset=utf-16";

    rdfApiRequest(200, defaultContentType).get(urlPrefix + "/pet store/api/rdf-legacy");
    rdfApiRequest(200, defaultContentType).get(urlPrefix + "/pet store/api/rdf-legacy/Category");
    rdfApiRequest(200, defaultContentType)
        .get(urlPrefix + "/pet store/api/rdf-legacy/Category/column/name");
    rdfApiRequest(200, defaultContentType)
        .get(urlPrefix + "/pet store/api/rdf-legacy/Category/name=cat");
    rdfApiRequestMinimalExpect(400).get(urlPrefix + "/pet store/api/rdf-legacy/doesnotexist");
    rdfApiRequest(200, defaultContentType).get(urlPrefix + "/api/rdf-legacy?schemas=pet store");

    rdfApiContentTypeRequest(200, defaultContentTypeWithCharset, defaultContentType)
        .get(urlPrefix + "/pet store/api/rdf-legacy");
    rdfApiContentTypeRequest(406, defaultContentTypeWithInvalidCharset, EXCEPTION_CONTENT_TYPE)
        .get(urlPrefix + "/pet store/api/rdf-legacy");

    rdfApiRequest(200, jsonldContentType).get(urlPrefix + "/pet store/api/jsonld-legacy");
    rdfApiRequest(200, ttlContentType).get(urlPrefix + "/pet store/api/ttl-legacy");

    rdfApiContentTypeRequest(200, jsonldContentType).get(urlPrefix + "/pet store/api/rdf-legacy");

    rdfApiContentTypeRequest(200, ttlContentType, jsonldContentType)
        .get(urlPrefix + "/pet store/api/jsonld-legacy");
    rdfApiContentTypeRequest(200, jsonldContentType, ttlContentType)
        .get(urlPrefix + "/pet store/api/ttl-legacy");

    rdfApiRequest(200, defaultContentType).head(urlPrefix + "/pet store/api/rdf-legacy");
    rdfApiContentTypeRequest(200, jsonldContentType).head(urlPrefix + "/pet store/api/rdf-legacy");
    rdfApiRequest(200, jsonldContentType).head(urlPrefix + "/pet store/api/jsonld-legacy");
    rdfApiRequest(200, ttlContentType).head(urlPrefix + "/pet store/api/ttl-legacy");

    rdfApiContentTypeRequest(200, ttlContentType, jsonldContentType)
        .head(urlPrefix + "/pet store/api/jsonld-legacy");
    rdfApiContentTypeRequest(200, jsonldContentType, ttlContentType)
        .head(urlPrefix + "/pet store/api/ttl-legacy");

    rdfApiRequest(200, defaultContentType)
        .get(urlPrefix + "/pet store/api/rdf-legacy?validate=fdp-v1.2");
    rdfApiRequest(400, EXCEPTION_CONTENT_TYPE)
        .get(urlPrefix + "/pet store/api/rdf-legacy?validate=nonExisting");

    rdfApiRequest(200, ACCEPT_YAML).get(urlPrefix + "/api/rdf-legacy?shacls");
    rdfApiContentTypeRequest(200, defaultContentType, ACCEPT_YAML)
        .get(urlPrefix + "/api/rdf-legacy?shacls");

    rdfApiRequest(200, ACCEPT_YAML).head(urlPrefix + "/api/rdf-legacy?shacls");
    rdfApiContentTypeRequest(200, defaultContentType, ACCEPT_YAML)
        .head(urlPrefix + "/api/rdf-legacy?shacls");

    rdfApiContentTypeRequest(
        200, Constants.ACCEPT_TTL + "; q=0.5, " + Constants.ACCEPT_JSONLD, jsonldContentType);
    rdfApiContentTypeRequest(200, Constants.ACCEPT_TTL + "; q=0.5, text/*", n3ContentType)
        .head(urlPrefix + "/pet store/api/rdf-legacy");
    rdfApiContentTypeRequest(406, "image/jpeg", EXCEPTION_CONTENT_TYPE)
        .head(urlPrefix + "/pet store/api/rdf-legacy");
  }

  @Test
  void testRdfApiContent() {
    String resultBase =
        given()
            .sessionId(sessionId)
            .when()
            .get("http://localhost:" + PORT + "/api/rdf-legacy?schemas=pet store")
            .getBody()
            .asString();

    String resultBaseNonExisting =
        given()
            .sessionId(sessionId)
            .when()
            .get(
                "http://localhost:"
                    + PORT
                    + "/api/rdf-legacy?schemas=thisSchemaTotallyDoesNotExist")
            .getBody()
            .asString();

    String resultShaclSetsYaml =
        given()
            .sessionId(sessionId)
            .when()
            .get("http://localhost:" + PORT + "/api/rdf-legacy?shacls")
            .getBody()
            .asString();

    String resultSchema =
        given()
            .sessionId(sessionId)
            .when()
            .get("http://localhost:" + PORT + "/pet store/api/rdf-legacy")
            .getBody()
            .asString();

    assertAll(
        () -> assertFalse(resultBase.contains("CatalogueOntologies")),
        () ->
            assertTrue(
                resultBaseNonExisting.contains(
                    "Schema 'thisSchemaTotallyDoesNotExist' unknown or permission denied")),
        () ->
            assertTrue(
                resultBase.contains(
                    "http://localhost:" + PORT + "/pet%20store/api/rdf/Category/column/name")),
        () ->
            assertTrue(
                resultSchema.contains(
                    "http://localhost:" + PORT + "/pet%20store/api/rdf/Category/column/name")),
        () ->
            assertTrue(
                resultShaclSetsYaml.contains(
                    """
                    - id: dcat-ap-v3
                      name: DCAT-AP
                      version: 3.0.0
                      sources:
                      - https://semiceu.github.io/DCAT-AP/releases/3.0.0/#validation-of-dcat-ap
                    - id: hri-v2.0.2""")));
  }

  private RequestSender rdfApiRequest(int expectStatusCode, String expectContentType) {
    return given()
        .sessionId(sessionId)
        .expect()
        .statusCode(expectStatusCode)
        .header("Content-Type", expectContentType)
        .when();
  }

  private RequestSender rdfApiContentTypeRequest(int expectStatusCode, String contentType) {
    return rdfApiContentTypeRequest(expectStatusCode, contentType, contentType);
  }

  private RequestSender rdfApiContentTypeRequest(
      int expectStatusCode, String givenContentType, String expectedContentType) {
    return given()
        .sessionId(sessionId)
        .header("Accept", givenContentType)
        .expect()
        .statusCode(expectStatusCode)
        .header("Content-Type", expectedContentType)
        .when();
  }

  private RequestSender rdfApiRequestMinimalExpect(int expectStatusCode) {
    return given().sessionId(sessionId).expect().statusCode(expectStatusCode).when();
  }
}
